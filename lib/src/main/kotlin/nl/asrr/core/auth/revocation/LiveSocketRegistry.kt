/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.auth.revocation

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

/**
 * The WebSockets open on this pod, by the sign-in session they authenticated with.
 *
 * A socket is checked only at the handshake, so revoking its session leaves it open: chat and
 * collaboration keep streaming to someone whose access was just withdrawn. Register each socket
 * here at the handshake; revocation closes the sockets of the revoked sessions, and
 * [SessionRevocationBus] tells the other pods to do the same.
 *
 * Needs `spring-websocket` on the classpath; core only compiles against it.
 */
open class LiveSocketRegistry {
    private val logger = KotlinLogging.logger {}
    private val socketsBySession = ConcurrentHashMap<String, MutableSet<WebSocketSession>>()

    fun register(authSessionId: String, socket: WebSocketSession) {
        // Closed sockets are dropped lazily here instead of from every handler's close callback.
        socketsBySession.values.forEach { sockets -> sockets.removeIf { !it.isOpen } }
        socketsBySession.entries.removeIf { it.value.isEmpty() }
        socketsBySession.computeIfAbsent(authSessionId) { ConcurrentHashMap.newKeySet() }.add(socket)
    }

    /** Closes this pod's sockets of [authSessionIds]. Returns how many were open. */
    fun closeSessions(authSessionIds: Collection<String>): Int {
        var closed = 0
        for (sessionId in authSessionIds) {
            val sockets = socketsBySession.remove(sessionId) ?: continue
            for (socket in sockets) {
                if (!socket.isOpen) continue
                runCatching { socket.close(SESSION_REVOKED) }
                    .onSuccess { closed++ }
                    .onFailure { logger.warn { "Could not close socket ${socket.id} of revoked session: ${it.message}" } }
            }
        }
        if (closed > 0) logger.info { "Closed $closed WebSocket(s) of ${authSessionIds.size} revoked session(s)" }
        return closed
    }

    fun openSocketCount(authSessionId: String): Int = socketsBySession[authSessionId]?.count { it.isOpen } ?: 0

    companion object {
        /** Application close code: the client should not reconnect with the same token. */
        val SESSION_REVOKED = CloseStatus(4401, "Session revoked")
    }
}
