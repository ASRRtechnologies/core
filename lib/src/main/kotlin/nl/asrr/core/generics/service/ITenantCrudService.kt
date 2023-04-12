/* Copyright 2017-2022 ASRR B.V. */
package nl.asrr.core.generics.service

import nl.asrr.core.generics.model.ITenantCrudEntity
import nl.asrr.core.generics.repository.ITenantCrudRepository
import org.springframework.data.domain.Page

abstract class ITenantCrudService<T : ITenantCrudEntity>(
    override val repository: ITenantCrudRepository<T>
) : ICrudService<T>(repository) {

    fun findAllByTenantId(tenantId: String): List<T> {
        return repository.findAllByTenantId(tenantId)
    }

    fun findOneByIdAndTenantId(id: String, tenantId: String): T {
        return repository.findOneByIdAndTenantId(id, tenantId) ?: throw IllegalArgumentException("No entity found with id $id and tenantId $tenantId")
    }

    fun find(tenantId: String, pageNumber: Int, pageSize: Int?): Page<T> {
        return repository.findAllByTenantId(tenantId, pageNumber, pageSize)
    }
}
