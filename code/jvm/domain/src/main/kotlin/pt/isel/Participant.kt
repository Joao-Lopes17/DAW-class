package pt.isel

/**
 * Represents a participant in a pt.isel.Channel
 */
data class Participant(
    val id: Int,
    val channel: Channel,
    val user: User,
    val permissions: Permissions,
)