package pt.isel

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.web.reactive.server.WebTestClient
import pt.isel.model.InvitationInput
import pt.isel.model.UserInput
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest(
    properties = ["spring.main.allow-bean-definition-overriding=true"],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
abstract class AbstractInvitationControllerTest {
    // Injected by the test environment
    @LocalServerPort
    var port: Int = 0

    @Autowired
    private lateinit var trxManager: TransactionManager

    val owner =
        UserInput(
            username = "The owner",
            password = "password",
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
    fun `createInvitation should return 201 if invitation created successfully`() {
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

        val invitation =
            InvitationInput(
                inviterName = userOwner.username,
                inviteeName = null,
                channelId = channel.id,
                type = Permissions.READ_ONLY,
            )

        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port/api").build()

        // Perform POST request and verify response
        client
            .post()
            .uri("/invitations")
            .bodyValue(invitation)
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody(Invitation::class.java)
            .value {
                assertEquals(invitation.inviterName, it.inviter.username)
                assertEquals(invitation.channelId, it.channelid)
                assertEquals(invitation.type, it.type)
            }
    }

    @Test
    fun `acceptInvitation should return 200 if invitation accepted successfully`() {
        val userOwner =
            trxManager.run {
                repoUser.createUser(
                    owner.username,
                    PasswordValidationInfo(newTokenValidationData()),
                )
            }

        val invitee =
            trxManager.run {
                repoUser.createUser(
                    "Invitee",
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

        trxManager.run {
            repoInvitation.createInvitation(
                Invitation(
                    id = 1,
                    code = "accept123",
                    inviter = userOwner,
                    invitee = null,
                    channelid = channel.id,
                    used = false,
                    type = Permissions.READ_WRITE,
                ),
            )
        }

        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port/api").build()

        // Perform POST request and verify response
        client
            .post()
            .uri("/invitations/accept")
            .bodyValue(mapOf("code" to "accept123", "userId" to invitee.id))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(Invitation::class.java)
            .value {
                assertEquals(invitee.id, it.invitee?.id)
                assertTrue(it.used)
            }
    }

    @Test
    fun `rejectInvitation should return 200 if invitation rejected successfully`() {
        val userOwner =
            trxManager.run {
                repoUser.createUser(
                    owner.username,
                    PasswordValidationInfo(newTokenValidationData()),
                )
            }

        val invitee =
            trxManager.run {
                repoUser.createUser(
                    "Invitee",
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

        trxManager.run {
            repoInvitation.createInvitation(
                Invitation(
                    id = 1,
                    code = "reject123",
                    inviter = userOwner,
                    invitee = null,
                    channelid = channel.id,
                    used = false,
                    type = Permissions.READ_WRITE,
                ),
            )
        }

        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port/api").build()

        // Perform POST request and verify response
        client
            .post()
            .uri("/invitations/reject")
            .bodyValue(mapOf("code" to "reject123", "userId" to invitee.id))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(Invitation::class.java)
            .value {
                assertEquals(invitee.id, it.invitee?.id)
                assertTrue(it.used)
            }
    }

    @Test
    fun `createInvitation should return 404 if inviter not found`() {
        val channel =
            trxManager.run {
                repoChannel.createChannel(
                    "Fun",
                    User(
                        id = 999,
                        username = "The owner",
                        passwordValidation = PasswordValidationInfo(newTokenValidationData()),
                    ),
                    ChannelKind.PUBLIC,
                )
            }

        val invitation =
            InvitationInput(
                inviterName = "999",
                inviteeName = null,
                channelId = channel.id,
                type = Permissions.READ_ONLY,
            )

        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port/api").build()

        // Perform POST request and verify response
        client
            .post()
            .uri("/invitations")
            .bodyValue(invitation)
            .exchange()
            .expectStatus()
            .isNotFound
    }

    @Test
    fun `createInvitation should return 404 if channel not found`() {
        val userOwner =
            trxManager.run {
                repoUser.createUser(
                    owner.username,
                    PasswordValidationInfo(newTokenValidationData()),
                )
            }

        val invitation =
            InvitationInput(
                inviterName = userOwner.username,
                inviteeName = null,
                channelId = 999,
                type = Permissions.READ_ONLY,
            )

        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port/api").build()

        // Perform POST request and verify response
        client
            .post()
            .uri("/invitations")
            .bodyValue(invitation)
            .exchange()
            .expectStatus()
            .isNotFound
    }

    @Test
    fun `acceptInvitation should return 400 if invitation already used`() {
        val userOwner =
            trxManager.run {
                repoUser.createUser(
                    owner.username,
                    PasswordValidationInfo(newTokenValidationData()),
                )
            }

        val invitee =
            trxManager.run {
                repoUser.createUser(
                    "Invitee",
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

        trxManager.run {
            repoInvitation.createInvitation(
                Invitation(
                    id = 1,
                    code = "used123",
                    inviter = userOwner,
                    invitee = null,
                    channelid = channel.id,
                    used = true,
                    type = Permissions.READ_WRITE,
                ),
            )
        }

        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port/api").build()

        // Perform POST request and verify response
        client
            .post()
            .uri("/invitations/accept")
            .bodyValue(mapOf("code" to "used123", "userId" to invitee.id))
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    @Test
    fun `rejectInvitation should return 400 if invitation already used`() {
        val userOwner =
            trxManager.run {
                repoUser.createUser(
                    owner.username,
                    PasswordValidationInfo(newTokenValidationData()),
                )
            }

        val invitee =
            trxManager.run {
                repoUser.createUser(
                    "Invitee",
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

        trxManager.run {
            repoInvitation.createInvitation(
                Invitation(
                    id = 1,
                    code = "used123",
                    inviter = userOwner,
                    invitee = null,
                    channelid = channel.id,
                    used = true,
                    type = Permissions.READ_WRITE,
                ),
            )
        }

        // given: an HTTP client
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port/api").build()

        // Perform POST request and verify response
        client
            .post()
            .uri("/invitations/reject")
            .bodyValue(mapOf("code" to "used123", "userId" to invitee.id))
            .exchange()
            .expectStatus()
            .isBadRequest
    }
}
