package pt.isel

data class AddParticipantRequest(
    val username: String,
    val channelId: Int,
    val permissions: Permissions// Match the string sent
)