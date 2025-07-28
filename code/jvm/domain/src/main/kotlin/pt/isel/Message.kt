package pt.isel

import java.sql.Timestamp

data class Message(
    val id: Int,
    val user: User,
    val channel: Channel,
    val content: String,
    val time: Timestamp,
)
