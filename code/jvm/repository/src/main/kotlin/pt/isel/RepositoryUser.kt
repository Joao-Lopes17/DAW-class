package pt.isel

import kotlinx.datetime.Instant

interface RepositoryUser : Repository<User> {
    fun createUser(
        username: String,
        passwordValidation: PasswordValidationInfo,
    ): User

    fun findUserByUsername(username: String): User?

    fun getTokenByTokenValidationInfo(tokenValidationInfo: TokenValidationInfo): Pair<User, Token>?

    fun getAllUsers(): List<User>

    fun createToken(
        token: Token,
        maxTokens: Int,
    )

    fun updateTokenLastUsed(
        token: Token,
        now: Instant,
    )

    fun removeUserById(userId: Int): Int

    fun removeTokenByValidationInfo(tokenValidationInfo: TokenValidationInfo): Int
}
