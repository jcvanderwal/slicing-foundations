package de.eventmodelers.support.internal.debug

import org.axonframework.config.Configuration
import org.axonframework.eventhandling.DomainEventMessage
import org.axonframework.eventsourcing.eventstore.EventStorageEngine
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

enum class ElementType {
  EVENT,
  DEAD_LETTER
}

data class FieldInfo(val name: String, val type: String, val example: String)

data class EventMessageInfo(
    val id: String,
    val title: String,
    val fields: List<FieldInfo>,
    val type: ElementType,
    val description: String,
    val highlighted: Boolean = false
)

data class EventsResponse(val events: List<EventMessageInfo>)

@ConditionalOnProperty("application.debug.enabled", havingValue = "true", matchIfMissing = false)
@RestController
class EventsDebugController(
    val eventStorageEngine: EventStorageEngine,
    val configuration: Configuration
) {

  @CrossOrigin
  @GetMapping("/internal/debug/events/{aggregateId}")
  fun resolveEvents(@PathVariable("aggregateId") aggregateId: String): List<DomainEventMessage<*>> {
    return eventStorageEngine.readEvents(aggregateId.toString()).asSequence().toList()
  }

  @CrossOrigin
  @GetMapping("/internal/debug/history/{aggregateId}")
  fun resolveHistory(
      @RequestParam("processingGroup", required = false) processingGroup: String?,
      @PathVariable("aggregateId") aggregateId: String
  ): EventsResponse {
    val domainEvents = eventStorageEngine.readEvents(aggregateId).asSequence().toList()

    val mappedDomainEvents =
        domainEvents.map { event ->
          EventMessageInfo(
              id = event.identifier,
              title = camelCaseToWords(event.payloadType.simpleName),
              fields = extractFields(event.payload),
              type = ElementType.EVENT,
              description = "")
        }

    if (processingGroup != null) {
      val dlq =
          configuration.eventProcessingConfiguration().deadLetterQueue(processingGroup).orElse(null)
      var letters: List<EventMessageInfo> =
          dlq.deadLetters()
              .toList()
              .flatMap { it.asSequence() }
              .filter { it.message() is DomainEventMessage<*> }
              .map { Pair(it.message() as DomainEventMessage<*>, it.cause().orElse(null)) }
              .map {
                EventMessageInfo(
                    it.first.aggregateIdentifier,
                    it.first.payloadType.simpleName,
                    emptyList(),
                    ElementType.EVENT,
                    it.second?.message() ?: "",
                    highlighted = true)
              }

      return EventsResponse(mappedDomainEvents + letters)
    } else {
      return EventsResponse(mappedDomainEvents)
    }
  }

  private fun camelCaseToWords(str: String): String {
    return str.replace(Regex("([a-z])([A-Z])"), "$1 $2")
        .replace(Regex("([A-Z]+)([A-Z][a-z])"), "$1 $2")
        .trim()
  }

  private fun extractFields(payload: Any): List<FieldInfo> {
    return payload::class.java.declaredFields.mapNotNull { field ->
      field.isAccessible = true
      try {
        FieldInfo(
            name = field.name,
            type = field.type.simpleName,
            example = field.get(payload)?.toString() ?: "")
      } catch (e: Exception) {
        null
      }
    }
  }

  private fun extractFieldsFromMap(payload: Map<String, Any?>): List<FieldInfo> {
    return payload.map { (key, value) ->
      FieldInfo(name = key, type = "String", example = value?.toString() ?: "")
    }
  }
}
