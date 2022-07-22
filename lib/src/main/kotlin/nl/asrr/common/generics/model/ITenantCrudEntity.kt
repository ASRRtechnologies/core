/* Copyright 2017-2022 ASRR B.V. */
package nl.asrr.common.generics.model

import nl.asrr.common.generics.model.ICrudEntity

interface ITenantCrudEntity : ICrudEntity {
    val tenantId: String
}
