/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.monitoring

import io.sentry.EventProcessor
import io.sentry.Sentry
import io.sentry.SentryOptions

/**
 * Turns Sentry on for a deployment and leaves it off everywhere else.
 *
 * The switch is `SENTRY_DSN` in the environment, which only the deployment manifests should set.
 * Do not put a DSN in a bundled `sentry.properties`: Alpha's did, so every laptop, CI runner and
 * e2e container reported into the project while the image blanked it with `ENV SENTRY_DSN=""` and
 * prod and dev reported nothing at all.
 *
 * Call it from `main` before Spring starts, so a context that fails to come up is reported too. The
 * logback `SentryAppender` also initialises the SDK when it starts, at the lowest priority, so it
 * never replaces what is configured here however often Spring Boot reconfigures logging.
 *
 * Needs `io.sentry:sentry` on the classpath; core only compiles against it.
 */
object SentryBootstrap {

    /**
     * @param application the release name prefix, e.g. `alpha-api` gives `alpha-api@4.270.0`.
     * @param inAppPackage the package whose frames Sentry marks as the app's own.
     * @param eventProcessors added to every event, typically a [RequestContextEventProcessor].
     */
    fun init(
        application: String,
        version: String,
        inAppPackage: String = "nl.asrr",
        eventProcessors: List<EventProcessor> = listOf(RequestContextEventProcessor()),
        env: (String) -> String? = System::getenv,
    ) {
        val options = options(application, version, inAppPackage, eventProcessors, env) ?: return
        Sentry.init(options)
    }

    fun options(
        application: String,
        version: String,
        inAppPackage: String = "nl.asrr",
        eventProcessors: List<EventProcessor> = emptyList(),
        env: (String) -> String? = System::getenv,
    ): SentryOptions? {
        val dsn = env("SENTRY_DSN")?.trim().orEmpty()
        if (dsn.isEmpty()) return null

        return SentryOptions().apply {
            // Environment and the like still come from SENTRY_* variables and sentry.properties.
            isEnableExternalConfiguration = true
            this.dsn = dsn
            // An image built without a tag (a local `docker build`, the e2e stack) has no version;
            // no release is better than every such build sharing one.
            if (version.isNotBlank() && version != "unknown") release = "$application@$version"
            isSendDefaultPii = false
            addInAppInclude(inAppPackage)
            eventProcessors.forEach(::addEventProcessor)
            env("SENTRY_TRACES_SAMPLE_RATE")?.trim()?.toDoubleOrNull()?.takeIf { it > 0.0 }?.let { rate ->
                setTracesSampler { context ->
                    // A trace the browser started is followed through, so a slow page shows its requests.
                    context.transactionContext.parentSampled?.let { if (it) 1.0 else 0.0 }
                        ?: sampleRate(context.transactionContext.name, rate)
                }
            }
        }
    }

    /** Kubelet probes and Prometheus scrapes outnumber real requests and say nothing. */
    fun sampleRate(transactionName: String, rate: Double): Double = if (transactionName.contains("/actuator")) 0.0 else rate
}
