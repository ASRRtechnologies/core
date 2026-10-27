/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.ops.migration

import io.github.oshai.kotlinlogging.KLogger

/**
 * The one line format for what a startup migration did on this boot:
 * `migration=<Class> status=noop|applied|disabled|failed [changed=N] [keep="<why>"] - <detail>`.
 *
 * AURA reads these from the prod logs in Loki (`prodreview`) and counts, per migration, how many
 * releases logged `status=noop`. One that has been idle across releases and is older than two weeks
 * is proposed for deletion. Pass `keep` for a migration that must stay although it is idle (the
 * only place an index is created where `auto-index-creation` is off, versioned defaults): those are
 * never proposed. A migration that is silent when idle can never be proposed at all, which is what
 * `MigrationLogContract` in the test fixtures guards.
 *
 * The name goes in the message rather than relying on the logger, because Logback cuts the logger
 * column to 35 characters and `DashboardPermissionsBackfillMigration` turns into `ashboardPerm…`.
 */
object MigrationLog {

    fun noop(logger: KLogger, migration: Any, detail: String = "nothing to do", keep: String? = null) = logger.info { line(migration, "noop", detail, keep = keep) }

    fun applied(logger: KLogger, migration: Any, changed: Number, detail: String) = logger.warn { line(migration, "applied", detail, changed = changed) }

    fun disabled(logger: KLogger, migration: Any) = logger.info { line(migration, "disabled", "switched off by its startup flag") }

    fun failed(logger: KLogger, migration: Any, e: Throwable, detail: String = "failed") = logger.error(e) { line(migration, "failed", detail) }

    fun line(migration: Any, status: String, detail: String, keep: String? = null, changed: Number? = null): String = buildString {
        append("migration=").append(nameOf(migration.javaClass.simpleName)).append(" status=").append(status)
        if (changed != null) append(" changed=").append(changed)
        if (keep != null) append(" keep=\"").append(keep).append('"')
        append(" - ").append(detail)
    }

    /** Spring's CGLIB proxies are named `Foo$$SpringCGLIB$$0`; the migration is `Foo`. */
    internal fun nameOf(simpleName: String): String = simpleName.substringBefore("$$")
}
