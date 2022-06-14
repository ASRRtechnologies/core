package nl.asrr.common.models.entity

import java.time.ZonedDateTime

/**
 * Base modifier entity extension for our entities. Stores modification and creation data.
 * @author Amar Ramdas
 */
abstract class BaseModifierEntity(
    var modifiedBy: String,
    var modifiedAt: ZonedDateTime,
    var createdBy: String,
    val createdAt: ZonedDateTime = ZonedDateTime.now()
)
