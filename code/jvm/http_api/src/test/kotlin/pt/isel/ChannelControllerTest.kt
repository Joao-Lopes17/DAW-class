package pt.isel

import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import pt.isel.mem.TransactionManagerInMem
import pt.isel.Controllers.ChannelController
import pt.isel.model.ChannelInput
import java.util.stream.Stream
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

fun newTokenValidationData() = "token-${abs(Random.nextLong())}"
class ChannelControllerTest {
    companion object {
        private val jdbi =
            Jdbi
                .create(
                    PGSimpleDataSource().apply {
                        setURL(Environment.getDbUrl())
                    },
                ).configureWithAppRequirements()

        @JvmStatic
        fun transactionManagers(): Stream<TransactionManager> =
            Stream.of(
                TransactionManagerInMem().also { cleanup(it) },
                TransactionManagerJdbi(jdbi).also { cleanup(it) },
            )

        private fun cleanup(trxManager: TransactionManager) {
            trxManager.run {
                repoMessage.clear()
                repoParticipant.clear()
                repoChannel.clear()
                repoUser.clear()
            }
        }

        private fun createChannelService(trxManager: TransactionManager) = ChannelService(trxManager, Sha256TokenEncoder())

        private fun createMessageService(trxManager: TransactionManager) = MessageService(trxManager, Sha256TokenEncoder())
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `getAllChannels should return a list of channels`(trxManager: TransactionManager) {
        val channelService = createChannelService(trxManager)
        val messageService = createMessageService(trxManager)
        val controllerChannels = ChannelController(channelService, messageService)

        val resp = controllerChannels.getAllChannels()

        assertEquals(HttpStatus.OK, resp.statusCode)
        val channels = resp.body as List<Channel>
        assertEquals(0, channels.size)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `createChannel should return created channel`(trxManager: TransactionManager) {
        val testChannelName = "channel-${abs(Random.nextInt())}"
        val channelService = createChannelService(trxManager)
        val messageService = createMessageService(trxManager)
        val controllerChannels = ChannelController(channelService, messageService)

        val owner =
            trxManager.run {
                repoUser.createUser("Owner", PasswordValidationInfo(newTokenValidationData()))
            }
        val channelInput = ChannelInput(testChannelName, owner.username, ChannelKind.PUBLIC)
        val authToken = newTokenValidationData()
        val authenticatedUser = AuthenticatedUser(
            owner,
            authToken
        )

        val resp = controllerChannels.createChannel(authenticatedUser, channelInput)

        assertEquals(HttpStatus.OK, resp.statusCode)
        val createdChannel = resp.body as Channel
        assertEquals(testChannelName, createdChannel.name)
        assertEquals(owner.id, createdChannel.owner.id)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `createChannel should return participantNotFound if participant doesn't exist`(trxManager: TransactionManager) {
        val testChannelName = "channel-${abs(Random.nextInt())}"
        val channelService = createChannelService(trxManager)
        val messageService = createMessageService(trxManager)
        val controllerChannels = ChannelController(channelService, messageService)
        val user = User(
            0,
            "user",
            PasswordValidationInfo(newTokenValidationData()),
        )
        val channelInput = ChannelInput( testChannelName, "9999", ChannelKind.PUBLIC)
        val authToken = newTokenValidationData()
        val authenticatedUser = AuthenticatedUser(
            user,
            authToken
        )
        val resp = controllerChannels.createChannel(authenticatedUser, channelInput)

        assertEquals(HttpStatus.NOT_FOUND, resp.statusCode)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `deleteChannel should return true if channel was deleted`(trxManager: TransactionManager) {
        val testChannelName = "channel-${abs(Random.nextInt())}"
        val channelService = createChannelService(trxManager)
        val messageService = createMessageService(trxManager)
        val controllerChannels = ChannelController(channelService, messageService)

        val owner =
            trxManager.run {
                repoUser.createUser("Owner", PasswordValidationInfo(newTokenValidationData()))
            }
        val channelInput = ChannelInput(testChannelName, owner.username, ChannelKind.PUBLIC)
        val authToken = newTokenValidationData()
        val authenticatedUser = AuthenticatedUser(
            owner,
            authToken
        )

        val resp = controllerChannels.createChannel(authenticatedUser, channelInput)

        assertEquals(HttpStatus.OK, resp.statusCode)
        val createdChannel = resp.body as Channel
        assertEquals(testChannelName, createdChannel.name)
        assertEquals(owner.id, createdChannel.owner.id)

        val channelDeleted =
            controllerChannels.deleteChannel(
                authenticatedUser, authenticatedUser.user.username, createdChannel.id
            )

        assertEquals(HttpStatus.OK, channelDeleted.statusCode)
        assertTrue(channelDeleted.body as Boolean)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `deleteChannel should return channel not found if channel doesn't exist`(trxManager: TransactionManager) {
        val testChannelName = "channel-${abs(Random.nextInt())}"
        val channelService = createChannelService(trxManager)
        val messageService = createMessageService(trxManager)
        val controllerChannels = ChannelController(channelService, messageService)

        val owner =
            trxManager.run {
                repoUser.createUser("Owner", PasswordValidationInfo(newTokenValidationData()))
            }
        val channelInput = ChannelInput(testChannelName, owner.username, ChannelKind.PUBLIC)
        val authToken = newTokenValidationData()
        val authenticatedUser = AuthenticatedUser(
            owner,
            authToken
        )

        val resp = controllerChannels.createChannel(authenticatedUser, channelInput)

        assertEquals(HttpStatus.OK, resp.statusCode)
        val createdChannel = resp.body as Channel
        assertEquals(testChannelName, createdChannel.name)
        assertEquals(owner.id, createdChannel.owner.id)

        val channelDeleted =
            controllerChannels.deleteChannel(
                authenticatedUser, authenticatedUser.user.username, 9999
            )
        assertEquals(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Channel not found"), resp.body)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `deleteChannel should return userNotFound if user doesn't exist`(trxManager: TransactionManager) {
        val testChannelName = "channel-${abs(Random.nextInt())}"
        val channelService = createChannelService(trxManager)
        val messageService = createMessageService(trxManager)
        val controllerChannels = ChannelController(channelService, messageService)

        val owner =
            trxManager.run {
                repoUser.createUser("Owner", PasswordValidationInfo(newTokenValidationData()))
            }
        val channelInput = ChannelInput(testChannelName, owner.username, ChannelKind.PUBLIC)
        val authToken = newTokenValidationData()
        val authenticatedUser = AuthenticatedUser(
            owner,
            authToken
        )

        val resp = controllerChannels.createChannel(authenticatedUser, channelInput)

        assertEquals(HttpStatus.OK, resp.statusCode)
        val createdChannel = resp.body as Channel
        assertEquals(testChannelName, createdChannel.name)
        assertEquals(owner.id, createdChannel.owner.id)

        val channelDeleted =
            controllerChannels.deleteChannel(
                authenticatedUser, "ZZZZ", createdChannel.id
            )
        assertEquals(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Channel not found"), resp.body)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `deleteChannel should return userNotOwner if user isn't the owner`(trxManager: TransactionManager) {
        val testChannelName = "channel-${abs(Random.nextInt())}"
        val channelService = createChannelService(trxManager)
        val messageService = createMessageService(trxManager)
        val controllerChannels = ChannelController(channelService, messageService)

        val owner =
            trxManager.run {
                repoUser.createUser("Owner", PasswordValidationInfo(newTokenValidationData()))
            }

        val user = trxManager.run {
            repoUser.createUser("User1", PasswordValidationInfo(newTokenValidationData()))
        }
        val channelInput = ChannelInput(testChannelName, owner.username, ChannelKind.PUBLIC)
        val authToken = newTokenValidationData()
        val authenticatedOwner = AuthenticatedUser(
            owner,
            authToken
        )

        val authToken1 = newTokenValidationData()
        val authenticatedUser= AuthenticatedUser(
            user,
            authToken1
        )
        val resp = controllerChannels.createChannel(authenticatedOwner, channelInput)

        assertEquals(HttpStatus.OK, resp.statusCode)
        val createdChannel = resp.body as Channel
        assertEquals(testChannelName, createdChannel.name)
        assertEquals(owner.id, createdChannel.owner.id)

        val channelDeleted =
            controllerChannels.deleteChannel(
                authenticatedUser, authenticatedUser.user.username, createdChannel.id
            )
        assertEquals(ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User is not the owner"), resp.body)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `addParticipantToChannel should add user to channel`(trxManager: TransactionManager) {
        val channelService = createChannelService(trxManager)
        val messageService = createMessageService(trxManager)
        val controllerChannels = ChannelController(channelService, messageService)

        val owner =
            trxManager.run {
                repoUser.createUser("Owner", PasswordValidationInfo(newTokenValidationData()))
            }
        val channel =
            trxManager.run {
                repoChannel.createChannel("test-channel", owner, ChannelKind.PUBLIC)
            }
        val participant =
            trxManager.run {
                repoUser.createUser("pt.isel.Participant", PasswordValidationInfo(newTokenValidationData()))
            }
        val p = AddParticipantRequest(participant.username, channel.id, Permissions.READ_ONLY)

        val resp = controllerChannels.addParticipantToChannel(p)

        assertEquals(HttpStatus.OK, resp.statusCode)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `addParticipantToChannel should return channelNotFound if channel doesn't exits`(trxManager: TransactionManager) {
        val channelService = createChannelService(trxManager)
        val messageService = createMessageService(trxManager)
        val controllerChannels = ChannelController(channelService, messageService)

        val participant =
            trxManager.run {
                repoUser.createUser("pt.isel.Participant", PasswordValidationInfo(newTokenValidationData()))
            }
        val p = AddParticipantRequest(participant.username, 999, Permissions.READ_ONLY)
        val resp = controllerChannels.addParticipantToChannel(p)

        assertEquals(HttpStatus.NOT_FOUND, resp.statusCode)
        assertEquals(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Channel not found"), resp.body)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `addParticipantToChannel should return UserNotFound if user doesn't exits`(trxManager: TransactionManager) {
        val channelService = createChannelService(trxManager)
        val messageService = createMessageService(trxManager)
        val controllerChannels = ChannelController(channelService, messageService)

        val owner =
            trxManager.run {
                repoUser.createUser("Owner", PasswordValidationInfo(newTokenValidationData()))
            }
        val channel =
            trxManager.run {
                repoChannel.createChannel("test-channel", owner, ChannelKind.PUBLIC)
            }
        val p = AddParticipantRequest("no user", channel.id, Permissions.READ_ONLY)
        val resp = controllerChannels.addParticipantToChannel(p)

        assertEquals(HttpStatus.NOT_FOUND, resp.statusCode)
        assertEquals(ResponseEntity.status(HttpStatus.NOT_FOUND).body("pt.isel.User not found"), resp.body)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `addParticipantToChannel should return UserAlreadyOnChannel if user is already on channel`(trxManager: TransactionManager) {
        val channelService = createChannelService(trxManager)
        val messageService = createMessageService(trxManager)
        val controllerChannels = ChannelController(channelService, messageService)

        val owner =
            trxManager.run {
                repoUser.createUser("Owner", PasswordValidationInfo(newTokenValidationData()))
            }
        val channel =
            trxManager.run {
                repoChannel.createChannel("test-channel", owner, ChannelKind.PUBLIC)
            }
        val p = AddParticipantRequest(owner.username, channel.id, Permissions.READ_ONLY)
        val resp = controllerChannels.addParticipantToChannel(p)

        assertEquals(HttpStatus.CONFLICT, resp.statusCode)
        assertEquals(
            ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body("pt.isel.User is already a participant in the channel"),
            resp.body,
        )
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `removeParticipantOfChannel should remove user of channel`(trxManager: TransactionManager) {
        val channelService = createChannelService(trxManager)
        val messageService = createMessageService(trxManager)
        val controllerChannels = ChannelController(channelService, messageService)

        val owner =
            trxManager.run {
                repoUser.createUser("Owner", PasswordValidationInfo(newTokenValidationData()))
            }
        val channel =
            trxManager.run {
                repoChannel.createChannel("test-channel", owner, ChannelKind.PUBLIC)
            }
        val participant =
            trxManager.run {
                repoUser.createUser("pt.isel.Participant", PasswordValidationInfo(newTokenValidationData()))
            }
        val p = AddParticipantRequest(participant.username, channel.id, Permissions.READ_ONLY)

        val addParticipant = controllerChannels.addParticipantToChannel(p)

        assertEquals(HttpStatus.OK, addParticipant.statusCode)

        val result = controllerChannels.removeParticipantOfChannel(p)

        assertEquals(HttpStatus.OK, result.statusCode)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `removeParticipantOfChannel should return channelNotFound if channel doesn't exits`(
        trxManager: TransactionManager
    ) {
        val channelService = createChannelService(trxManager)
        val messageService = createMessageService(trxManager)
        val controllerChannels = ChannelController(channelService, messageService)

        val owner =
            trxManager.run {
                repoUser.createUser("Owner", PasswordValidationInfo(newTokenValidationData()))
            }
        val channel =
            trxManager.run {
                repoChannel.createChannel("test-channel", owner, ChannelKind.PUBLIC)
            }
        val participant =
            trxManager.run {
                repoUser.createUser("pt.isel.Participant", PasswordValidationInfo(newTokenValidationData()))
            }
        val p = AddParticipantRequest(participant.username, channel.id, Permissions.READ_ONLY)

        val addParticipant = controllerChannels.addParticipantToChannel(p)

        assertEquals(HttpStatus.OK, addParticipant.statusCode)

        val ch = AddParticipantRequest(participant.username, 9999, Permissions.READ_ONLY)

        val result = controllerChannels.removeParticipantOfChannel(ch)

        assertEquals(HttpStatus.NOT_FOUND, result.statusCode)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `removeParticipantOfChannel should return userNotFound if user doesn't exits`(
        trxManager: TransactionManager
    ) {
        val channelService = createChannelService(trxManager)
        val messageService = createMessageService(trxManager)
        val controllerChannels = ChannelController(channelService, messageService)

        val owner =
            trxManager.run {
                repoUser.createUser("Owner", PasswordValidationInfo(newTokenValidationData()))
            }
        val channel =
            trxManager.run {
                repoChannel.createChannel("test-channel", owner, ChannelKind.PUBLIC)
            }
        val participant =
            trxManager.run {
                repoUser.createUser("pt.isel.Participant", PasswordValidationInfo(newTokenValidationData()))
            }
        val p = AddParticipantRequest(participant.username, channel.id, Permissions.READ_ONLY)

        val addParticipant = controllerChannels.addParticipantToChannel(p)

        assertEquals(HttpStatus.OK, addParticipant.statusCode)

        val ch = AddParticipantRequest("zzzzz", channel.id, Permissions.READ_ONLY)

        val result = controllerChannels.removeParticipantOfChannel(ch)

        assertEquals(HttpStatus.NOT_FOUND, result.statusCode)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `removeParticipantOfChannel should return userNotOnChannel if user is not on channel`(
        trxManager: TransactionManager
    ) {
        val channelService = createChannelService(trxManager)
        val messageService = createMessageService(trxManager)
        val controllerChannels = ChannelController(channelService, messageService)

        val owner =
            trxManager.run {
                repoUser.createUser("Owner", PasswordValidationInfo(newTokenValidationData()))
            }
        val channel =
            trxManager.run {
                repoChannel.createChannel("test-channel", owner, ChannelKind.PUBLIC)
            }
        val participant =
            trxManager.run {
                repoUser.createUser("pt.isel.Participant", PasswordValidationInfo(newTokenValidationData()))
            }

        val ch = AddParticipantRequest(participant.username, channel.id, Permissions.READ_ONLY)

        val result = controllerChannels.removeParticipantOfChannel(ch)

        assertEquals(HttpStatus.NOT_FOUND, result.statusCode)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `getChannelsByOwner should return all channels of a specific owner`(trxManager: TransactionManager) {
        val channelService = createChannelService(trxManager)
        val messageService = createMessageService(trxManager)
        val controllerChannels = ChannelController(channelService, messageService)

        val owner =
            trxManager.run {
                repoUser.createUser("Owner", PasswordValidationInfo(newTokenValidationData()))
            }
        val createdChannel1 =
            trxManager.run {
                repoChannel.createChannel("Test pt.isel.Channel1", owner, ChannelKind.PUBLIC)
            }
        val createdChannel2 =
            trxManager.run {
                repoChannel.createChannel("Test pt.isel.Channel2", owner, ChannelKind.PRIVATE)
            }

        val resp = controllerChannels.getChannelsByOwner(owner.username)

        assertEquals(HttpStatus.OK, resp.statusCode)
        val channels = resp.body as List<Channel>
        assertEquals(2, channels.size)
        assertEquals(createdChannel1.name, channels[0].name)
        assertEquals(createdChannel2.name, channels[1].name)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `getChannelsByOwner should return empty list if owner has no channels`(trxManager: TransactionManager) {
        val channelService = createChannelService(trxManager)
        val messageService = createMessageService(trxManager)
        val controllerChannels = ChannelController(channelService, messageService)

        val owner =
            trxManager.run {
                repoUser.createUser("OwnerWithoutChannels", PasswordValidationInfo(newTokenValidationData()))
            }

        val resp = controllerChannels.getChannelsByOwner(owner.username)

        assertEquals(HttpStatus.OK, resp.statusCode)
        val channels = resp.body as List<Channel>
        assertEquals(0, channels.size)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `getChannelsByOwner should return UserNotFound if owner does not exist`(trxManager: TransactionManager) {
        val channelService = createChannelService(trxManager)
        val messageService = createMessageService(trxManager)
        val controllerChannels = ChannelController(channelService, messageService)

        val resp = controllerChannels.getChannelsByOwner("no user")

        assertEquals(HttpStatus.NOT_FOUND, resp.statusCode)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `getPublicChannels should return all public channels`(trxManager: TransactionManager) {
        val channelService = createChannelService(trxManager)
        val messageService = createMessageService(trxManager)
        val controllerChannels = ChannelController(channelService, messageService)

        val owner =
            trxManager.run {
                repoUser.createUser("Owner", PasswordValidationInfo(newTokenValidationData()))
            }
        val publicChannel1 =
            trxManager.run {
                repoChannel.createChannel("Public Channel 1", owner, ChannelKind.PUBLIC)
            }
        val publicChannel2 =
            trxManager.run {
                repoChannel.createChannel("Public Channel 2", owner, ChannelKind.PUBLIC)
            }
        val privateChannel =
            trxManager.run {
                repoChannel.createChannel("Private Channel", owner, ChannelKind.PRIVATE)
            }

        val resp = controllerChannels.getPublicChannels()

        assertEquals(HttpStatus.OK, resp.statusCode)
        val channels = resp.body as List<Channel>
        assertEquals(2, channels.size)
        assertEquals(publicChannel1.name, channels[0].name)
        assertEquals(publicChannel2.name, channels[1].name)
    }
}
