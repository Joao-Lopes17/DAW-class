package pt.isel.model

import pt.isel.Permissions

class InvitationInput(
    val inviterName: String,
    val inviteeName: String? = "",
    val channelId: Int,
    val type: Permissions = Permissions.READ_ONLY,
)
