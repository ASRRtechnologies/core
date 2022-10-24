/* Copyright 2017-2022 ASRR B.V. */
package nl.asrr.common.generics.repository

import nl.asrr.common.generics.model.ITenantCrudEntity
import org.springframework.data.domain.Page

interface ITenantCrudRepository<T : ITenantCrudEntity> : ICrudRepository<T> {
    fun findAllByTenantId(tenantId: String): List<T>
    fun findAllByTenantId(tenantId: String, pageNumber: Int, pageSize: Int?): Page<T>
    fun findOneByIdAndTenantId(id: String, tenantId: String): T?
}
