/* Copyright 2017-2022 ASRR B.V. */
package nl.asrr.core.generics.repository

import nl.asrr.core.generics.model.ITenantCrudEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ITenantCrudRepository<T : ITenantCrudEntity> : ICrudRepository<T> {
    fun findAllByTenantId(tenantId: String): List<T>
    fun findAllByTenantId(tenantId: String, pageable: Pageable): Page<T>
    fun findOneByIdAndTenantId(id: String, tenantId: String): T?
}
