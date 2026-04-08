package de.eventmodelers.support.notifications.internal

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@Service
class SseNotificationService {

  private val logger = KotlinLogging.logger {}
  private val emitters = ConcurrentHashMap<String, SseEmitter>()

  fun subscribe(sessionId: String, timeout: Long = 0L): SseEmitter {
    val emitter = SseEmitter(timeout)

    emitter.onCompletion {
      logger.info { "SSE connection completed for session: $sessionId" }
      emitters.remove(sessionId)
    }

    emitter.onTimeout {
      logger.info { "SSE connection timed out for session: $sessionId" }
      emitters.remove(sessionId)
    }

    emitter.onError { ex ->
      logger.warn { "SSE connection error for session: $sessionId - ${ex.message}" }
      emitters.remove(sessionId)
    }

    emitters[sessionId] = emitter
    logger.info { "Client subscribed with session: $sessionId" }

    return emitter
  }

  fun sendNotification(sessionId: String, notification: Notification): Boolean {
    val emitter = emitters[sessionId]
    if (emitter == null) {
      logger.warn { "No active SSE connection for session: $sessionId" }
      return false
    }

    return try {
      emitter.send(
          SseEmitter.event().id(notification.id).name(notification.type).data(notification))
      logger.debug { "Notification sent to session $sessionId: ${notification.message}" }
      true
    } catch (ex: Exception) {
      logger.error { "Failed to send notification to session $sessionId: ${ex.message}" }
      emitters.remove(sessionId)
      false
    }
  }

  fun broadcast(notification: Notification) {
    val deadEmitters = mutableListOf<String>()

    emitters.forEach { (sessionId, emitter) ->
      try {
        emitter.send(
            SseEmitter.event().id(notification.id).name(notification.type).data(notification))
      } catch (ex: Exception) {
        logger.warn { "Failed to broadcast to session $sessionId, marking for removal" }
        deadEmitters.add(sessionId)
      }
    }

    deadEmitters.forEach { emitters.remove(it) }
    logger.debug { "Broadcast notification to ${emitters.size} clients" }
  }

  fun isConnected(sessionId: String): Boolean = emitters.containsKey(sessionId)

  fun getActiveSessionCount(): Int = emitters.size
}

data class Notification(
    val id: String = UUID.randomUUID().toString(),
    val type: String = "message",
    val message: String = "",
    val payload: Map<String, Any?> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)
