package nl.asrr.core.gaia.scheduler

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "gaia")
data class GaiaSchedulerProperties(
    val enabled: Boolean = false,
    val name: String? = null,
    val version: String? = null,
    val intervalMs: Long = 10_000,
)
