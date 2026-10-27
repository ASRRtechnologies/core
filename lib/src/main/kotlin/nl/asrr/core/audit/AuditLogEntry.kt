/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.audit

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

/**
 * One line of the central audit log: who did what, when, to which record.
 *
 * Two kinds of line share the collection, linked by [requestId]:
 * - [AuditAction.REQUEST]: a change request as the user made it (method, route, outcome, IP), plus
 *   the explicit sign-in events ([AuditAction.LOGIN] and friends);
 * - record lines ([AuditAction.CREATE], [AuditAction.UPDATE], …): what that request did to the data,
 *   built with [AuditChanges] from the documents themselves, with the changed fields.
 *
 * Append-only: nothing in the application updates or deletes a line; [expireAt] carries the
 * retention and a TTL index removes the line when it passes.
 */
@Document(collection = AuditLogEntry.COLLECTION)
@CompoundIndexes(
    CompoundIndex(name = "tenant_at", def = "{'tenantId': 1, 'at': -1}"),
    CompoundIndex(name = "tenant_record_at", def = "{'tenantId': 1, 'collection': 1, 'entityId': 1, 'at': -1}"),
    CompoundIndex(name = "tenant_actor_at", def = "{'tenantId': 1, 'actorId': 1, 'at': -1}"),
    CompoundIndex(name = "tenant_request", def = "{'tenantId': 1, 'requestId': 1}", sparse = true),
)
data class AuditLogEntry(
    @Id
    val id: String? = null,
    /** Null only for events that happen before anyone is known, such as a failed sign-in to no account. */
    val tenantId: String?,
    val at: Instant,
    val action: AuditAction,
    /** Username of whoever acted; for an API key its creator, for an assistant or worker its name. */
    val actorId: String?,
    /** The super admin behind an impersonation ("log in as") session. */
    val impersonatedBy: String? = null,
    /** How the change came in; null on lines written before it was recorded. */
    val channel: AuditChannel? = null,
    /** The API key (or agent sign-in) the change came through, so one key's work can be found back. */
    val apiKeyId: String? = null,
    val apiKeyName: String? = null,
    /** The MCP tool that made the change, e.g. `create_ticket`. */
    val mcpTool: String? = null,
    val collection: String? = null,
    val entityId: String? = null,
    /** A human handle for the record (name, title, number) so the log reads without lookups. */
    val label: String? = null,
    val changes: List<AuditFieldChange> = emptyList(),
    /** How many further changes were left out to keep the line small. */
    val truncatedChanges: Int = 0,
    /** Records a multi-document write touched. */
    val matched: Long? = null,
    /** The filter of a multi-document write, rendered and cut short. */
    val filter: String? = null,
    val method: String? = null,
    val path: String? = null,
    val status: Int? = null,
    val ip: String? = null,
    val requestId: String? = null,
    /** Free text for auth events, e.g. why a sign-in failed. */
    val detail: String? = null,
    @Indexed(name = "expire_at_ttl", expireAfter = "0s")
    val expireAt: Instant? = null,
) {
    companion object {
        const val COLLECTION = "audit_log"
    }
}

/**
 * How a change reached the application. An API key and an AI agent act with the rights of the
 * person who made them, so without this the log could not tell "Ina changed it" from "Ina's
 * Claude changed it".
 */
enum class AuditChannel {
    /** A signed-in person in the app. */
    UI,

    /** A request with an API key: Zapier, a script, curl. */
    API_KEY,

    /** A tool call on an MCP endpoint, with an API key or an OAuth sign-in. */
    MCP,
}

data class AuditFieldChange(
    val path: String,
    val from: String?,
    val to: String?,
    /** The update operator (`$set`, `$unset`, `$push`, …) for an atomic update; null for a full save. */
    val op: String? = null,
)

enum class AuditAction {
    REQUEST,
    CREATE,
    UPDATE,
    BULK_UPDATE,
    SOFT_DELETE,
    RESTORE,
    DELETE,
    LOGIN,
    LOGIN_FAILED,
    LOGOUT,
    IMPERSONATION_START,

    /** Someone's access to a tenant was withdrawn at once: SCIM deactivation or deletion, member removal, session policy. */
    ACCESS_REVOKED,
}
