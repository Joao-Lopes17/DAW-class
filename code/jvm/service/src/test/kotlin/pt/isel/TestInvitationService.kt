package pt.isel

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import pt.isel.mem.TransactionManagerInMem
import java.util.stream.Stream
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

fun generateRandomToken() = "token-${abs(Random.nextLong())}"

class InvitationServiceTest {
    companion object {
        @JvmStatic
        fun transactionManagers(): Stream<TransactionManager> =
            Stream.of(
                TransactionManagerInMem(), // Uses an in-memory instance for tests
            )

        private fun cleanup(trxManager: TransactionManager) {
            trxManager.run {
                repoInvitation.clear()
                repoUser.clear()
                repoChannel.clear()
            }
        }

        private fun createInvitationService(trxManager: TransactionManager): InvitationService = InvitationService(trxManager)
    }

    @BeforeEach
    fun setup() {
        // Cleanup before each test
        cleanup(TransactionManagerInMem())
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `createInvitation should return a created invitation`(trxManager: TransactionManager) {
        val service = createInvitationService(trxManager)
        val inviter =
            trxManager.run {
                repoUser.createUser("Inviter", PasswordValidationInfo(generateRandomToken()))
            }
        val invitee =
            trxManager.run {
                repoUser.createUser("Invitee", PasswordValidationInfo(generateRandomToken()))
            }

        val result =
            service.createInvitation(
                inviterName = inviter.username,
                inviteeName = invitee.username,
                channelId = 0,
                type = Permissions.READ_ONLY,
            )

        assertTrue(result is Either.Right)
        val invitation = (result as Either.Right).value
        assertEquals(inviter.id, invitation.inviter.id)
        assertEquals(invitee.id, invitation.invitee?.id)
        assertEquals(Permissions.READ_ONLY, invitation.type)
        assertEquals(0, invitation.channelid)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `createInvitation should fail if inviter is not found`(trxManager: TransactionManager) {
        val service = createInvitationService(trxManager)
        val result =
            service.createInvitation(
                inviterName = "999", // Invalid ID
                inviteeName = null,
                channelId = 0,
                type = Permissions.READ_ONLY,
            )

        assertTrue(result is Either.Left)
        assertEquals(InvitationError.InviterNotFound, (result as Either.Left).value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `createInvitation should fail if invitee is not found`(trxManager: TransactionManager) {
        val service = createInvitationService(trxManager)
        val inviter =
            trxManager.run {
                repoUser.createUser("Inviter", PasswordValidationInfo(generateRandomToken()))
            }

        val result =
            service.createInvitation(
                inviterName = inviter.username,
                inviteeName = "999", // Invalid invitee ID
                channelId = 0,
                type = Permissions.READ_ONLY,
            )

        assertTrue(result is Either.Left)
        assertEquals(InvitationError.InviteeNotFound, (result as Either.Left).value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `getInvitationById should return the correct invitation`(trxManager: TransactionManager) {
        val service = createInvitationService(trxManager)
        val inviter =
            trxManager.run {
                repoUser.createUser("Inviter", PasswordValidationInfo(generateRandomToken()))
            }

        val invitation =
            trxManager.run {
                repoInvitation.createInvitation(
                    Invitation(
                        id = 0,
                        code = "test-code",
                        inviter = inviter,
                        invitee = null,
                        channelid = 0,
                        used = false,
                        type = Permissions.READ_ONLY,
                    ),
                )
            }

        val result = service.getInvitationById(0)

        assertTrue(result is Either.Right)
        val fetchedInvitation = (result as Either.Right).value
        assertEquals(0, fetchedInvitation.id)
        assertEquals("test-code", fetchedInvitation.code)
        assertEquals(inviter.id, fetchedInvitation.inviter.id)
        assertEquals(0, fetchedInvitation.channelid)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `getInvitationById should return error if invitation does not exist`(trxManager: TransactionManager) {
        val service = createInvitationService(trxManager)
        val result = service.getInvitationById(999) // Nonexistent ID

        assertTrue(result is Either.Left)
        assertEquals(InvitationError.InvitationNotFound, (result as Either.Left).value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `markInvitationAsUsed should return success if invitation exists`(trxManager: TransactionManager) {
        val service = createInvitationService(trxManager)
        val inviter =
            trxManager.run {
                repoUser.createUser("Inviter", PasswordValidationInfo(generateRandomToken()))
            }

        val invitation =
            trxManager.run {
                repoInvitation.createInvitation(
                    Invitation(
                        id = 0,
                        code = "test-code",
                        inviter = inviter,
                        invitee = null,
                        channelid = 0,
                        used = false,
                        type = Permissions.READ_ONLY,
                    ),
                )
            }

        val result = service.markInvitationAsUsed(id = 0)

        assertTrue(result is Either.Right)
        val updatedInvitation = trxManager.run { repoInvitation.getInvitationByCode("test-code") }
        assertTrue(updatedInvitation?.used == true)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `markInvitationAsUsed should fail if invitation does not exist`(trxManager: TransactionManager) {
        val service = createInvitationService(trxManager)
        val result = service.markInvitationAsUsed(0)

        assertTrue(result is Either.Left)
        assertEquals(InvitationError.InvitationNotFound, (result as Either.Left).value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `get invitations by code`(trxManager: TransactionManager) {
        val service = createInvitationService(trxManager)
        val inviter =
            trxManager.run {
                repoUser.createUser("Inviter", PasswordValidationInfo(generateRandomToken()))
            }

        val invitee =
            trxManager.run {
                repoUser.createUser("Invitee", PasswordValidationInfo(generateRandomToken()))
            }

        val invitation =
            trxManager.run {
                repoInvitation.createInvitation(
                    Invitation(
                        id = 0,
                        code = "test-code",
                        inviter = inviter,
                        invitee = invitee,
                        channelid = 0,
                        used = false,
                        type = Permissions.READ_ONLY,
                    ),
                )
            }

        // Fetch the invitation
        val result = service.getInvitationByCode("test-code")
        assertTrue(result is Either.Right)

        val fetchedInvitation = (result as Either.Right).value
        assertEquals(0, fetchedInvitation.id) // Check if the invitation is the same
        assertEquals(inviter.id, fetchedInvitation.inviter.id) // Check if the inviter is the same
        assertEquals(invitee.id, fetchedInvitation.invitee?.id) // Check if the invitee is the same
        assertEquals(0, fetchedInvitation.channelid) // Check if the channel is the same
        assertEquals(false, fetchedInvitation.used) // Check if the invitation is not used
        assertEquals(Permissions.READ_ONLY, fetchedInvitation.type) // Check if the type is the same
        assertEquals("test-code", fetchedInvitation.code) // Check if the code is the same
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `get invitations by code should return error if invitation does not exist`(trxManager: TransactionManager) {
        val service = createInvitationService(trxManager)
        val result = service.getInvitationByCode("invalid-code")

        assertTrue(result is Either.Left)
        assertEquals(InvitationError.InvitationNotFound, (result as Either.Left).value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `get all invitations`(trxManager: TransactionManager) {
        val service = createInvitationService(trxManager)
        val inviter =
            trxManager.run {
                repoUser.createUser("Inviter", PasswordValidationInfo(generateRandomToken()))
            }

        val invitee =
            trxManager.run {
                repoUser.createUser("Invitee", PasswordValidationInfo(generateRandomToken()))
            }

        val invitation =
            trxManager.run {
                repoInvitation.createInvitation(
                    Invitation(
                        id = 0,
                        code = "test-code",
                        inviter = inviter,
                        invitee = invitee,
                        channelid = 0,
                        used = false,
                        type = Permissions.READ_ONLY,
                    ),
                )
            }

        val result = service.getAllInvitations()
        assertTrue(result.isNotEmpty())
        assertEquals(1, result.size)
        assertEquals(0, result[0].id)
        assertEquals("test-code", result[0].code)
        assertEquals(inviter.id, result[0].inviter.id)
        assertEquals(invitee.id, result[0].invitee?.id)
        assertEquals(0, result[0].channelid)
        assertEquals(false, result[0].used)
        assertEquals(Permissions.READ_ONLY, result[0].type)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `get all invitations should return empty list if there are no invitations`(trxManager: TransactionManager) {
        val service = createInvitationService(trxManager)
        val result = service.getAllInvitations()
        assertTrue(result.isEmpty())
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `accept invitation`(trxManager: TransactionManager) {
        val service = createInvitationService(trxManager)
        val inviter =
            trxManager.run {
                repoUser.createUser("Inviter", PasswordValidationInfo(generateRandomToken()))
            }

        val invitee =
            trxManager.run {
                repoUser.createUser("Invitee", PasswordValidationInfo(generateRandomToken()))
            }
        val channel =
            trxManager.run {
                repoChannel.createChannel("channel1", inviter, ChannelKind.PUBLIC)
            }
        val invitation =
            trxManager.run {
                repoInvitation.createInvitation(
                    Invitation(
                        id = 0,
                        code = "test-code",
                        inviter = inviter,
                        invitee = invitee,
                        channelid = 0,
                        used = false,
                        type = Permissions.READ_ONLY,
                    ),
                )
            }

        val result = service.acceptInvitation(invitee.username, invitation, channel.id, Permissions.READ_ONLY)
        assertTrue(result is Either.Right)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `accept invitation should fail if invitation does not exist`(trxManager: TransactionManager) {
        val service = createInvitationService(trxManager)
        val inviter =
            trxManager.run {
                repoUser.createUser("Inviter", PasswordValidationInfo(generateRandomToken()))
            }
        val channel =
            trxManager.run {
                repoChannel.createChannel("channel1", inviter, ChannelKind.PUBLIC)
            }
        val result = service.acceptInvitation("invalid-code", 0, channel.id, Permissions.READ_ONLY)
        assertTrue(result is Either.Left)
        assertEquals(InvitationError.InvitationNotFound, (result as Either.Left).value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `accept invitation should fail if user does not exist`(trxManager: TransactionManager) {
        val service = createInvitationService(trxManager)
        val inviter =
            trxManager.run {
                repoUser.createUser("Inviter", PasswordValidationInfo(generateRandomToken()))
            }

        val invitee =
            trxManager.run {
                repoUser.createUser("Invitee", PasswordValidationInfo(generateRandomToken()))
            }
        val channel =
            trxManager.run {
                repoChannel.createChannel("channel1", inviter, ChannelKind.PUBLIC)
            }

        val invitation =
            trxManager.run {
                repoInvitation.createInvitation(
                    Invitation(
                        id = 0,
                        code = "test-code",
                        inviter = inviter,
                        invitee = invitee,
                        channelid = 0,
                        used = false,
                        type = Permissions.READ_ONLY,
                    ),
                )
            }

        val result = service.acceptInvitation("fakeInvitee", 0, channel.id, Permissions.READ_ONLY)
        assertTrue(result is Either.Left)
        assertEquals(InvitationError.UserNotFound, (result as Either.Left).value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `reject invitation should fail if invitation does not exist`(trxManager: TransactionManager) {
        val service = createInvitationService(trxManager)
        val result = service.rejectInvitation("invalid-code", 0)
        assertTrue(result is Either.Left)
        assertEquals(InvitationError.InvitationNotFound, (result as Either.Left).value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `reject invitation should fail if user does not exist`(trxManager: TransactionManager) {
        val service = createInvitationService(trxManager)
        val inviter =
            trxManager.run {
                repoUser.createUser("Inviter", PasswordValidationInfo(generateRandomToken()))
            }

        val invitee =
            trxManager.run {
                repoUser.createUser("Invitee", PasswordValidationInfo(generateRandomToken()))
            }

        val invitation =
            trxManager.run {
                repoInvitation.createInvitation(
                    Invitation(
                        id = 0,
                        code = "test-code",
                        inviter = inviter,
                        invitee = invitee,
                        channelid = 0,
                        used = false,
                        type = Permissions.READ_ONLY,
                    ),
                )
            }

        val result = service.rejectInvitation("test-code", 0)
        assertTrue(result is Either.Left)
        assertEquals(InvitationError.UserNotFound, (result as Either.Left).value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `reject invitations should return success if invitation exists`(trxManager: TransactionManager) {
        val service = createInvitationService(trxManager)
        val inviter =
            trxManager.run {
                repoUser.createUser("Inviter", PasswordValidationInfo(generateRandomToken()))
            }
        val invitee =
            trxManager.run {
                repoUser.createUser("Invitee", PasswordValidationInfo(generateRandomToken()))
            }

        val invitation =
            trxManager.run {
                repoInvitation.createInvitation(
                    Invitation(
                        id = 0,
                        code = "test-code",
                        inviter = inviter,
                        invitee = invitee,
                        channelid = 0,
                        used = false,
                        type = Permissions.READ_ONLY,
                    ),
                )
            }

        val result = service.rejectInvitation(invitee.username, 0)

        assertTrue(result is Either.Right)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `reject invitations should return error if invitation does not exist`(trxManager: TransactionManager) {
        val service = createInvitationService(trxManager)
        val result = service.rejectInvitation("invalid-code", 0)

        assertTrue(result is Either.Left)
        assertEquals(InvitationError.InvitationNotFound, (result as Either.Left).value)
    }
}
