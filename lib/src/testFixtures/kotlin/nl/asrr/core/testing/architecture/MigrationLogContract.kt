/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.testing.architecture

import nl.asrr.core.testing.architecture.ArchitectureScan.require
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Every startup migration says on every boot whether it still had work, in the one format
 * [nl.asrr.core.ops.migration.MigrationLog] writes. A migration that is silent when idle can never
 * be proposed for deletion, and one that logs its own wording cannot be told apart once Logback has
 * cut its logger name to 35 characters.
 *
 * Reads the sources: a migration is a `*Migration.kt` in a directory named `migration`.
 */
abstract class MigrationLogContract {

    /** Source root, relative to the module directory Gradle runs tests in. */
    open val sourceRoot: String = "src/main/kotlin"

    /** Lower bound that proves the scan found the migrations; 0 for an app that has none yet. */
    open val minMigrations: Int = 0

    private val migrations: List<File> by lazy {
        File(sourceRoot).walkTopDown()
            .filter { it.isFile && it.name.endsWith("Migration.kt") && it.parentFile.name == "migration" }
            .sortedBy { it.name }
            .toList()
    }

    @Test
    fun `the scan actually finds the migrations`() {
        require(migrations.size >= minMigrations) { "Expected $minMigrations+ startup migrations under $sourceRoot, found ${migrations.size}" }
    }

    @Test
    fun `every startup migration reports its idle state through MigrationLog`() {
        val offenders = migrations.filterNot { "MigrationLog.noop(" in it.readText() }.map { it.name }
        require(offenders.isEmpty()) {
            "Log the idle path with MigrationLog.noop(...) (pass keep = \"<why>\" if it must stay forever):\n" +
                offenders.joinToString("\n") { "  $it" }
        }
    }
}
