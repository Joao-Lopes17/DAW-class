package pt.isel

interface RepositoryInvitation : Repository<Invitation> {
    fun createInvitation(invitation: Invitation): Int

    fun getAllInvitations(): List<Invitation>

    fun getInvitationById(invitationId: Int): Invitation?

    fun getInvitationByCode(code: String): Invitation?

    fun getAllInvitationsByUser(userId: Int): List<Invitation>

    fun markInvitationAsUsed(id: Int): Boolean

    fun acceptInvitation(
        userId: Int,
        id: Int,
    ): Boolean

    fun rejectInvitation(
        userId: Int,
        id: Int
    ): Boolean

    override fun deleteById(id: Int)
}
