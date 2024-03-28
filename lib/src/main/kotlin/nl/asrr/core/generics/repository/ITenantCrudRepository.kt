/* Copyright 2017-2022 ASRR B.V. */
package nl.asrr.core.generics.repository

import nl.asrr.core.generics.model.ITenantCrudEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.query.TextCriteria

interface ITenantCrudRepository<T : ITenantCrudEntity> : ICrudRepository<T> {
    fun existsByTenantIdAndId(tenantId: String, id: String): Boolean
    fun findAllByTenantId(tenantId: String): List<T>
    fun findAllByTenantId(tenantId: String, pageable: Pageable): Page<T>
    fun findAllByTenantId(tenantId: String, criteria: TextCriteria, pageable: Pageable): Page<T>
    fun findOneByIdAndTenantId(id: String, tenantId: String): T?
    fun findAllByIdAndTenantId(ids: List<String>, tenantId: String): List<T>
    fun deleteAllByIdAndTenantId(ids: List<String>, tenantId: String)
    fun deleteByIdAndTenantId(id: String, tenantId: String)
}
