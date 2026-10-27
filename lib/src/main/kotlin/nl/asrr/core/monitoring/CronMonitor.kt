/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.monitoring

import io.sentry.MonitorConfig
import io.sentry.MonitorSchedule
import io.sentry.MonitorScheduleUnit
import io.sentry.Sentry
import io.sentry.util.CheckInUtils
import java.time.ZoneId
import java.util.concurrent.Callable

/**
 * A scheduled job whose silence a customer would notice: mail that is not fetched or sent, invoices
 * that do not reach the bookkeeping. Each reports a check-in to a Sentry cron monitor, which raises
 * an issue when a run is missed or overruns. Errors inside a run are already reported through the
 * log; this covers the job that stops running at all, which logs nothing.
 *
 * Implement it on an enum of the app's monitored jobs:
 *
 * ```
 * enum class MonitoredJob(override val slug: String, override val schedule: CronMonitor.Schedule,
 *     override val marginMinutes: Long, override val maxRuntimeMinutes: Long) : CronMonitor {
 *     INBOX_POLLING("inbox-polling", CronMonitor.Schedule.Every(5), 5, 30),
 * }
 * ```
 *
 * Not every `@Scheduled` method belongs there: Sentry bills per monitor. Add a job when its absence
 * would go unnoticed for longer than it should. Call [run] inside the `@SchedulerLock` method, so
 * only the pod that holds the lock checks in. The monitor is created from [monitorConfig] on the
 * first check-in; nothing is set up in Sentry by hand.
 *
 * A fixed delay counts from the end of the previous run and each pod keeps its own timer, so the
 * gap between two runs is the delay plus the run itself plus the phase between pods: give a
 * fixed-delay job a margin of one whole interval.
 *
 * Needs `io.sentry:sentry` on the classpath; core only compiles against it.
 */
interface CronMonitor {
    val slug: String
    val schedule: Schedule

    /** Minutes a run may be late before it counts as missed. */
    val marginMinutes: Long

    /** Minutes a run may take before it counts as hung; follow the job's `lockAtMostFor`. */
    val maxRuntimeMinutes: Long

    sealed interface Schedule {
        data class Every(val minutes: Int) : Schedule

        /** Five-field crontab, read in the JVM's zone like the `@Scheduled(cron = …)` it mirrors. */
        data class Cron(val expression: String) : Schedule
    }

    fun <T> run(block: () -> T): T = if (Sentry.isEnabled()) CheckInUtils.withCheckIn(slug, monitorConfig(), Callable(block)) else block()

    fun monitorConfig(): MonitorConfig = MonitorConfig(
        when (val schedule = schedule) {
            is Schedule.Every -> MonitorSchedule.interval(schedule.minutes, MonitorScheduleUnit.MINUTE)
            is Schedule.Cron -> MonitorSchedule.crontab(schedule.expression)
        },
    ).apply {
        checkinMargin = marginMinutes
        maxRuntime = maxRuntimeMinutes
        timezone = ZoneId.systemDefault().id
        // One late or failed run is a pod restart or a deploy; two in a row is a job that stopped.
        failureIssueThreshold = 2
        recoveryThreshold = 1
    }
}
