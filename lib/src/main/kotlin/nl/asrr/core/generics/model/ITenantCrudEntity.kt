/* Copyright 2017-2022 ASRR B.V. */
package nl.asrr.core.generics.model

interface ITenantCrudEntity : ICrudEntity {
    val tenantId: String
}
