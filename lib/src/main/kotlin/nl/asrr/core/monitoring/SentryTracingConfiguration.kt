/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.monitoring

import io.sentry.ScopesAdapter
import io.sentry.Sentry
import io.sentry.protocol.TransactionNameSource
import io.sentry.spring7.SentrySpringFilter
import io.sentry.spring7.tracing.SentryTracingFilter
import io.sentry.spring7.tracing.SpringMvcTransactionNameProvider
import io.sentry.spring7.tracing.TransactionNameProvider
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Request tracing without the Sentry Boot starter. Opt in with `@Import(SentryTracingConfiguration::class)`;
 * it is only active on a deployment (`SENTRY_DSN` set), and how much is traced is
 * `SENTRY_TRACES_SAMPLE_RATE`. Declare a [ReportingContextProvider] bean to tag transactions
 * with the tenant.
 *
 * Order matters. [SentrySpringFilter] gives each request its own Sentry scope, so nothing a request
 * puts on it survives onto the next request served by the same pooled thread, and
 * [SentryTracingFilter] opens the transaction inside it, continuing the trace the browser started
 * when the request carries one. Both sit ahead of Spring Security so a rejected request is traced
 * as well.
 *
 * Needs `io.sentry:sentry-spring-7` on the classpath; core only compiles against it.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = ["SENTRY_DSN"])
class SentryTracingConfiguration {

    @Bean
    fun sentryRequestScopeFilter(): FilterRegistrationBean<SentrySpringFilter> = FilterRegistrationBean(SentrySpringFilter()).apply { order = Ordered.HIGHEST_PRECEDENCE }

    @Bean
    fun sentryTracingFilter(): FilterRegistrationBean<SentryTracingFilter> = FilterRegistrationBean(SentryTracingFilter(ScopesAdapter.getInstance(), RouteTransactionNames())).apply { order = Ordered.HIGHEST_PRECEDENCE + 1 }

    @Bean
    fun sentryTraceContextFilter(reportingContext: ObjectProvider<ReportingContextProvider>): FilterRegistrationBean<TraceContextFilter> =
        FilterRegistrationBean(TraceContextFilter(reportingContext.ifAvailable ?: ReportingContextProvider { null })).apply { order = Ordered.LOWEST_PRECEDENCE }

    /**
     * Names a transaction by its route (`GET /api/v1/projects/{id}`). The filter starts every
     * transaction under the raw path and only renames it when this returns something, so a request
     * no controller matched would keep the address it was sent to, and with it whatever token a
     * scanner or a stale link put in the path.
     */
    class RouteTransactionNames : TransactionNameProvider {
        private val routes = SpringMvcTransactionNameProvider()

        override fun provideTransactionName(request: HttpServletRequest): String = routes.provideTransactionName(request) ?: "${request.method} [unmatched]"

        override fun provideTransactionSource(): TransactionNameSource = TransactionNameSource.ROUTE
    }

    /**
     * The transaction is finished by the filter above, after Spring Security has cleared its
     * context, so [RequestContextEventProcessor] would find nobody. This runs inside the security
     * chain, once authentication is known, and puts the tenant on the transaction while it is open.
     */
    class TraceContextFilter(private val context: ReportingContextProvider) : OncePerRequestFilter() {
        override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
            runCatching {
                val context = context.current() ?: return@runCatching
                Sentry.getSpan()?.let { span ->
                    context.tenantId?.let { span.setTag(RequestContextEventProcessor.TENANT_TAG, it) }
                    context.auth?.let { span.setTag(RequestContextEventProcessor.AUTH_TAG, it) }
                }
            }
            filterChain.doFilter(request, response)
        }
    }
}
