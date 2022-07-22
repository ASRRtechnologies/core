/* Copyright 2017-2022 ASRR B.V. */
package nl.asrr.cosmos.generics.repository

import nl.asrr.cosmos.generics.model.ITenantCrudEntity

interface ITenantCrudRepository<T : ITenantCrudEntity> : ICrudRepository<T> {
    fun findAllByTenantId(tenantId: String): List<T>
}
