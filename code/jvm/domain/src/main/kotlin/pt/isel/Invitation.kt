package pt.isel

data class Invitation(
    val id: Int,
    val code: String,
    val inviter: User,
    val invitee: User? = null,
    val channelid: Int,
    val used: Boolean = false,
    val type: Permissions = Permissions.READ_ONLY, // Permissions { READ_ONLY, READ_WRITE }
)
