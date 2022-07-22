/* Copyright 2017-2022 ASRR B.V. */
package nl.asrr.common.generics.model

import java.time.ZonedDateTime

interface ICrudEntity {
    val id: String
    val created: ZonedDateTime
    val createdBy: String
    val updated: ZonedDateTime
    val updatedBy: String
}
