package pt.isel

import kotlinx.datetime.Clock
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.http.HttpStatus
import pt.isel.mem.TransactionManagerInMem
import pt.isel.Controllers.InvitationController
import pt.isel.model.InvitationInput
import java.util.stream.Stream
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

fun newInvitationToken() = "token-${abs(Random.nextLong())}"

class InvitationControllerTest {
    companion object {
        private val jdbi =
            Jdbi
                .create(PGSimpleDataSource().apply { setURL(Environment.getDbUrl()) })
                .configureWithAppRequirements()

        @JvmStatic
        fun transactionManagers(): Stream<TransactionManager> =
            Stream.of(
                TransactionManagerInMem().also { cleanup(it) },
                TransactionManagerJdbi(jdbi).also { cleanup(it) },
            )

        private fun cleanup(trxManager: TransactionManager) {
            trxManager.run {
                repoParticipant.clear()
                repoUser.clear()
                repoChannel.clear()
                repoInvitation.clear()
            }
        }

        private fun createInvitationService(trxManager: TransactionManager) = InvitationService(trxManager)

        private fun createInvitationController(trxManager: TransactionManager): InvitationController {
            val invitationService = createInvitationService(trxManager)
            return InvitationController(invitationService)
        }
    }

    @BeforeEach
    fun setup() {
        cleanup(TransactionManagerInMem()) // cleanup in-memory repository
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `createInvitation should return created invitation`(trxManager: TransactionManager) {
        val invitationController = createInvitationController(trxManager)

        val inviter =
            trxManager.run {
                repoUser.createUser("Owner", PasswordValidationInfo(newInvitationToken()))
            }

        val invitee =
            trxManager.run {
                repoUser.createUser("Invitee", PasswordValidationInfo(newInvitationToken()))
            }

        val channel =
            trxManager.run {
                repoChannel.createChannel("test-channel", inviter, ChannelKind.PUBLIC)
            }

        val resp =
            invitationController.createInvitation(
                InvitationInput(
                    inviter.username,
                    invitee.username,
                    channel.id,
                    Permissions.READ_ONLY,
                ),
            )

        assertEquals(HttpStatus.CREATED, resp.statusCode)
        val createdInvitation = resp.body as Invitation
        assertNotNull(createdInvitation)
        assertEquals(inviter.id, createdInvitation.inviter.id)
        assertEquals(invitee.id, createdInvitation.invitee!!.id)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `getAllInvitations should return empty list when no invitations exist`(trxManager: TransactionManager) {
        val invitationController = createInvitationController(trxManager)

        val resp = invitationController.getAllInvitations()

        assertEquals(HttpStatus.OK, resp.statusCode)
        assertEquals(emptyList<Invitation>(), resp.body)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `getInvitationById should return invitation if exists`(trxManager: TransactionManager) {
        val clock = Clock.System
        val invitationController = createInvitationController(trxManager)

        val inviter =
            trxManager.run {
                repoUser.createUser("Owner", PasswordValidationInfo(newInvitationToken()))
            }

        val invitee =
            trxManager.run {
                repoUser.createUser("Invitee", PasswordValidationInfo(newInvitationToken()))
            }

        val channel =
            trxManager.run {
                repoChannel.createChannel("test-channel", inviter, ChannelKind.PUBLIC)
            }

        val createdInvitation =
            trxManager.run {
                repoInvitation.createInvitation(
                    Invitation(
                        id = 0,
                        code = newInvitationToken(),
                        inviter = inviter,
                        invitee = invitee,
                        channelid = channel.id,
                        used = false,
                        type = Permissions.READ_ONLY,
                    ),
                )
            }

        val resp = invitationController.getInvitationById(createdInvitation)

        assertEquals(HttpStatus.OK, resp.statusCode)
        val fetchedInvitation = resp.body as Invitation
        assertNotNull(fetchedInvitation)
        assertEquals(createdInvitation, fetchedInvitation.id)
        assertEquals(inviter.id, fetchedInvitation.inviter.id)
        fetchedInvitation.invitee?.let { assertEquals(invitee.id, it.id) }
        assertEquals(Permissions.READ_ONLY, fetchedInvitation.type)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `getInvitationById should return 404 if invitation does not exist`(trxManager: TransactionManager) {
        val invitationController = createInvitationController(trxManager)

        val resp = invitationController.getInvitationById(999)

        assertEquals(HttpStatus.NOT_FOUND, resp.statusCode)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `acceptInvitation should return OK if invitation is accepted`(trxManager: TransactionManager) {
        val invitationController = createInvitationController(trxManager)

        val inviter = trxManager.run { repoUser.createUser("Owner", PasswordValidationInfo(newInvitationToken())) }
        val invitee = trxManager.run { repoUser.createUser("Invitee", PasswordValidationInfo(newInvitationToken())) }
        val channel = trxManager.run { repoChannel.createChannel("test-channel", inviter, ChannelKind.PUBLIC) }
        val code = newInvitationToken()

        val invitationId = trxManager.run {
            repoInvitation.createInvitation(
                Invitation(0, code, inviter, invitee, channel.id, false, Permissions.READ_ONLY),
            )
        }

        val createdInvitation = trxManager.run { repoInvitation.getInvitationByCode(code) }
        val inviteRequest = acceptInviteRequest(
            invitee.username,
            invitationId,
            channel.id,
            Permissions.READ_ONLY
        )

        val response = invitationController.acceptInvitation(inviteRequest)

        assertEquals(HttpStatus.OK, response.statusCode)
        val acceptedInvitation = response.body as Invitation
        assertNotNull(acceptedInvitation)
        if (createdInvitation != null) {
            assertEquals(createdInvitation.id, acceptedInvitation.id)
        }
        assertEquals(invitee.id, acceptedInvitation.invitee?.id)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `rejectInvitation should return OK if invitation is rejected`(trxManager: TransactionManager) {
        val invitationController = createInvitationController(trxManager)

        val inviter = trxManager.run { repoUser.createUser("Owner", PasswordValidationInfo(newInvitationToken())) }
        val invitee = trxManager.run { repoUser.createUser("Invitee", PasswordValidationInfo(newInvitationToken())) }
        val channel = trxManager.run { repoChannel.createChannel("test-channel", inviter, ChannelKind.PUBLIC) }
        val code = newInvitationToken()

        val invitationId = trxManager.run {
            repoInvitation.createInvitation(
                Invitation(0, code, inviter, invitee, channel.id, false, Permissions.READ_ONLY),
            )
        }

        val createdInvitation = trxManager.run { repoInvitation.getInvitationByCode(code) }
        val rejectInviteRequest = rejectInviteRequest(
            invitee.username,
            invitationId
        )
        val response = invitationController.rejectInvitation(rejectInviteRequest)

        assertEquals(HttpStatus.OK, response.statusCode)
        val rejectedInvitation = response.body as Invitation
        assertNotNull(rejectedInvitation)

        if (createdInvitation != null) {
            assertEquals(createdInvitation.id, rejectedInvitation.id)
        }
        assertEquals(invitee.id, rejectedInvitation.invitee?.id)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `markInvitationAsUsed should return OK if invitation is marked as used`(trxManager: TransactionManager) {
        val invitationController = createInvitationController(trxManager)

        val inviter = trxManager.run { repoUser.createUser("Owner", PasswordValidationInfo(newInvitationToken())) }
        val invitee = trxManager.run { repoUser.createUser("Invitee", PasswordValidationInfo(newInvitationToken())) }
        val channel = trxManager.run { repoChannel.createChannel("test-channel", inviter, ChannelKind.PUBLIC) }
        val code = newInvitationToken()

       val invitationId = trxManager.run {
            repoInvitation.createInvitation(
                Invitation(0, code, inviter, invitee, channel.id, false, Permissions.READ_ONLY),
            )
        }

        val createdInvitation = trxManager.run { repoInvitation.getInvitationByCode(code) }

        val response = invitationController.markInvitationAsUsed(invitationId)

        assertEquals(HttpStatus.OK, response.statusCode)
        val usedInvitation = response.body as Invitation
        assertNotNull(usedInvitation)

        if (createdInvitation != null) {
            assertEquals(createdInvitation.id, usedInvitation.id)
        }
        assertTrue(usedInvitation.used)
    }
}
