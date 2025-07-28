package pt.isel

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.web.reactive.server.WebTestClient
import pt.isel.model.MessageInput
import pt.isel.model.UserInput
import java.sql.Timestamp
import kotlin.test.assertEquals

@SpringBootTest(
    properties = ["spring.main.allow-bean-definition-overriding=true"],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
abstract class AbstractMessageControllerTest {
    // Injected by the test environment
    @LocalServerPort
    var port: Int = 0

    @Autowired
    private lateinit var trxManager: TransactionManager

    val owner =
        UserInput(
            username = "The owner",
            "password",
        )

    @BeforeEach
    fun setUp() {
        trxManager.run {
            repoParticipant.clear()
            repoMessage.clear()
            repoChannel.clear()
            repoUser.clear()
        }
    }

    @Test
    fun `createMessage should return 201 if message created successfully`() {
        val userOwner =
            trxManager.run {
                repoUser.createUser(
                    owner.username,
                    PasswordValidationInfo(newTokenValidationData()),
                )
            }

        val channel =
            trxManager.run {
                repoChannel.createChannel(
                    "Fun",
                    userOwner,
                    ChannelKind.PUBLIC,
                )
            }

        val participant =
            trxManager.run {
                repoParticipant.createParticipant(channel, userOwner, Permissions.READ_WRITE)
            }

        val authToken = "validToken" // Replace with actual token generation logic

        val message =
            MessageInput(
                username = userOwner.username,
                channelId = channel.id,
                content = "Hi everyone!",
            )

        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port/api").build()

        // Perform POST request and verify response
        client
            .post()
            .uri("/channel/${channel.id}/messages")
            .bodyValue(message)
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody(Message::class.java)
            .value {
                assertEquals(message.username, it.user.username)
                assertEquals(message.channelId, it.channel.id)
                assertEquals(message.content, it.content)
            }
    }

    @Test
    fun `createMessage should return 404 if channel wasn't found`() {
        val userOwner =
            trxManager.run {
                repoUser.createUser(
                    owner.username,
                    PasswordValidationInfo(newTokenValidationData()),
                )
            }

        val authToken = "validToken" // Replace with actual token generation logic

        val messageInput =
            MessageInput(
                username = userOwner.username,
                channelId = 999,
                content = "Hi everyone!",
            )

        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port/api").build()

        // Perform POST request and verify response
        client
            .post()
            .uri("/channel/${messageInput.channelId}/messages")
            .bodyValue(messageInput)
            .exchange()
            .expectStatus()
            .isNotFound
    }

    @Test
    fun `createMessage should return 404 if user wasn't found`() {
        val userOwner =
            trxManager.run {
                repoUser.createUser(
                    owner.username,
                    PasswordValidationInfo(newTokenValidationData()),
                )
            }

        val channel =
            trxManager.run {
                repoChannel.createChannel(
                    "Fun",
                    userOwner,
                    ChannelKind.PRIVATE,
                )
            }

        val authToken = "validToken" // Replace with actual token generation logic

        val messageInput =
            MessageInput(
                username = "999",
                channelId = channel.id,
                content = "Hi everyone!",
            )

        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port/api").build()

        // Perform POST request and verify response
        client
            .post()
            .uri("/channel/${channel.id}/messages")
            .bodyValue(messageInput)
            .exchange()
            .expectStatus()
            .isNotFound
    }
}
