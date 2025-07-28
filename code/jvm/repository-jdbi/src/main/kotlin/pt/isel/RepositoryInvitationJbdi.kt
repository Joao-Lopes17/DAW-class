package pt.isel

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet

class RepositoryInvitationJdbi(
    private val handle: Handle,
) : RepositoryInvitation {
    override fun createInvitation(invitation: Invitation): Int =
        handle
            .createUpdate(
                """
                INSERT INTO dbo.invitations (code, inviter_id, invitee_id, channel_id, used, permission)
                VALUES (:code, :inviter_id, :invitee_id, :channel_id, :used, :permission)
                """,
            ).bind("code", invitation.code)
            .bind("inviter_id", invitation.inviter.id)
            .bind("invitee_id", invitation.invitee?.id)
            .bind("channel_id", invitation.channelid)
            .bind("used", invitation.used)
            .bind("permission", invitation.type.name)
            .executeAndReturnGeneratedKeys()
            .mapTo(Int::class.java)
            .one()

    override fun getAllInvitations(): List<Invitation> =
        handle
            .createQuery("""
                   SELECT 
        i.id AS invitation_id,
        i.code AS invitation_code,
        i.inviter_id,
        u1.username AS inviter_username,
        u1.password_validation AS inviter_password_validation,
        i.invitee_id,
        u2.username AS invitee_username,
        u2.password_validation AS invitee_password_validation,
        i.channel_id,
        i.used,
        i.permission AS invitation_permission
    FROM dbo.invitations i
    LEFT JOIN dbo.users u1 ON i.inviter_id = u1.id
    LEFT JOIN dbo.users u2 ON i.invitee_id = u2.id

            """.trimIndent())
            .map(InvitationMapper())
            .list()

    override fun getInvitationById(invitationId: Int): Invitation? =
        handle
            .createQuery(
                """
    SELECT 
        i.id AS invitation_id,
        i.code AS invitation_code,
        i.inviter_id,
        u1.username AS inviter_username,
        u1.password_validation AS inviter_password_validation,
        i.invitee_id,
        u2.username AS invitee_username,
        u2.password_validation AS invitee_password_validation,
        i.channel_id,
        i.used,
        i.permission AS invitation_permission
    FROM dbo.invitations i
    LEFT JOIN dbo.users u1 ON i.inviter_id = u1.id
    LEFT JOIN dbo.users u2 ON i.invitee_id = u2.id
    WHERE i.id = :id
    """.trimIndent())
            .bind("id", invitationId)
            .map(InvitationMapper())
            .findOne()
            .orElse(null)

    override fun getInvitationByCode(code: String): Invitation? =
        handle
            .createQuery( """
    SELECT 
        i.id AS invitation_id,
        i.code AS invitation_code,
        i.inviter_id,
        u1.username AS inviter_username,
        u1.password_validation AS inviter_password_validation,
        i.invitee_id,
        u2.username AS invitee_username,
        u2.password_validation AS invitee_password_validation,
        i.channel_id,
        i.used,
        i.permission AS invitation_permission
    FROM dbo.invitations i
    LEFT JOIN dbo.users u1 ON i.inviter_id = u1.id
    LEFT JOIN dbo.users u2 ON i.invitee_id = u2.id
    WHERE i.code = :code
    """.trimIndent())
            .bind("code", code)
            .map(InvitationMapper())
            .findOne()
            .orElse(null)

    override fun markInvitationAsUsed(id: Int): Boolean =
        handle
            .createUpdate(
                """
                UPDATE dbo.invitations 
                SET used = :used 
                WHERE id = :id
                """,
            ).bind("used", true)
            .bind("id", id)
            .execute() > 0

    override fun acceptInvitation(
        userId: Int,
        id: Int,
    ): Boolean =
        handle
            .createUpdate(
                """
                UPDATE dbo.invitations 
                SET invitee_id = :user_id, used = :used 
                WHERE id = :id
                
                """,
            ).bind("user_id", userId)
            .bind("used", true)
            .bind("id", id)
            .execute() > 0

    override fun rejectInvitation(
        userId: Int,
        id: Int
    ): Boolean =
        handle
            .createUpdate(
                """
                UPDATE dbo.invitations 
                SET invitee_id = :user_id, used = :used 
                WHERE id = :id
                """,
            ).bind("user_id", userId)
            .bind("used", true)
            .bind("id", id)
            .execute() > 0

    override fun findById(id: Int): Invitation? =
        handle
            .createQuery("SELECT * FROM dbo.invitations WHERE id = :id")
            .bind("id", id)
            .map(InvitationMapper())
            .findOne()
            .orElse(null)

    override fun findAll(): List<Invitation> =
        handle
            .createQuery("SELECT * FROM dbo.invitations")
            .map(InvitationMapper())
            .list()

    override fun save(entity: Invitation) {
        if (entity.id == 0) {
            createInvitation(entity)
        } else {
            handle
                .createUpdate(
                    """
                    UPDATE dbo.invitations 
                    SET code = :code, inviter_id = :inviter_id, invitee_id = :invitee_id, 
                        used = :used, permission = :permission
                    WHERE id = :id
                    """,
                ).bind("id", entity.id)
                .bind("code", entity.code)
                .bind("inviter_id", entity.inviter.id)
                .bind("invitee_id", entity.invitee?.id)
                .bind("used", entity.used)
                .bind("permission", entity.type.name)
                .execute()
        }
    }

    override fun deleteById(id: Int) {
        handle
            .createUpdate("DELETE FROM dbo.invitations WHERE id = :id")
            .bind("id", id)
            .execute()
    }

    override fun clear() {
        handle
            .createUpdate("DELETE FROM dbo.invitations")
            .execute()
    }

    override fun getAllInvitationsByUser(userId: Int): List<Invitation> =
        handle
            .createQuery("""
    SELECT 
        i.id AS invitation_id,
        i.code AS invitation_code,
        i.inviter_id,
        u1.username AS inviter_username,
        u1.password_validation AS inviter_password_validation,
        i.invitee_id,
        u2.username AS invitee_username,
        u2.password_validation AS invitee_password_validation,
        i.channel_id,
        i.used,
        i.permission AS invitation_permission
    FROM dbo.invitations i
    LEFT JOIN dbo.users u1 ON i.inviter_id = u1.id
    LEFT JOIN dbo.users u2 ON i.invitee_id = u2.id
    WHERE invitee_id = :user_id
    """.trimIndent())
            .bind("user_id", userId)
            .map(InvitationMapper())
            .list()

    private class InvitationMapper : RowMapper<Invitation> {
        override fun map(
            rs: ResultSet,
            ctx: StatementContext,
        ): Invitation {
            val inviter = User(
                id = rs.getInt("inviter_id"),
                username = rs.getString("inviter_username"),
                passwordValidation = PasswordValidationInfo(rs.getString("inviter_password_validation")),
            )

            val invitee = rs.getInt("invitee_id").let {
                if (rs.wasNull()) {
                    null
                } else {
                    User(
                        id = it,
                        username = rs.getString("invitee_username"),
                        passwordValidation = PasswordValidationInfo(rs.getString("invitee_password_validation")),
                    )
                }
            }

            return Invitation(
                id = rs.getInt("invitation_id"),
                code = rs.getString("invitation_code"),
                inviter = inviter,
                invitee = invitee,
                channelid = rs.getInt("channel_id"),
                used = rs.getBoolean("used"),
                type = Permissions.valueOf(rs.getString("invitation_permission")),
            )
        }
    }
}
