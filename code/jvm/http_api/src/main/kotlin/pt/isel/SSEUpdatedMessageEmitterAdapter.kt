package pt.isel

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

/*
 * - SseEmitter - Spring MVC type
 * - UpdatedTimeSlotEmitter is our own interface (domain)
 * - SseUpdatedTimeSlotEmitterAdapter is our own type (http) that is adapted to the UpdatedTimeSlotEmitter interface,
 * -  which uses SseEmitter
 * Yuml class diagram:
   [TimeSlotController]->1[EventService]
   [TimeSlotController]-.-new>[SseUpdatedTimeSlotEmitterAdapter]
   [TimeSlotController]-.-new>[SseEmitter]
   [SseUpdatedTimeSlotEmitterAdapter]-.-new>[SseEventBuilder]
   [EventService]->*[UpdatedTimeSlotEmitter]
   [Message]-^[UpdatedTimeSlot]
   [KeepAlive]-^[UpdatedTimeSlot]
   [EventService]-.-new>[Message]
   [EventService]-.-new>[KeepAlive]
   [SseUpdatedTimeSlotEmitterAdapter]-^[UpdatedTimeSlotEmitter]

   [UpdatedTimeSlot]
   [Message|id: Long;slot: TimeSlot]
   [KeepAlive|timestamp: Instant]
   [UpdatedTimeSlotEmitter|emit(signal: UpdatedTimeSlot);onCompletion(callback: () -\> Unit);onError(callback: (Throwable) -\> Unit)]
   [EventService|addEmitter(eventId: Int, emitter: UpdatedTimeSlotEmitter);removeEmitter(eventId: Int, emitter: UpdatedTimeSlotEmitter);sendEventToAll(ev: Event, signal: UpdatedTimeSlot)]
 */
class SseUpdatedMessageEmitterAdapter(
    private val sseEmitter: SseEmitter,
) : UpdatedMessagesEmitter {
    override fun emit(signal: UpdatedMessage) {
        val msg =
            when (signal) {
                is UpdatedMessage.Message ->
                    SseEmitter
                        .event()
                        .id(signal.id.toString())
                        .name("message")
                        .data(signal.message)

                is UpdatedMessage.KeepAlive -> SseEmitter.event().comment(signal.timestamp.epochSeconds.toString())
            }
        sseEmitter.send(msg)
    }

    override fun onCompletion(callback: () -> Unit) {
        sseEmitter.onCompletion(callback)
    }

    override fun onError(callback: (Throwable) -> Unit) {
        sseEmitter.onError(callback)
    }
}