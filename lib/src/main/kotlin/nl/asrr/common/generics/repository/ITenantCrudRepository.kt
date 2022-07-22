/* Copyright 2017-2022 ASRR B.V. */
package nl.asrr.common.generics.repository

import nl.asrr.common.generics.model.ITenantCrudEntity

interface ITenantCrudRepository<T : ITenantCrudEntity> : ICrudRepository<T> {
    fun findAllByTenantId(tenantId: String): List<T>
}
