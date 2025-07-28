package pt.isel

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.web.reactive.server.WebTestClient
import pt.isel.model.ChannelInput
import pt.isel.model.UserInput
import kotlin.math.abs
import kotlin.random.Random

fun newTokenValidationData() = "token-${abs(Random.nextLong())}"

@SpringBootTest(
    properties = ["spring.main.allow-bean-definition-overriding=true"],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
abstract class AbstractChannelControllerTest {
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
    fun `getAllChannels should return a list of channels`() {
        val userOwner =
            trxManager.run {
                repoUser.createUser(
                    owner.username,
                    PasswordValidationInfo(
                        newTokenValidationData(),
                    ),
                )
            }
        val channel0 =
            trxManager.run { repoChannel.createChannel("Gaming", userOwner, ChannelKind.PRIVATE) }
        val channel1 =
            trxManager.run {
                repoChannel.createChannel(
                    "Status Meeting",
                    userOwner,
                    ChannelKind.PRIVATE,
                )
            }

        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port/api").build()

        // Perform GET request and verify response
        client
            .get()
            .uri("/channels")
            .exchange()
            .expectStatus()
            .isOk
            .expectBodyList(Channel::class.java)
            .hasSize(2)
            .contains(channel1, channel0)
    }

    @Test
    fun `getChannelById should return an channel if found`() {
        val userOwner =
            trxManager.run {
                repoUser.createUser(
                    owner.username,
                    PasswordValidationInfo(newTokenValidationData()),
                )
            }
        val channel0 =
            trxManager.run { repoChannel.createChannel("Gaming", userOwner, ChannelKind.PRIVATE) }

        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port/api").build()

        // Perform GET request and verify response
        client
            .get()
            .uri("/channels/${channel0.id}")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(Channel::class.java)
            .isEqualTo(channel0)
    }

    @Test
    fun `getChannelById should return 404 if channel not found`() {
        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port/api").build()

        // Perform GET request and verify response
        client
            .get()
            .uri("/channels/999")
            .exchange()
            .expectStatus()
            .isNotFound
    }

    @Test
    fun `createChannel should return 201 if channel created successfully`() {
        val userOwner =
            trxManager.run {
                repoUser.createUser(
                    owner.username,
                    PasswordValidationInfo(newTokenValidationData()),
                )
            }
        val channel =
            ChannelInput(
                name = "Fun",
                ownerName = userOwner.username,
                type = ChannelKind.PRIVATE,
            )

        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port/api").build()

        // Perform POST request and verify response
        client
            .post()
            .uri("/channels")
            .bodyValue(channel)
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody(Channel::class.java)
            .also {
                val channelId = trxManager.run { repoChannel.findAll().last().id }
                it.isEqualTo(Channel(channelId, channel.name, userOwner, channel.type))
            }
    }

    @Test
    fun `createChannel should return 404 if organizer not found`() {
        // Mock channel service to return ChannelError.ParticipantNotFound
        val channelInput =
            ChannelInput(
                name = "Fun",
                ownerName = "999",
                type = ChannelKind.PRIVATE,
            )

        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port/api").build()

        // Perform POST request and verify response
        client
            .post()
            .uri("/channels")
            .bodyValue(channelInput)
            .exchange()
            .expectStatus()
            .isNotFound
    }
}
