/* Copyright 2017-2022 ASRR B.V. */
package nl.asrr.common.generics.service

import nl.asrr.common.generics.model.ITenantCrudEntity
import nl.asrr.common.generics.repository.ITenantCrudRepository

abstract class ITenantCrudService<T : ITenantCrudEntity>(
    override val repository: ITenantCrudRepository<T>
) : ICrudService<T>(repository) {

    fun findAllByTenantId(tenantId: String): List<T> {
        return repository.findAllByTenantId(tenantId)
    }
}
