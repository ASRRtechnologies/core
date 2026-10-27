/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.ops.scheduling

/**
 * Marks a `@Scheduled` method that deliberately runs on every replica instead of taking a ShedLock
 * `@SchedulerLock`: it touches only this pod's memory, or it is a worker loop that claims its own
 * work atomically. Every replica runs every `@Scheduled` job, so without a lock a notification job
 * on three pods notifies three times; `ScheduledJobLockContract` in the test fixtures holds each
 * scheduled method to one of the two.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RunsOnEveryInstance(val reason: String)
