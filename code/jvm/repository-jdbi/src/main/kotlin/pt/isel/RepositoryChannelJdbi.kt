package pt.isel

import org.jdbi.v3.core.Handle
import java.sql.ResultSet

class RepositoryChannelJdbi(
    private val handle: Handle,
) : RepositoryChannel {
    override fun findById(id: Int): Channel? =
        handle
            .createQuery(
                """
            SELECT c.*, u.* FROM dbo.channels c
            JOIN dbo.users u ON c.owner_id = u.id
            WHERE c.id = :id
            """,
            ).bind("id", id)
            .map { rs, _ -> mapRowToChannel(rs) }
            .findOne()
            .orElse(null)

    override fun findAll(): List<Channel> =
        handle
            .createQuery(
                """
            SELECT c.*, u.* FROM dbo.channels c
            JOIN dbo.users u ON c.owner_id = u.id
            """,
            ).map { rs, _ -> mapRowToChannel(rs) }
            .list()

    override fun save(entity: Channel) {
        handle
            .createUpdate(
                """
            UPDATE dbo.channels 
            SET name = :name, owner_id = :owner_id, type = :type
            WHERE id = :id
            """,
            ).bind("id", entity.id)
            .bind("name", entity.name)
            .bind("owner_id", entity.owner.id)
            .bind("type", entity.type.name)
            .execute()
    }

    override fun deleteById(id: Int) {
        handle
            .createUpdate("DELETE FROM dbo.channels WHERE id = :id")
            .bind("id", id)
            .execute()
    }

    override fun clear() {
        handle.createUpdate("DELETE FROM dbo.channels").execute()
    }

    override fun createChannel(
        name: String,
        owner: User,
        kind: ChannelKind,
    ): Channel {
        val id =
            handle
                .createUpdate(
                    """
            INSERT INTO dbo.channels (name, owner_id, type) 
            VALUES (:name, :owner_id, :type)
            """,
                ).bind("name", name)
                .bind("owner_id", owner.id)
                .bind("type", kind.name)
                .executeAndReturnGeneratedKeys()
                .mapTo(Int::class.java)
                .one()

        return Channel(id, name, owner, kind)
    }

    override fun findAllByUser(user: User): List<Channel> =
        handle
            .createQuery(
                """
SELECT c.*, u.*
FROM dbo.channels c
JOIN dbo.participants p ON c.id = p.channel_id
JOIN dbo.users u ON p.user_id = u.id
WHERE u.id = :id;
            """,
            ).bind("id", user.id)
            .map { rs, _ -> mapRowToChannel(rs) }
            .list()

    private fun mapRowToChannel(rs: ResultSet): Channel {
        val owner =
            User(
                rs.getInt("owner_id"),
                rs.getString("username"),
                PasswordValidationInfo(rs.getString("password_validation")),
            )
        return Channel(
            id = rs.getInt("id"),
            name = rs.getString("name"),
            owner = owner,
            type = ChannelKind.valueOf(rs.getString("type")),
        )
    }
}
