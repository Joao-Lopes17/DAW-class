package pt.isel.Controllers

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.*
import org.springframework.web.bind.annotation.*
import pt.isel.*
import pt.isel.model.ChannelInput
import pt.isel.model.Problem

@RestController
@RequestMapping("/api/channels")
class ChannelController(
    private val channelService: ChannelService,
    //private val messageService: MessageService,
) {
    @GetMapping
    fun getAllChannels(): ResponseEntity<List<Channel>> {
        val channels = channelService.getAllChannels()
        return ok(channels)
    }

    @GetMapping("/owner/{username}")
    fun getChannelsByOwner(
        @PathVariable username: String,
    ): ResponseEntity<List<Channel>> {
        val channels = channelService.getChannelsByOwner(username)
        check(channels is Success)
        return if (channels.value.isNotEmpty()) {
            ok(channels.value)
        } else {
            status(HttpStatus.NOT_FOUND).build()
        }
    }

    @GetMapping("/public")
    fun getPublicChannels(): ResponseEntity<List<Channel>> {
        val channels = channelService.getPublicChannels()
        return ok(channels)
    }

    @PostMapping
    fun createChannel(
        user: AuthenticatedUser,
        @RequestBody ch: ChannelInput,
    ): ResponseEntity<Any> {
        val channel: Either<ChannelError, Channel> =
            channelService.createChannel(user.token, ch.name, ch.ownerName, ch.type)
        return when (channel) {
            is Either.Right<Channel> -> ok(channel.value)
            is Either.Left<*> -> Problem.ParticipantNotFound.response(HttpStatus.NOT_FOUND)
        }
    }

    @DeleteMapping("{channelId}")
    fun deleteChannel(
        user: AuthenticatedUser,
        @RequestBody ownername: String,
        @PathVariable channelId: Int,
    ): ResponseEntity<Any>{
        val channelDeleted =
            channelService.deleteChannel(user.token, channelId, ownername)
        return when(channelDeleted){
            is Either.Right<*> -> ok(channelDeleted)
            is Either.Left<*> ->
                when(channelDeleted.value){
                    is ChannelError.ChannelNotFound -> status(HttpStatus.NOT_FOUND).body("Channel not found")
                    is ChannelError.UserNotFound -> status(HttpStatus.NOT_FOUND).body("User not found")
                    is ChannelError.UserNotOwner ->
                        status(HttpStatus.BAD_REQUEST)
                            .body("User is not the owner")
                    else -> status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred")
                }
        }
    }

    @PostMapping("/addParticipant")
    fun addParticipantToChannel(
        @RequestBody req: AddParticipantRequest
    ): ResponseEntity<*> {
        val result = channelService.addParticipantToChannel(req.username, req.channelId, req.permissions)
        return when (result) {
            is Either.Right<*> -> status(HttpStatus.OK).body(result.value)
            is Either.Left<*> ->
                when (result.value) {
                    is ChannelError.ChannelNotFound -> status(HttpStatus.NOT_FOUND).body("Channel not found")
                    is ChannelError.UserNotFound -> status(HttpStatus.NOT_FOUND).body("User not found")
                    is ChannelError.UserIsAlreadyOnChannel ->
                        status(HttpStatus.CONFLICT)
                            .body("User is already a participant in the channel")
                    else -> status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred")
                }
        }
    }

    @GetMapping("/{channelId}")
    fun getChannelById(
        @PathVariable channelId: Int,
    ): ResponseEntity<ChannelInfo> {
        val channel = channelService.getChannelById(channelId)
        return if (channel != null) {
            ResponseEntity.ok(channel)
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        }
    }
}
