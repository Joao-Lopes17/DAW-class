package pt.isel

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class RepositoryMessageJdbi(
    private val handle: Handle,
) : RepositoryMessage {
    override fun createMessage(
        sender: User,
        channel: Channel,
        content: String,
    ): Message {
        val time = System.currentTimeMillis()
        val timestamp = Timestamp(time)
        val id =
            handle
                .createUpdate(
                    """
            INSERT INTO dbo.messages (user_id, channel_id, content, time_stamp) 
            VALUES (:user_id, :channel_id, :content, :time_stamp)
            """,
                ).bind("user_id", sender.id)
                .bind("channel_id", channel.id)
                .bind("content", content)
                .bind("time_stamp", timestamp)
                .executeAndReturnGeneratedKeys()
                .mapTo(Int::class.java)
                .one()
        return Message(id, sender, channel, content, timestamp)
    }

    override fun findUserMessagesInChannel(
        user: User,
        channel: Channel,
    ): List<Message> =
        handle
            .createQuery(
                """
                SELECT 
                    m.*,
                    u.*,
                    c.*,
                    o.username AS owner_username,
                    o.password_validation AS owner_password                    
                FROM dbo.messages m
                JOIN dbo.users u ON m.user_id = u.id
                JOIN dbo.channels c ON m.channel_id = c.id
                JOIN dbo.users o ON c.owner_id = o.id
                WHERE m.user_id = :user_id AND m.channel_id = :channel_id
               """,
            ).bind("user_id", user.id)
            .bind("channel_id", channel.id)
            .map(MessageMapper())
            .list()

    override fun getLatestMessages(channel: Channel): List<Message> =
        handle
            .createQuery(
                """
                SELECT
                    m.*,
                    c.*,
                    u.*,
                    o.username AS owner_username,
                    o.password_validation AS owner_password
                FROM dbo.messages m
                JOIN dbo.users u ON m.user_id = u.id
                JOIN dbo.channels c ON m.channel_id = c.id
                JOIN dbo.users o ON c.owner_id = o.id
                WHERE m.channel_id = :channel_id
                ORDER BY m.time_stamp DESC
                LIMIT 10
                """,
            ).bind("channel_id", channel.id)
            .map(MessageMapper())
            .list()

    override fun deleteMessage(message: Message) {
        handle
            .createUpdate("DELETE FROM dbo.messages WHERE id = :id")
            .bind("id", message.id)
            .execute()
    }

    override fun findByChannel(channel: Channel): List<Message> =
        handle
            .createQuery(
                """
                SELECT
                    m.*,
                    u.*,
                    c.*,
                    o.username AS owner_username,
                    o.password_validation AS owner_password
                FROM
                    dbo.messages m
                JOIN dbo.users u ON m.user_id = u.id
                JOIN dbo.channels c ON m.channel_id = c.id
                JOIN dbo.users o ON c.owner_id = o.id
                WHERE c.id = :channel_id
                """,
            ).bind("channel_id", channel.id)
            .map(MessageMapper())
            .list()

    override fun findById(id: Int): Message? =
        handle
            .createQuery(
                """
                SELECT 
                    m.*,
                    c.*,
                    u.*,
                    o.username AS owner_username,
                    o.password_validation AS owner_password
                FROM dbo.messages m
                JOIN dbo.channels c ON m.channel_id = c.id
                JOIN dbo.users u ON m.user_id = u.id
                JOIN dbo.users o ON c.owner_id = o.id
                WHERE m.id = :id
            """,
            ).bind("id", id)
            .map(MessageMapper())
            .findOne()
            .orElse(null)

    override fun findAll(): List<Message> =
        handle
            .createQuery(
                """
                        SELECT
                            m.*,
                            u.*,
                            c.*,
                            o.username AS owner_username,
                            o.password_validation AS owner_password
                        FROM
                            dbo.messages m
                        JOIN dbo.users u ON m.user_id = u.id
                        JOIN dbo.channels c ON m.channel_id = c.id
                        JOIN dbo.users o ON c.owner_id = o.id;
                """,
            ).map(MessageMapper())
            .list()

    override fun save(entity: Message) {
        handle
            .createUpdate(
                """
            UPDATE dbo.messages 
            SET user_id = :user_id, channel_id = :channel_id , content = :content, time_stamp = :time_stamp 
            WHERE id = :id
            """,
            ).bind("user_id", entity.user.id)
            .bind("channel_id", entity.channel.id)
            .bind("content", entity.content)
            .bind("time_stamp", entity.time)
            .bind("id", entity.id)
            .execute()
    }

    override fun deleteById(id: Int) {
        handle
            .createUpdate("DELETE FROM dbo.messages WHERE id = :id")
            .bind("id", id)
            .execute()
    }

    override fun clear() {
        handle.createUpdate("DELETE FROM dbo.messages").execute()
    }

    // Mapper for Messages
    private class MessageMapper : RowMapper<Message> {
        override fun map(
            rs: ResultSet,
            ctx: StatementContext,
        ): Message {
            val user =
                User(
                    id = rs.getInt("user_id"),
                    username = rs.getString("username"),
                    PasswordValidationInfo(rs.getString("password_validation")),
                )
            val owner =
                User(
                    id = rs.getInt("owner_id"),
                    username = rs.getString("owner_username"),
                    PasswordValidationInfo(rs.getString("owner_password")),
                )

            val channel =
                Channel(
                    id = rs.getInt("channel_id"),
                    name = rs.getString("name"),
                    owner = owner,
                    type = ChannelKind.valueOf(rs.getString("type")),
                )

            return Message(
                id = rs.getInt("id"),
                user = user,
                channel = channel,
                content = rs.getString("content"),
                time = rs.getTimestamp("time_stamp"),
            )
        }
    }
}
