/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.testing.architecture

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import nl.asrr.core.ops.scheduling.RunsOnEveryInstance
import nl.asrr.core.testing.architecture.ArchitectureScan.require
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.lang.reflect.Method
import java.time.Duration

/**
 * Every replica runs every `@Scheduled` job: without a lock, Alpha's overdue-lead job sent each
 * notification three times on a three-pod prod. So each `@Scheduled` method either takes a ShedLock
 * `@SchedulerLock` with explicit, ISO-8601 durations and a unique name, or states with
 * [everyInstanceAnnotation] (default [RunsOnEveryInstance]) why it must run on each pod.
 *
 * Needs `net.javacrumbs.shedlock:shedlock-spring` on the test classpath.
 */
abstract class ScheduledJobLockContract {

    abstract val basePackage: String

    /** Lower bound that proves the scan found the jobs. */
    open val minScheduledMethods: Int = 1

    /** The opt-out annotation; it must have a `reason(): String`. Override when the app has its own. */
    open val everyInstanceAnnotation: Class<out Annotation> = RunsOnEveryInstance::class.java

    private val scheduledMethods: List<Method> by lazy {
        val scanner = ClassPathScanningCandidateComponentProvider(false)
        scanner.addIncludeFilter(AnnotationTypeFilter(Component::class.java))
        scanner.findCandidateComponents(basePackage)
            .mapNotNull { it.beanClassName }
            .flatMap { className -> Class.forName(className).declaredMethods.toList() }
            .filter { AnnotatedElementUtils.hasAnnotation(it, Scheduled::class.java) }
            .sortedBy { it.label() }
    }

    private fun Method.label() = "${declaringClass.simpleName}.$name"

    private fun Method.everyInstanceReason(): String? = getAnnotation(everyInstanceAnnotation)?.let { annotation ->
        runCatching { annotation.annotationClass.java.getMethod("reason").invoke(annotation) as String }.getOrDefault("")
    }

    @Test
    fun `the scan actually finds the scheduled jobs`() {
        require(scheduledMethods.size >= minScheduledMethods) { "Expected $minScheduledMethods+ @Scheduled methods, found ${scheduledMethods.size}" }
    }

    @Test
    fun `every scheduled method is locked or explicitly runs on every instance`() {
        val offenders = scheduledMethods.mapNotNull { method ->
            val locked = method.isAnnotationPresent(SchedulerLock::class.java)
            val reason = method.everyInstanceReason()
            when {
                locked && reason != null -> "${method.label()}: has both @SchedulerLock and @${everyInstanceAnnotation.simpleName}"
                !locked && reason == null -> "${method.label()}: add @SchedulerLock (or @${everyInstanceAnnotation.simpleName} with a reason)"
                reason != null && reason.isBlank() -> "${method.label()}: @${everyInstanceAnnotation.simpleName} needs a reason"
                else -> null
            }
        }
        require(offenders.isEmpty()) { "Each @Scheduled job runs once per replica unless it is locked:\n" + offenders.joinToString("\n") { "  $it" } }
    }

    @Test
    fun `lock names are unique and both durations are explicit and consistent`() {
        val locks = scheduledMethods.mapNotNull { m -> m.getAnnotation(SchedulerLock::class.java)?.let { m to it } }

        val problems = mutableListOf<String>()
        locks.groupBy { (_, lock) -> lock.name }
            .filter { (_, users) -> users.size > 1 }
            .forEach { (name, users) -> problems += "lock name '$name' is shared by ${users.joinToString { it.first.label() }}" }

        for ((method, lock) in locks) {
            if (lock.name.isBlank()) problems += "${method.label()}: blank lock name"
            if (lock.lockAtMostFor.isBlank() || lock.lockAtLeastFor.isBlank()) {
                problems += "${method.label()}: set lockAtMostFor and lockAtLeastFor explicitly"
                continue
            }
            val atMost = runCatching { Duration.parse(lock.lockAtMostFor) }.getOrNull()
            val atLeast = runCatching { Duration.parse(lock.lockAtLeastFor) }.getOrNull()
            if (atMost == null || atLeast == null) {
                problems += "${method.label()}: durations must be ISO-8601, e.g. PT30S"
            } else if (atLeast > atMost) {
                problems += "${method.label()}: lockAtLeastFor ($atLeast) exceeds lockAtMostFor ($atMost)"
            }
        }
        require(problems.isEmpty()) { problems.joinToString("\n") }
    }
}
