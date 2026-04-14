package nl.asrr.core.gaia.scheduler

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling

@AutoConfiguration
@EnableScheduling
@EnableConfigurationProperties(GaiaSchedulerProperties::class)
@ConditionalOnProperty(prefix = "gaia", name = ["enabled"], havingValue = "true")
open class GaiaSchedulerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    open fun gaiaScheduler(
        properties: GaiaSchedulerProperties,
        @org.springframework.beans.factory.annotation.Value("\${spring.application.name:unknown}") applicationName: String,
        @org.springframework.beans.factory.annotation.Value("\${spring.application.company:unknown}") company: String,
        @org.springframework.beans.factory.annotation.Value("\${spring.profiles.active:default}") profile: String,
    ): GaiaScheduler = GaiaScheduler(properties, applicationName, company, profile)
}
