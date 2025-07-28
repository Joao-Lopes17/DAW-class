package pt.isel

import org.jdbi.v3.core.Handle
import java.sql.ResultSet

class RepositoryParticipantJdbi(
    private val handle: Handle,
) : RepositoryParticipant {
    override fun findById(id: Int): Participant? =
        handle
            .createQuery(
                """
            SELECT 
            p.*,
            u.*,
            c.*,
            o.username AS owner_username,
            o.password_validation AS owner_password            
            FROM dbo.participants p
            JOIN dbo.users u ON p.user_id = u.id
            JOIN dbo.channels c ON p.channel_id = c.id
            INNER JOIN dbo.users o ON c.owner_id = o.id
            WHERE p.id = :id
            """,
            ).bind("id", id)
            .map { rs, _ -> mapRowToParticipant(rs) }
            .findOne()
            .orElse(null)

    override fun findAll(): List<Participant> =
        handle
            .createQuery(
                """
            SELECT 
                p.*,
                u.*,
                c.*,
                o.username AS owner_username,
                o.password_validation AS owner_password
            FROM dbo.participants p
            JOIN dbo.users u ON p.user_id = u.id
            JOIN dbo.channels c ON p.channel_id = c.id
            INNER JOIN dbo.users o ON c.owner_id = o.id
            """,
            ).map { rs, _ -> mapRowToParticipant(rs) }
            .list()

    override fun save(entity: Participant) {
        handle
            .createUpdate(
                """
            UPDATE dbo.participants 
            SET user_id = :user_id, channel_id = :channel_id, permission = :permission
            WHERE id = :id
            """,
            ).bind("id", entity.id)
            .bind("channel_id", entity.channel.id)
            .bind("user_id", entity.user.id)
            .bind("permission", entity.permissions.name)
            .execute()
    }

    override fun deleteById(id: Int) {
        handle
            .createUpdate("DELETE FROM dbo.participants WHERE id = :id")
            .bind("id", id)
            .execute()
    }

    override fun clear() {
        handle.createUpdate("DELETE FROM dbo.participants").execute()
    }

    override fun createParticipant(
        channel: Channel,
        user: User,
        permission: Permissions
    ): Participant {
        val id =
            handle
                .createUpdate(
                    """
            INSERT INTO dbo.participants (user_id, channel_id, permission) 
            VALUES (:user_id, :channel_id, :permission)
            """,
                ).bind("user_id", user.id)
                .bind("channel_id", channel.id)
                .bind("permission", permission.name)
                .executeAndReturnGeneratedKeys()
                .mapTo(Int::class.java)
                .one()

        return Participant(id, channel, user, permission)
    }

    override fun removeParticipant(
        channel: Channel,
        user: User,
    ) {
        handle
            .createUpdate(
                "DELETE FROM dbo.participants WHERE user_id = :user_id AND channel_id = :channel_id"
            ).bind("user_id", user.id)
            .bind("channel_id", channel.id)
            .executeAndReturnGeneratedKeys()
            .mapTo(Int::class.java)
            .one()
    }

    override fun findByUsername(username: String, channel: Channel): Participant? =
        handle
            .createQuery(
                """
            SELECT 
                p.*,
                u.*,
                c.*,
                o.username AS owner_username,
                o.password_validation AS owner_password
            FROM dbo.participants p
            JOIN dbo.users u ON p.user_id = u.id
            JOIN dbo.channels c ON p.channel_id = c.id
            INNER JOIN dbo.users o ON c.owner_id = o.id
            WHERE u.username = :username AND channel_id = :channel_id
            """,
            ).bind("username", username)
            .bind("channel_id", channel.id)
            .map { rs, _ -> mapRowToParticipant(rs) }
            .findOne()
            .orElse(null)

    override fun findAllByChannel(channel: Channel): List<Participant> =
        handle
            .createQuery(
                """
            SELECT 
                p.*,
                u.*,
                c.*,
                o.username AS owner_username,
                o.password_validation AS owner_password
            FROM dbo.participants p
            JOIN dbo.users u ON p.user_id = u.id
            JOIN dbo.channels c ON p.channel_id = c.id
            INNER JOIN dbo.users o ON c.owner_id = o.id
            WHERE c.id = :channel_id
            """,
            ).bind("channel_id", channel.id)
            .map { rs, _ -> mapRowToParticipant(rs) }
            .list()

    private fun mapRowToParticipant(rs: ResultSet): Participant {
        // Create the pt.isel.User
        val user = User(
            rs.getInt("user_id"),
            rs.getString("username"),
            PasswordValidationInfo(rs.getString("password_validation"))
        )
        val owner = User(
            rs.getInt("owner_id"),
            rs.getString("owner_username"),
            PasswordValidationInfo(rs.getString("owner_password")),

            )

        // Create the pt.isel.Channel
        val channel =
            Channel(
                id = rs.getInt("channel_id"),
                name = rs.getString("name"),
                owner = owner,
                type = ChannelKind.valueOf(rs.getString("type")),
            )

        // Return the pt.isel.Participant
        return Participant(
            rs.getInt("id"),
            channel,
            user,
            Permissions.valueOf(rs.getString("permission"))
        )
    }
}