package nl.asrr.core.gaia.scheduler

import io.github.oshai.kotlinlogging.KotlinLogging
import nl.asrr.core.gaia.dto.CreateApplication
import nl.asrr.core.gaia.service.GaiaNodeService
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled

private val log = KotlinLogging.logger {}

open class GaiaScheduler(
    properties: GaiaSchedulerProperties,
    @Value("\${spring.application.name:unknown}") applicationName: String,
    @Value("\${spring.application.company:unknown}") company: String,
    @Value("\${spring.profiles.active:default}") profile: String,
) {
    private val service = GaiaNodeService(
        CreateApplication(
            name = requireNotNull(properties.name) {
                "gaia.name must be set when gaia.enabled=true"
            },
            project = applicationName,
            company = company,
            profile = profile,
        ),
        version = properties.version,
    )

    @Scheduled(fixedRateString = "\${gaia.interval-ms:10000}")
    fun update() {
        try {
            service.updateNode()
        } catch (e: Exception) {
            log.debug(e) { "GAIA node update failed" }
        }
    }
}
