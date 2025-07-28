package pt.isel

import java.sql.Timestamp

interface RepositoryMessage : Repository<Message> {
    fun createMessage(
        sender: User,
        channel: Channel,
        content: String,

    ): Message

    fun findUserMessagesInChannel(
        user: User,
        channel: Channel,
    ): List<Message>

    fun getLatestMessages(channel: Channel): List<Message>

    fun deleteMessage(message: Message)

    fun findByChannel(channel: Channel): List<Message>
}
