package pt.isel.model

import java.sql.Timestamp

data class MessageInput(
    //val authToken: String,
    val username: String,
    val channelId: Int,
    val content: String,
)
