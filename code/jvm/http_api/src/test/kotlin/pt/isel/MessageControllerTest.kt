package pt.isel

import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import pt.isel.mem.TransactionManagerInMem
import pt.isel.Controllers.MessageController
import pt.isel.model.MessageInput
import java.sql.Timestamp
import java.util.stream.Stream
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.assertEquals

fun newMessageTokenValidationData() = "token-${abs(Random.nextLong())}"

class MessageControllerTest {
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

        private fun createMessageService(trxManager: TransactionManager) = MessageService(trxManager, Sha256TokenEncoder())

        private fun createMessageController(trxManager: TransactionManager): MessageController {
            val messageService = createMessageService(trxManager)
            return MessageController(messageService)
        }
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `createMessage should return created message`(trxManager: TransactionManager) {
        val messageController = createMessageController(trxManager)

        val owner =
            trxManager.run {
                repoUser.createUser("Owner", PasswordValidationInfo(newMessageTokenValidationData()))
            }
        val channel =
            trxManager.run {
                repoChannel.createChannel("test-channel", owner, ChannelKind.PUBLIC)
            }
        val participant =
            trxManager.run {
                repoUser.createUser("pt.isel.Participant", PasswordValidationInfo(newMessageTokenValidationData()))
            }
        trxManager.run {
            repoParticipant.createParticipant(channel, participant, Permissions.READ_ONLY)
        }

        val authToken = newTokenValidationData()
        val authenticatedUser = AuthenticatedUser(
            participant,
            authToken
        )
        val messageInput =
            MessageInput(participant.username, channel.id, "Hello, World!")
        val resp = messageController.createMessage(authenticatedUser, messageInput)

        assertEquals(HttpStatus.CREATED, resp.statusCode)
        val createdMessage = resp.body as Message
        assertEquals(messageInput.content, createdMessage.content)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `createMessage should return channelNotFound if channel doesn't exist`(trxManager: TransactionManager) {
        val messageController = createMessageController(trxManager)

        val owner =
            trxManager.run {
                repoUser.createUser("Owner", PasswordValidationInfo(newMessageTokenValidationData()))
            }
        val channel =
            trxManager.run {
                repoChannel.createChannel("test-channel", owner, ChannelKind.PUBLIC)
            }
        val participant =
            trxManager.run {
                repoUser.createUser("pt.isel.Participant", PasswordValidationInfo(newMessageTokenValidationData()))
            }
        trxManager.run {
            repoParticipant.createParticipant(channel, participant, Permissions.READ_ONLY)
        }

        val authToken = newTokenValidationData()
        val authenticatedUser = AuthenticatedUser(
            participant,
            authToken
        )
        val messageInput =
            MessageInput(participant.username, channelId = 9999, "Hello, World!")
        val resp = messageController.createMessage(authenticatedUser, messageInput)

        assertEquals(HttpStatus.NOT_FOUND, resp.statusCode)
        assertEquals(ResponseEntity.status(HttpStatus.NOT_FOUND).body("pt.isel.Channel not found"), resp.body)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `createMessage should return userNotFound if user doesn't exist`(trxManager: TransactionManager) {
        val messageController = createMessageController(trxManager)

        val owner =
            trxManager.run {
                repoUser.createUser("Owner", PasswordValidationInfo(newMessageTokenValidationData()))
            }
        val channel =
            trxManager.run {
                repoChannel.createChannel("test-channel", owner, ChannelKind.PUBLIC)
            }
        val participant =
            trxManager.run {
                repoUser.createUser("pt.isel.Participant", PasswordValidationInfo(newMessageTokenValidationData()))
            }
        trxManager.run {
            repoParticipant.createParticipant(channel, participant, Permissions.READ_ONLY)
        }

        val authToken = newTokenValidationData()
        val authenticatedUser = AuthenticatedUser(
            participant,
            authToken
        )

        val messageInput =
            MessageInput(username = "9999", channel.id, "Hello, World!")
        val resp = messageController.createMessage(authenticatedUser, messageInput)

        assertEquals(HttpStatus.NOT_FOUND, resp.statusCode)
        assertEquals(ResponseEntity.status(HttpStatus.NOT_FOUND).body("pt.isel.User not found"), resp.body)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `getMessagesByChannelId should return messages`(trxManager: TransactionManager) {
        val messageController = createMessageController(trxManager)

        val owner =
            trxManager.run {
                repoUser.createUser("Owner", PasswordValidationInfo(newMessageTokenValidationData()))
            }
        val channel =
            trxManager.run {
                repoChannel.createChannel("test-channel", owner, ChannelKind.PUBLIC)
            }
        val participant =
            trxManager.run {
                repoUser.createUser("pt.isel.Participant", PasswordValidationInfo(newMessageTokenValidationData()))
            }
        trxManager.run {
            repoParticipant.createParticipant(channel, participant, Permissions.READ_ONLY)
        }

        // Create a message to test retrieval
        trxManager.run {
            repoMessage.createMessage(participant, channel, "First message")
            repoMessage.createMessage(participant, channel, "Second message")
        }

        val resp = messageController.getMessagesByChannelId(channel.id)

        assertEquals(HttpStatus.OK, resp.statusCode)
        assertEquals(2, (resp.body as List<*>).size)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `deleteMessage should remove a message`(trxManager: TransactionManager) {
        val messageController = createMessageController(trxManager)

        val owner =
            trxManager.run {
                repoUser.createUser("Owner", PasswordValidationInfo(newMessageTokenValidationData()))
            }
        val channel =
            trxManager.run {
                repoChannel.createChannel("test-channel", owner, ChannelKind.PUBLIC)
            }
        val participant =
            trxManager.run {
                repoUser.createUser("pt.isel.Participant", PasswordValidationInfo(newMessageTokenValidationData()))
            }
        trxManager.run {
            repoParticipant.createParticipant(channel, participant, Permissions.READ_ONLY)
        }

        val message =
            trxManager.run {
                repoMessage.createMessage(participant, channel, "pt.isel.Message to delete")
            }

        val resp = messageController.deleteMessage(message.id)

        assertEquals(HttpStatus.NO_CONTENT, resp.statusCode)

        // Verify that the message was actually deleted
        val getMessageResp = messageController.getMessagesByChannelId(channel.id)
        val messages = getMessageResp.body as List<*>
        assertEquals(0, messages.size)
    }
}
