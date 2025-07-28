package pt.isel.mem

import jakarta.inject.Named
import kotlinx.datetime.Instant
import pt.isel.PasswordValidationInfo
import pt.isel.RepositoryUser
import pt.isel.Token
import pt.isel.TokenValidationInfo
import pt.isel.User
import kotlin.math.abs
import kotlin.random.Random

fun newTokenValidationData() = "token-${abs(Random.nextLong())}"

/**
 * Naif in memory repository non thread-safe and basic sequential id.
 * Useful for unit tests purpose.
 */
@Named
class RepositoryUserInMem : RepositoryUser {
    private val users = mutableListOf<User>(User(1, "Roberto", PasswordValidationInfo(newTokenValidationData())))
    private val tokens = mutableListOf<Token>()

    override fun createUser(
        username: String,
        passwordValidation: PasswordValidationInfo,
    ): User {
        val nextUserId = (users.maxOfOrNull { it.id } ?: 0) + 1
        return User(nextUserId, username, passwordValidation)
            .also { users.add(it) }
    }

    override fun findUserByUsername(username: String): User? = users.firstOrNull { it.username == username }

    override fun getTokenByTokenValidationInfo(tokenValidationInfo: TokenValidationInfo): Pair<User, Token>? =
        tokens.firstOrNull { it.tokenValidationInfo == tokenValidationInfo }?.let {
            val user = findById(it.userId)
            requireNotNull(user)
            user to it
        }

    override fun getAllUsers(): List<User> = users.toList()

    override fun createToken(
        token: Token,
        maxTokens: Int,
    ) {
        val nrOfTokens = tokens.count { it.userId == token.userId }

        // Remove the oldest token if we have achieved the maximum number of tokens
        if (nrOfTokens >= maxTokens) {
            tokens
                .filter { it.userId == token.userId }
                .minByOrNull { it.lastUsedAt }!!
                .also { tk -> tokens.removeIf { it.tokenValidationInfo == tk.tokenValidationInfo } }
        }
        tokens.add(token)
    }

    override fun updateTokenLastUsed(
        token: Token,
        now: Instant,
    ) {
        tokens.removeIf { it.tokenValidationInfo == token.tokenValidationInfo }
        tokens.add(token)
    }

    override fun removeUserById(userId: Int): Int {
        val count = users.count { it.id == userId }
        users.removeIf { it.id == userId }
        return count
    }

    override fun removeTokenByValidationInfo(tokenValidationInfo: TokenValidationInfo): Int {
        val count = tokens.count { it.tokenValidationInfo == tokenValidationInfo }
        tokens.removeAll { it.tokenValidationInfo == tokenValidationInfo }
        return count
    }

    override fun findById(id: Int): User? = users.firstOrNull { it.id == id }

    override fun findAll(): List<User> = users.toList()

    override fun save(entity: User) {
        users.removeIf { it.id == entity.id }
        users.add(entity)
    }

    override fun deleteById(id: Int) {
        users.removeIf { it.id == id }
    }

    override fun clear() {
        users.clear()
    }
}
