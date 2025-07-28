package pt.isel.mem

import jakarta.inject.Named
import pt.isel.Invitation
import pt.isel.Permissions
import pt.isel.RepositoryInvitation

/**
 * Naive in-memory repository, non thread-safe and basic sequential id.
 * Useful for unit tests purpose.
 */
@Named
class RepositoryInvitationInMem : RepositoryInvitation {
    private val invitations = mutableListOf<Invitation>()

    override fun createInvitation(invitation: Invitation): Int {
        val id = invitations.size
        invitations.add(invitation.copy(id = id))
        return id
    }

    override fun findAll(): List<Invitation> = invitations.toList()

    override fun save(entity: Invitation) {
        invitations.replaceAll { if (it.id == entity.id) entity else it }
    }

    override fun deleteById(id: Int) {
        invitations.removeIf { it.id == id }
    }

    override fun clear() {
        invitations.clear()
    }

    override fun getAllInvitations(): List<Invitation> = invitations.toList()

    override fun getInvitationById(invitationId: Int): Invitation? = invitations.getOrNull(invitationId)

    override fun getInvitationByCode(code: String): Invitation? = invitations.find { it.code == code }

    override fun markInvitationAsUsed(id: Int): Boolean = updateInvitationStatus(id, true)

    override fun getAllInvitationsByUser(userId: Int): List<Invitation> = invitations.filter { it.invitee?.id == userId }

    override fun acceptInvitation(
        userId: Int,
        id: Int,
    ): Boolean = updateInvitationStatus(id, true)

    override fun rejectInvitation(
        userId: Int,
        id: Int
    ): Boolean = updateInvitationStatus(id, true)

    override fun findById(id: Int): Invitation? = invitations.getOrNull(id)

    fun updateInvitationStatus(
        id: Int,
        used: Boolean,
    ): Boolean {
        val invitation = invitations.find { it.id == id } ?: return false
        val updated = invitation.copy(used = used)
        invitations.replaceAll { if (it.id == id) updated else it }
        return true
    }
}
