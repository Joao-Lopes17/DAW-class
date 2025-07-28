@file:Suppress("ktlint:standard:no-wildcard-imports")

package pt.isel.Controllers

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import pt.isel.AuthenticatedUser
import pt.isel.Either
import pt.isel.MessageError
import pt.isel.MessageService
import pt.isel.model.MessageInput

@RestController
@RequestMapping("/api/channel/{channelId}/messages")
class MessageController(
    private val messageService: MessageService,
) {
    @PostMapping
    fun createMessage(
        user: AuthenticatedUser,
        @RequestBody messageInput: MessageInput,
    ): ResponseEntity<Any> {
        val result =
            messageService.createMessage(
                user.token,
                messageInput.username,
                messageInput.channelId,
                messageInput.content,
            )

        // Dealing with the result
        return when (result) {
            is Either.Right -> ResponseEntity.status(HttpStatus.CREATED).body(result.value)
            is Either.Left ->
                when (result.value) {
                    is MessageError.ChannelNotFound ->
                        ResponseEntity.status(HttpStatus.NOT_FOUND).body("The specified channel does not exist.")
                    is MessageError.UserNotFound ->
                        ResponseEntity.status(HttpStatus.NOT_FOUND).body("The specified user does not exist.")
                    is MessageError.UserIsNotParticipant ->
                        ResponseEntity.status(HttpStatus.FORBIDDEN).body("The user is not a participant in this channel.")
                    is MessageError.MessageNotFound ->
                        ResponseEntity.status(HttpStatus.NOT_FOUND).body("The specified message does not exist.")
                    is MessageError.UserDoesntHavePermission ->
                        ResponseEntity.status(HttpStatus.FORBIDDEN).body("User does not have permission.")
                    else ->
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.")
                }
        }
    }

    @GetMapping
    fun getMessagesByChannelId(
        @PathVariable channelId: Int,
    ): ResponseEntity<Any> {
        val result = messageService.getMessagesByChannelId(channelId)
        return when (result) {
            is Either.Right -> ResponseEntity.status(HttpStatus.OK).body(result.value)
            is Either.Left -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("pt.isel.Channel not found")
        }
    }

    @DeleteMapping("/{messageId}")
    fun deleteMessage(
        @PathVariable messageId: Int,
    ): ResponseEntity<Any> {
        val result = messageService.deleteMessage(messageId)
        return when (result) {
            is Either.Right -> ResponseEntity.status(HttpStatus.NO_CONTENT).build()
            is Either.Left ->
                when (result.value) {
                    is MessageError.MessageNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("pt.isel.Message not found")
                    else -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred")
                }
        }
    }

/*    @GetMapping("/sse")
    fun streamMessages(@PathVariable channelId: Int): SseEmitter {
        return messageService.streamMessages(channelId)
    }*/
}
