/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.monitoring

import io.sentry.Breadcrumb
import io.sentry.EventProcessor
import io.sentry.Hint
import io.sentry.SentryEvent
import io.sentry.protocol.Request
import io.sentry.protocol.SentryTransaction
import io.sentry.protocol.User

/**
 * Who a report is about, read off the reporting thread. [userRecordId] is the generated record id,
 * never the username, which is an e-mail address. [auth] names how the caller signed in (`jwt`,
 * `api-key`, …).
 */
data class ReportingContext(
    val tenantId: String? = null,
    val auth: String? = null,
    val userRecordId: String? = null,
)

/** Reads the [ReportingContext] of the current thread, typically off the Spring `SecurityContext`. */
fun interface ReportingContextProvider {
    fun current(): ReportingContext?
}

/**
 * Says which tenant and which user a report is about, so an issue can be read as "only one
 * customer" or "three customers" instead of a bare stack trace.
 *
 * [context] is read on the reporting thread at the moment of the error rather than kept on a Sentry
 * scope: the appender captures on the thread that logged, and a scope set per request would have to
 * be unwound on every exit path of a pooled thread. Typically it reads the Spring `SecurityContext`.
 *
 * It also takes off what the request filter attached beyond method and path: headers can carry an
 * API key, a query string a sign-in or unsubscribe token, and the refresh endpoint has the refresh
 * token in the path itself.
 */
class RequestContextEventProcessor(
    private val context: ReportingContextProvider = ReportingContextProvider { null },
) : EventProcessor {

    override fun process(event: SentryEvent, hint: Hint): SentryEvent {
        event.request?.let(::strip)
        event.breadcrumbs?.forEach(::strip)
        // A report that cannot be annotated is still a report.
        val context = runCatching { context.current() }.getOrNull() ?: return event
        context.tenantId?.let { event.setTag(TENANT_TAG, it) }
        context.auth?.let { event.setTag(AUTH_TAG, it) }
        context.userRecordId?.let { event.user = User().apply { id = it } }
        return event
    }

    override fun process(transaction: SentryTransaction, hint: Hint): SentryTransaction {
        transaction.request?.let(::strip)
        transaction.breadcrumbs?.forEach(::strip)
        return transaction
    }

    private fun strip(request: Request) {
        request.headers = null
        request.cookies = null
        request.queryString = null
        request.data = null
        request.envs = null
        request.url = request.url?.let(::safePath)
    }

    private fun strip(breadcrumb: Breadcrumb) {
        (breadcrumb.getData("url") as? String)?.let { breadcrumb.setData("url", safePath(it)) }
    }

    companion object {
        const val TENANT_TAG = "tenant"
        const val AUTH_TAG = "auth"

        private val CREDENTIAL_PATH = Regex("(/auth/(?:refresh|logout)/)[^/]+")
        private val UUID = Regex("^[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}$")

        // Record ids are 16 to 24 characters; a path segment this long is a token, whichever
        // endpoint it belongs to.
        private val TOKEN_SEGMENT = Regex("^[\\w\\-.~+=%]{32,}$")

        /** The path of a URL with anything that can be a credential taken out. */
        fun safePath(url: String): String = url
            .substringBefore('?')
            .substringBefore('#')
            .replace(CREDENTIAL_PATH, "$1[Filtered]")
            .split('/')
            .joinToString("/") { if (TOKEN_SEGMENT.matches(it) && !UUID.matches(it)) "[Filtered]" else it }
    }
}
