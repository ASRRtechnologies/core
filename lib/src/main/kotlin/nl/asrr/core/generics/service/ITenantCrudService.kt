/* Copyright 2017-2022 ASRR B.V. */
package nl.asrr.core.generics.service

import nl.asrr.core.generics.model.ITenantCrudEntity
import nl.asrr.core.generics.repository.ITenantCrudRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

abstract class ITenantCrudService<T : ITenantCrudEntity>(
    override val repository: ITenantCrudRepository<T>
) : ICrudService<T>(repository) {

    fun findAllByTenantId(tenantId: String): List<T> {
        return repository.findAllByTenantId(tenantId)
    }

    fun findAllByIdAndTenantId(ids: List<String>, tenantId: String): List<T> {
        return repository.findAllByIdAndTenantId(ids, tenantId)
    }

    fun findOneByIdAndTenantId(id: String, tenantId: String): T {
        return repository.findOneByIdAndTenantId(id, tenantId) ?: throw IllegalArgumentException("No entity found with id $id and tenantId $tenantId")
    }

    fun find(tenantId: String, pageable: Pageable): Page<T> {
        return repository.findAllByTenantId(tenantId, pageable)
    }

    fun delete(tenantId: String, id: String, ) {
        repository.delete(findOneByIdAndTenantId(id, tenantId))
    }

    fun deleteList(ids: List<String>, tenantId: String) {
        repository.deleteAllByIdAndTenantId(ids, tenantId)
    }
}
