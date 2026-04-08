package de.eventmodelers.support.notifications.internal

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.axonframework.messaging.unitofwork.CurrentUnitOfWork
import org.springframework.stereotype.Component

/**
 * Annotation to mark methods that should trigger a client notification via SSE.
 *
 * When applied to a method, a notification will be sent to the specified client session after the
 * method executes successfully.
 *
 * The session ID can be resolved in multiple ways:
 * - From a method parameter annotated with @SessionId
 * - From a method parameter named "sessionId"
 * - From a SpEL expression referencing method parameters
 *
 * Example usage:
 * ```
 * @NotifyClient(message = "Account created successfully")
 * fun createAccount(@SessionId sessionId: String, accountData: AccountData) { ... }
 *
 * @NotifyClient(
 *     sessionIdExpression = "#request.sessionId",
 *     message = "Order processed",
 *     type = "order.completed"
 * )
 * fun processOrder(request: OrderRequest) { ... }
 * ```
 *
 * @param message The notification message to send
 * @param type The notification type/event name (default: "notification")
 * @param sessionIdExpression SpEL expression to extract session ID from method parameters
 * @param includeResult If true, includes the method return value in the notification payload
 * @param notifyOnError If true, also sends notification when method throws an exception
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class NotifyClient

@Aspect
@Component
class NotificationInterceptor(val notificationService: SseNotificationService) {
  @Around(
      "@annotation(de.eventmodelers.support.notifications.internal.NotifyClient) && @annotation(org.axonframework.eventhandling.EventHandler)")
  fun aroundEventHandler(joinPoint: ProceedingJoinPoint): Any? {
    val metaData = CurrentUnitOfWork.get().message?.metaData // ... use metadata
    val result = joinPoint.proceed()
    notificationService.broadcast(Notification(type = "message"))
    return result
  }
}
