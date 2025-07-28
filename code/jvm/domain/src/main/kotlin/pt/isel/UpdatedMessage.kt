package pt.isel

import kotlinx.datetime.Instant

sealed interface UpdatedMessage {
    data class Message(
        val id: Long,
        val message: pt.isel.Message,
    ) : UpdatedMessage

    data class KeepAlive(
        val timestamp: Instant,
    ) : UpdatedMessage
}