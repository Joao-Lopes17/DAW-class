package pt.isel

public final data class acceptInviteRequest(
    val username: String,
    val invitationId: Int,
    val chId: Int,
    val permission: Permissions
)
