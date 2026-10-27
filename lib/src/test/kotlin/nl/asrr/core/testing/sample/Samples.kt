/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.testing.sample

import com.fasterxml.jackson.annotation.JsonProperty
import nl.asrr.core.ops.scheduling.RunsOnEveryInstance
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock

// Fixtures for the architecture contracts: `good` must pass every contract, `bad` must fail each.

@Document("good_order")
data class GoodOrder(val id: String, val tenantId: String, @field:JsonProperty("isPaid") val isPaid: Boolean = false)

interface GoodOrderRepository : MongoRepository<GoodOrder, String> {
    fun findOneByIdAndTenantId(id: String, tenantId: String): GoodOrder?
}

class GoodOrderService(private val repository: GoodOrderRepository) {
    fun find(id: String, tenantId: String) = repository.findOneByIdAndTenantId(id, tenantId)
}

@Component
class GoodJobs {
    @Scheduled(fixedDelay = 60_000)
    @SchedulerLock(name = "good-sweep", lockAtMostFor = "PT5M", lockAtLeastFor = "PT30S")
    fun sweep() = Unit

    @Scheduled(fixedDelay = 1_000)
    @RunsOnEveryInstance("clears this pod's own cache")
    fun clearCache() = Unit
}
