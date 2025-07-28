@file:Suppress("ktlint:standard:no-wildcard-imports")

package pt.isel

import kotlinx.datetime.*
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import org.slf4j.LoggerFactory
import java.sql.ResultSet

class RepositoryUserJdbi(
    private val handle: Handle,
) : RepositoryUser {
    override fun findById(id: Int): User? =
        handle
            .createQuery("SELECT * FROM dbo.users WHERE id = :id")
            .bind("id", id)
            .map(UserMapper())
            .findOne()
            .orElse(null)

    override fun findAll(): List<User> =
        handle
            .createQuery("SELECT * FROM dbo.users")
            .map(UserMapper())
            .list()

    override fun save(entity: User) {
        handle
            .createUpdate(
                """
            UPDATE dbo.users 
            SET username = :username
            WHERE id = :id
            """,
            ).bind("username", entity.username)
            .bind("id", entity.id)
            .execute()
    }

    override fun deleteById(id: Int) {
        handle
            .createUpdate("DELETE FROM dbo.users WHERE id = :id")
            .bind("id", id)
            .execute()
    }

    override fun clear() {
        handle.createUpdate("DELETE FROM dbo.Tokens").execute()
        handle.createUpdate("DELETE FROM dbo.users").execute()
    }

    override fun createUser(
        username: String,
        passwordValidation: PasswordValidationInfo,
    ): User {
        val id =
            handle
                .createUpdate(
                    """
            INSERT INTO dbo.users (username, password_validation) 
            VALUES (:username, :password_validation)
            RETURNING id
            """,
                ).bind("username", username)
                .bind("password_validation", passwordValidation.validationInfo)
                .executeAndReturnGeneratedKeys()
                .mapTo(Int::class.java)
                .one()

        return User(id, username, passwordValidation)
    }

    override fun findUserByUsername(username: String): User? =
        handle
            .createQuery("SELECT * FROM dbo.users WHERE username = :username")
            .bind("username", username)
            .map(UserMapper())
            .findOne()
            .orElse(null)

    // Mapper for pt.isel.User
    private class UserMapper : RowMapper<User> {
        override fun map(
            rs: ResultSet,
            ctx: StatementContext,
        ): User =
            User(
                id = rs.getInt("id"),
                username = rs.getString("username"),
                passwordValidation = PasswordValidationInfo(rs.getString("password_validation")),
            )
    }

    override fun getTokenByTokenValidationInfo(tokenValidationInfo: TokenValidationInfo): Pair<User, Token>? =
        handle
            .createQuery(
                """
                select id, username, password_validation, token_validation, created_at, last_used_at
                from dbo.Users as users 
                inner join dbo.Tokens as tokens 
                on users.id = tokens.user_id
                where token_validation = :validation_information
            """,
            ).bind("validation_information", tokenValidationInfo.validationInfo)
            .mapTo<UserAndTokenModel>()
            .singleOrNull()
            ?.userAndToken

    override fun getAllUsers(): List<User> =
        handle
            .createQuery("SELECT * FROM dbo.Users")
            .mapTo<User>()
            .list()

    override fun createToken(
        token: Token,
        maxTokens: Int,
    ) {
        // Delete the oldest token when achieved the maximum number of tokens
        val deletions =
            handle
                .createUpdate(
                    """
                    delete from dbo.Tokens 
                    where user_id = :user_id 
                        and token_validation in (
                            select token_validation from dbo.Tokens where user_id = :user_id 
                                order by last_used_at desc offset :offset
                        )
                    """.trimIndent(),
                ).bind("user_id", token.userId)
                .bind("offset", maxTokens - 1)
                .execute()

        logger.info("{} tokens deleted when creating new token", deletions)

        handle
            .createUpdate(
                """
                insert into dbo.Tokens(user_id, token_validation, created_at, last_used_at) 
                values (:user_id, :token_validation, :created_at, :last_used_at)
                """.trimIndent(),
            ).bind("user_id", token.userId)
            .bind("token_validation", token.tokenValidationInfo.validationInfo)
            .bind("created_at", token.createdAt.epochSeconds)
            .bind("last_used_at", token.lastUsedAt.epochSeconds)
            .execute()
    }

    override fun updateTokenLastUsed(
        token: Token,
        now: Instant,
    ) {
        handle
            .createUpdate(
                """
                update dbo.Tokens
                set last_used_at = :last_used_at
                where token_validation = :validation_information
                """.trimIndent(),
            ).bind("last_used_at", now.epochSeconds)
            .bind("validation_information", token.tokenValidationInfo.validationInfo)
            .execute()
    }

    override fun removeUserById(userId: Int): Int {
        val deletions =
            handle
                .createUpdate(
                    """
                    delete from dbo.Tokens
                    where user_id = :user_id
                    """.trimIndent(),
                ).bind("user_id", userId)
                .execute()

        logger.info("{} tokens deleted when removing user", deletions)

        return handle
            .createUpdate(
                """
                delete from dbo.Users
                where id = :user_id
                """.trimIndent(),
            ).bind("user_id", userId)
            .execute()
    }

    override fun removeTokenByValidationInfo(tokenValidationInfo: TokenValidationInfo): Int =
        handle
            .createUpdate(
                """
                delete from dbo.Tokens
                where token_validation = :validation_information
            """,
            ).bind("validation_information", tokenValidationInfo.validationInfo)
            .execute()

    private data class UserAndTokenModel(
        val id: Int,
        val username: String,
        val passwordValidation: PasswordValidationInfo,
        val tokenValidation: TokenValidationInfo,
        val createdAt: Long,
        val lastUsedAt: Long,
    ) {
        val userAndToken: Pair<User, Token>
            get() =
                Pair(
                    User(id, username, passwordValidation),
                    Token(
                        tokenValidation,
                        id,
                        Instant.fromEpochSeconds(createdAt),
                        Instant.fromEpochSeconds(lastUsedAt),
                    ),
                )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RepositoryUserJdbi::class.java)
    }
}
