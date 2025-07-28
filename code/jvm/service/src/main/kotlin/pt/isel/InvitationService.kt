package pt.isel

import jakarta.inject.Named
import java.util.UUID

sealed class InvitationError {
    data object InviterNotFound : InvitationError()

    data object InviteeNotFound : InvitationError()

    data object InvitationNotFound : InvitationError()

    data object InvalidExpirationDate : InvitationError()

    data object UserNotFound : InvitationError()

    data object ChannelNotFound : InvitationError()
}

@Named
class InvitationService(
    private val trxManager: TransactionManager,
) {
    // Create a new invitation
    fun createInvitation(
        inviterName: String,
        inviteeName: String?,
        channelId: Int,
        type: Permissions,
    ): Either<InvitationError, Invitation> =
        trxManager.run {
            var userToRegistrate = false
            // Fetch inviter
            val inviter =
                repoUser.findUserByUsername(inviterName)
                    ?: return@run Either.Left(InvitationError.InviterNotFound)

            // Fetch invitee (if provided)
            val invitee =
                inviteeName?.let { username ->
                    repoUser.findUserByUsername(username)
                }
            if (inviteeName == ""){
                userToRegistrate = true
            }
            if (inviteeName != null && invitee == null && userToRegistrate == false) {
                return@run Either.Left(InvitationError.InviteeNotFound)
            }

            // Generate invitation
            val invitation =
                Invitation(
                    id = 0,
                    code = generateCode(),
                    inviter = inviter,
                    invitee = invitee,
                    channelid = channelId,
                    used = false,
                    type = type,
                )

            // Persist the invitation
            repoInvitation.createInvitation(invitation)
            Either.Right(invitation)
        }

    // Get an invitation by code
    fun getInvitationByCode(code: String): Either<InvitationError, Invitation> =
        trxManager.run {
            val invitation =
                repoInvitation.getInvitationByCode(code)
                    ?: return@run Either.Left(InvitationError.InvitationNotFound)

            Either.Right(invitation)
        }

    // Mark an invitation as used
    fun markInvitationAsUsed(id: Int): Either<InvitationError, Unit> =
        trxManager.run {
            val updated = repoInvitation.markInvitationAsUsed(id)
            if (!updated) {
                return@run Either.Left(InvitationError.InvitationNotFound)
            }
            Either.Right(Unit)
        }

    // Get all invitations
    fun getAllInvitations(): List<Invitation> =
        trxManager.run {
            repoInvitation.getAllInvitations()
        }

    // Get an invitation by id
    fun getInvitationById(invitationId: Int): Either<InvitationError, Invitation> =
        trxManager.run {
            val invitation =
                repoInvitation.getInvitationById(invitationId)
                    ?: return@run Either.Left(InvitationError.InvitationNotFound)

            Either.Right(invitation)
        }

    // Accept an invitation
    fun acceptInvitation(
        username: String,
        id: Int,
        channelId: Int,
        permission: Permissions
    ): Either<InvitationError, Unit> =
        trxManager.run {
            val invitation =
                repoInvitation.getInvitationById(id)
                    ?: return@run Either.Left(InvitationError.InvitationNotFound)
            // Fetch channel
            val channel =
                repoChannel.findById(channelId)
                    ?: return@run failure(InvitationError.ChannelNotFound)
            // Fetch pt.isel.User
            val user =
                repoUser.findUserByUsername(username)
                    ?: return@run failure(InvitationError.UserNotFound)

            // Fetch with token information and validate user
            val authenticatedUser =
                repoUser.findUserByUsername(username)
                    ?: return@run failure(InvitationError.UserNotFound)

            if (invitation.used) {
                return@run Either.Left(InvitationError.InvitationNotFound)
            }

            val updated = repoInvitation.acceptInvitation(authenticatedUser.id, id)
            repoParticipant.createParticipant(channel, user, permission)
            repoInvitation.deleteById(id)
            if (!updated) {
                return@run Either.Left(InvitationError.InvitationNotFound)
            }


            Either.Right(Unit)
        }

    // Reject an invitation
    fun rejectInvitation(
        username: String,
        id: Int,
    ): Either<InvitationError, Unit> =
        trxManager.run {
            val invitation =
                repoInvitation.getInvitationById(id)
                    ?: return@run Either.Left(InvitationError.InvitationNotFound)
            // Fetch with token information and validate user
            val authenticatedUser =
                repoUser.findUserByUsername(username)
                    ?: return@run failure(InvitationError.UserNotFound)
            if (invitation.used) {
                return@run Either.Left(InvitationError.InvitationNotFound)
            }

            val updated = repoInvitation.rejectInvitation(authenticatedUser.id,id)
            repoInvitation.deleteById(id)
            if (!updated) {
                return@run Either.Left(InvitationError.InvitationNotFound)
            }

            Either.Right(Unit)
        }

    // Generate unique invitation code
    private fun generateCode(): String =
        UUID
            .randomUUID()
            .toString()
            .replace("-", "")
            .take(12)

    // Get all invitations by user
    fun getAllinvitationsByUser(username: String):Either<InvitationError,List<Invitation> >  =
        trxManager.run {
            val authenticatedUser =
                repoUser.findUserByUsername(username)
                    ?: return@run failure(InvitationError.InviteeNotFound)

            success( repoInvitation.getAllInvitationsByUser(authenticatedUser.id))
        }
}
