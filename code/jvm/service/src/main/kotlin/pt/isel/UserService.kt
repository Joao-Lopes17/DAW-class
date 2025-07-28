package pt.isel

import jakarta.inject.Named
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.slf4j.Logger
import org.slf4j.LoggerFactory

data class TokenExternalInfo(
    val tokenValue: String,
    val tokenExpiration: Instant,
)

sealed class UserError {
    data object AlreadyUsedUsername : UserError()

    data object InsecurePassword : UserError()
}

sealed class TokenCreationError {
    data object UserOrPasswordAreInvalid : TokenCreationError()
}

@Named
class UserService(
    private val trxManager: TransactionManager,
    val usersDomain: UsersDomain,
    private val clock: Clock,
) {
    private val logger: Logger = LoggerFactory.getLogger(UserService::class.java)

    fun createToken(
        username: String,
        password: String,
    ): Either<TokenCreationError, TokenExternalInfo> {
        if (username.isBlank() || password.isBlank()) {
            logger.warn("Attempt to create a token with an empty username or password")
            return failure(TokenCreationError.UserOrPasswordAreInvalid)
        }
        return trxManager.run {
            val user: User =
                repoUser.findUserByUsername(username)
                    ?: return@run failure(TokenCreationError.UserOrPasswordAreInvalid)
            if (!usersDomain.validatePassword(password, user.passwordValidation)) {
                logger.warn("Login attempt with invalid credentials: $username")
                return@run failure(TokenCreationError.UserOrPasswordAreInvalid)
            }
            val tokenValue = usersDomain.generateTokenValue()
            val now = clock.now()
            val newToken =
                Token(
                    usersDomain.createTokenValidationInformation(tokenValue),
                    user.id,
                    createdAt = now,
                    lastUsedAt = now,
                )
            repoUser.createToken(newToken, usersDomain.maxNumberOfTokensPerUser)
            logger.info("Token created for user: $username")
            success(
                TokenExternalInfo(
                    tokenValue,
                    usersDomain.getTokenExpiration(newToken),
                ),
            )
        }
    }

    fun createUser(
        username: String,
        password: String,
    ): Either<UserError, User> {
        if (username.isBlank() || password.isBlank()) {
            logger.warn("Attempt to create a user with an empty username or password")
            return failure(UserError.InsecurePassword) // Alternatively, create a specific error for invalid usernames.
        }
        if (!usersDomain.isSafePassword(password)) {
            logger.warn("Attempt to create a user with an insecure password: $username")
            return failure(UserError.InsecurePassword)
        }

        val passwordValidationInfo = usersDomain.createPasswordValidationInformation(password)

        return trxManager.run {
            if (repoUser.findUserByUsername(username) != null) {
                logger.warn("Attempt to create a user with an already used username: $username")
                return@run failure(UserError.AlreadyUsedUsername)
            }
            val user = repoUser.createUser(username, passwordValidationInfo)
            logger.info("User created successfully: $username")
            success(user)
        }
    }

    fun revokeToken(token: String): Boolean {
        logger.info("Revoking token: $token")
        val tokenValidationInfo = usersDomain.createTokenValidationInformation(token)
        return trxManager.run {
            repoUser.removeTokenByValidationInfo(tokenValidationInfo)
            logger.info("Token revoked successfully: $token")
            true
        }
    }

    fun getUserByToken(token: String): User? {
        if (!usersDomain.canBeToken(token)) {
            logger.warn("Invalid token format: $token")
            return null
        }
        return trxManager.run {
            val tokenValidationInfo = usersDomain.createTokenValidationInformation(token)
            val userAndToken = repoUser.getTokenByTokenValidationInfo(tokenValidationInfo)
            userAndToken?.let { (user, userToken) ->
                if (usersDomain.isTokenTimeValid(clock, userToken)) {
                    repoUser.updateTokenLastUsed(userToken, clock.now())
                    logger.info("Token is valid and last used time updated: $token")
                    user
                } else {
                    logger.warn("Token is invalid or expired: $token")
                    null
                }
            }
        }
    }

    fun removeUserById(userId: Int): Boolean {
        logger.info("Removing user with id: $userId")
        return trxManager.run {
            repoUser.removeUserById(userId) > 0
        }
    }

    fun getAllUsers(): List<User> = trxManager.run { repoUser.findAll() }

    fun getUserById(userId: Int): UserInfo? =
        trxManager.run {
            repoUser.findById(userId)?.let { user ->
                UserInfo(user.username, user.passwordValidation.validationInfo)
            }
        }
    
}
