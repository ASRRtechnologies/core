/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.audit

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.data.mongodb.core.MongoTemplate
import java.time.Duration
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

private val logger = KotlinLogging.logger {}

/**
 * Writes the audit log off the request thread.
 *
 * Callers [submit] a thunk that builds the line; a single background thread builds the lines (the
 * field diff happens there, not on the request) and inserts them in batches of [BATCH_SIZE]. A
 * request never waits on the audit log: when the queue is full the line is dropped and counted,
 * because blocking every write in the application on a slow audit insert is the worse failure.
 *
 * Each line gets an [AuditLogEntry.expireAt] of [retentionDays] after it happened; the TTL index on
 * the collection removes it then. Register it as a bean (Spring calls [start] and [stop]).
 */
open class AuditLogWriter(
    private val mongoTemplate: MongoTemplate,
    retentionDays: Long = 365,
    queueCapacity: Int = 10_000,
) {
    private val queue = ArrayBlockingQueue<() -> AuditLogEntry?>(queueCapacity)
    private val retention = Duration.ofDays(retentionDays)
    private val droppedCount = AtomicLong()
    private val failedCount = AtomicLong()

    @Volatile
    private var running = false
    private var thread: Thread? = null

    val dropped: Long get() = droppedCount.get()
    val failed: Long get() = failedCount.get()

    /** Queues a line; false when the queue is full and the line was dropped. */
    fun submit(build: () -> AuditLogEntry?): Boolean {
        if (queue.offer(build)) return true
        val n = droppedCount.incrementAndGet()
        // Once per thousand, not per line: a full queue means a burst, and so would the log.
        if (n % 1_000 == 1L) logger.error { "Audit log queue full ($n lines dropped so far)" }
        return false
    }

    @PostConstruct
    fun start() {
        running = true
        thread = Thread(::loop, "audit-log-writer").apply {
            isDaemon = true
            start()
        }
    }

    @PreDestroy
    fun stop() {
        running = false
        thread?.interrupt()
        thread?.join(5_000)
        drain()
    }

    /** Writes everything queued right now. The background thread's step, and the test hook. */
    fun drain() {
        while (true) {
            val first = queue.poll() ?: return
            val batch = ArrayList<() -> AuditLogEntry?>(BATCH_SIZE).apply { add(first) }
            queue.drainTo(batch, BATCH_SIZE - 1)
            write(batch)
        }
    }

    private fun loop() {
        while (running) {
            try {
                val first = queue.poll(POLL_MILLIS, TimeUnit.MILLISECONDS) ?: continue
                val batch = ArrayList<() -> AuditLogEntry?>(BATCH_SIZE).apply { add(first) }
                queue.drainTo(batch, BATCH_SIZE - 1)
                write(batch)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                return
            } catch (e: Exception) {
                logger.error(e) { "Audit log writer step failed" }
            }
        }
    }

    private fun write(batch: List<() -> AuditLogEntry?>) {
        val entries = batch.mapNotNull { build ->
            try {
                build()?.let { it.copy(expireAt = it.at.plus(retention)) }
            } catch (e: Exception) {
                failedCount.incrementAndGet()
                logger.warn(e) { "Could not build an audit log line" }
                null
            }
        }
        if (entries.isEmpty()) return
        try {
            mongoTemplate.insert(entries, AuditLogEntry::class.java)
        } catch (e: Exception) {
            failedCount.addAndGet(entries.size.toLong())
            logger.error(e) { "Could not write ${entries.size} audit log lines" }
        }
    }

    companion object {
        const val BATCH_SIZE = 200
        private const val POLL_MILLIS = 500L
    }
}
