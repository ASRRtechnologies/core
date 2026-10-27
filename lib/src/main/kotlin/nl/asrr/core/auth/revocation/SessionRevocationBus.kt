/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.auth.revocation

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer

/**
 * Closes the sockets of revoked sessions on every pod. With several replicas a socket lives on
 * whichever one accepted it, so closing locally is not enough: the revocation is published on Redis
 * and each pod closes its own.
 *
 * Pass null for [redisTemplate] and [listenerContainer] when there is no Redis (local dev, a
 * single-pod e2e stack): then only this pod's sockets are closed, which is all there is. Decide that
 * from the app's own flag, not from whether a `StringRedisTemplate` bean exists: Spring Boot
 * auto-configures one against localhost even when nothing runs there, and publishing to it would
 * stall every revocation on a connect timeout.
 *
 * Register it as a bean (Spring calls [subscribe]). Needs `spring-data-redis` when Redis is used.
 */
open class SessionRevocationBus(
    private val registry: LiveSocketRegistry,
    private val redisTemplate: StringRedisTemplate? = null,
    private val listenerContainer: RedisMessageListenerContainer? = null,
    private val channel: String = CHANNEL,
) : MessageListener {
    private val logger = KotlinLogging.logger {}

    @PostConstruct
    fun subscribe() {
        listenerContainer?.addMessageListener(this, ChannelTopic(channel))
    }

    /** Closes the sockets of [authSessionIds] here and on every other pod. */
    fun publish(authSessionIds: Collection<String>) {
        if (authSessionIds.isEmpty()) return
        registry.closeSessions(authSessionIds)
        val template = redisTemplate ?: return
        runCatching { template.convertAndSend(channel, authSessionIds.joinToString(SEPARATOR)) }
            .onFailure { logger.warn { "Could not broadcast ${authSessionIds.size} revoked session(s) to other pods: ${it.message}" } }
    }

    override fun onMessage(message: Message, pattern: ByteArray?) {
        val sessionIds = String(message.body).split(SEPARATOR).filter { it.isNotBlank() }
        // Our own broadcast comes back too; closing an already-closed session is a no-op.
        registry.closeSessions(sessionIds)
    }

    companion object {
        const val CHANNEL = "auth:session-revoked"
        private const val SEPARATOR = ","
    }
}
