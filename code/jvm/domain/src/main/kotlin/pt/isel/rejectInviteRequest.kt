package pt.isel

data class rejectInviteRequest(
    val username: String,
    val invitationId: Int,
)