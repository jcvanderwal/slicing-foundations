package de.eventmodelers.support.notifications.internal

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
@CrossOrigin
class UINotificationResource(private val sseNotificationService: SseNotificationService) {

  @GetMapping("/subscribe/{sessionId}", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
  fun subscribe(
      @PathVariable sessionId: String,
      @RequestParam(required = false, defaultValue = "0") timeout: Long
  ): SseEmitter {
    return sseNotificationService.subscribe(sessionId, timeout)
  }

  @PostMapping("/send/{sessionId}")
  fun sendNotification(
      @PathVariable sessionId: String,
      @RequestBody request: SendNotificationRequest?
  ): ResponseEntity<NotificationResponse> {
    val notification =
        Notification(
            type = request?.type ?: "message",
            message = request?.message ?: "",
            payload = request?.payload ?: emptyMap())

    val sent = sseNotificationService.sendNotification(sessionId, notification)

    return if (sent) {
      ResponseEntity.ok(NotificationResponse(success = true, message = "Notification sent"))
    } else {
      ResponseEntity.ok(
          NotificationResponse(success = false, message = "No active connection for session"))
    }
  }

  @PostMapping("/broadcast")
  fun broadcast(
      @RequestBody request: SendNotificationRequest
  ): ResponseEntity<NotificationResponse> {
    val notification =
        Notification(
            type = request.type ?: "notification",
            message = request.message,
            payload = request.payload ?: emptyMap())

    sseNotificationService.broadcast(notification)
    return ResponseEntity.ok(
        NotificationResponse(
            success = true,
            message =
                "Notification broadcast to ${sseNotificationService.getActiveSessionCount()} clients"))
  }

  @GetMapping("/status/{sessionId}")
  fun getConnectionStatus(@PathVariable sessionId: String): ResponseEntity<ConnectionStatus> {
    return ResponseEntity.ok(
        ConnectionStatus(
            sessionId = sessionId,
            connected = sseNotificationService.isConnected(sessionId),
            activeConnections = sseNotificationService.getActiveSessionCount()))
  }
}

data class SendNotificationRequest(
    val message: String,
    val type: String? = null,
    val payload: Map<String, Any?>? = null
)

data class NotificationResponse(val success: Boolean, val message: String)

data class ConnectionStatus(
    val sessionId: String,
    val connected: Boolean,
    val activeConnections: Int
)
