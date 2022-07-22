/* Copyright 2017-2022 ASRR B.V. */
package nl.asrr.cosmos.generics.service

import nl.asrr.cosmos.generics.model.ITenantCrudEntity
import nl.asrr.cosmos.generics.repository.ITenantCrudRepository

abstract class ITenantCrudService<T : ITenantCrudEntity>(
    override val repository: ITenantCrudRepository<T>
) : ICrudService<T>(repository) {

    fun findAllByTenantId(tenantId: String): List<T> {
        return repository.findAllByTenantId(tenantId)
    }
}
