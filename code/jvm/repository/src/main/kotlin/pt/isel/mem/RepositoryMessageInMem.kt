package pt.isel.mem

import pt.isel.Channel
import pt.isel.Message
import pt.isel.RepositoryMessage
import pt.isel.User
import java.sql.Timestamp

class RepositoryMessageInMem : RepositoryMessage {
    private val messages = mutableListOf<Message>()

    override fun createMessage(
        sender: User,
        channel: Channel,
        content: String,
    ): Message {
        val time = System.currentTimeMillis()
        val timestamp = Timestamp(time)
        val message = Message(messages.size, sender, channel, content, timestamp)
        messages.add(message)
        return message
    }

    override fun findUserMessagesInChannel(
        user: User,
        channel: Channel,
    ): List<Message> =
        messages.filter {
            it.user == user && it.channel == channel
        }

    override fun getLatestMessages(channel: Channel): List<Message> = messages.filter { it.channel == channel }.sortedByDescending { it.id }

    override fun deleteMessage(message: Message) {
        messages.removeIf { it.id == message.id }
    }

    override fun findByChannel(channel: Channel): List<Message> = messages.filter { it.channel == channel }

    override fun findById(id: Int): Message? = messages.firstOrNull { it.id == id }

    override fun findAll(): List<Message> = messages.toList()

    override fun save(entity: Message) {
        messages.removeIf { it.id == entity.id }
        messages.add(entity)
    }

    override fun deleteById(id: Int) {
        messages.removeIf { it.id == id }
    }

    override fun clear() {
        messages.clear()
    }
}
