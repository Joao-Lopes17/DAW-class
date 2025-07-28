package pt.isel

import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import pt.isel.mem.TransactionManagerInMem
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

class TestUserService {
    companion object {
        private val jdbi =
            Jdbi
                .create(
                    PGSimpleDataSource().apply {
                        setURL(Environment.getDbUrl())
                    },
                ).configureWithAppRequirements()

        @JvmStatic
        fun transactionManagers(): Stream<TransactionManager> =
            Stream.of(
                TransactionManagerInMem().also { cleanup(it) },
                TransactionManagerJdbi(jdbi).also { cleanup(it) },
            )

        private fun cleanup(trxManager: TransactionManager) {
            trxManager.run {
                repoMessage.clear()
                repoParticipant.clear()
                repoChannel.clear()
                repoUser.clear()
            }
        }

        private fun createUserService(
            trxManager: TransactionManager,
            testClock: TestClock,
            tokenTtl: Duration = 30.days,
            tokenRollingTtl: Duration = 30.minutes,
            maxTokensPerUser: Int = 3,
        ) = UserService(
            trxManager,
            UsersDomain(
                BCryptPasswordEncoder(),
                Sha256TokenEncoder(),
                UsersDomainConfig(
                    tokenSizeInBytes = 256 / 8,
                    tokenTtl = tokenTtl,
                    tokenRollingTtl,
                    maxTokensPerUser = maxTokensPerUser,
                ),
            ),
            testClock,
        )
    }

    /**
     * Safe password -> 8..12 char, has upper and lowerCase letter, a digit and a special char.
     */

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `createUser should create and return a participant`(trxManager: TransactionManager) {
        val serviceUser = createUserService(trxManager, TestClock())

        val username = "Alice"
        val password = "Alice5678#"

        val result =
            serviceUser.createUser(
                username,
                password,
            )

        assertIs<Success<User>>(result)
        assertEquals(username, result.value.username)
        assertTrue { serviceUser.usersDomain.validatePassword(password, result.value.passwordValidation) }
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `createUser with invalid password should return InsecurePassword`(trxManager: TransactionManager) {
        val serviceUser = createUserService(trxManager, TestClock())

        // No upperCase letter
        val result: Either<UserError, User> = serviceUser.createUser("Alice", "alice5678#")

        assertIs<Failure<UserError>>(result)
        assertIs<UserError.InsecurePassword>(result.value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `createUser with already used username should return AlreadyUsedUsername error`(trxManager: TransactionManager) {
        val serviceUser = createUserService(trxManager, TestClock())

        serviceUser.createUser("Alice", "Alice5678#")

        val result: Either<UserError, User> =
            serviceUser.createUser(
                "Alice",
                "Mary1234#",
            )

        assertIs<Failure<UserError>>(result)
        assertIs<UserError.AlreadyUsedUsername>(result.value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `createToken should create a token`(trxManager: TransactionManager) {
        val username = "Alice"
        val password = "Alice5678#"

        val serviceUser = createUserService(trxManager, TestClock())
        val user =
            serviceUser
                .createUser(username, password)
                .let {
                    check(it is Success)
                    it.value
                }

        val result =
            serviceUser.createToken(
                user.username,
                password,
            )

        assertIs<Success<TokenExternalInfo>>(result)
        val tokenInfo = (result as Success).value
        assertEquals(tokenInfo.tokenValue.isNotBlank(), true)
        assertEquals(tokenInfo.tokenExpiration > TestClock().now(), true)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `createToken should return UserOrPasswordAreInvalid if user is invalid`(trxManager: TransactionManager) {
        val serviceUser = createUserService(trxManager, TestClock())

        val result =
            serviceUser.createToken(
                "invalidUser",
                "ValidPass1#",
            )

        assertTrue(result is Failure)
        assertEquals((result).value, TokenCreationError.UserOrPasswordAreInvalid)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `createToken should return UserOrPasswordAreInvalid if password is invalid`(trxManager: TransactionManager) {
        val serviceUser = createUserService(trxManager, TestClock())
        val user =
            serviceUser
                .createUser("Alice", "Alice5678#")
                .let {
                    check(it is Success)
                    it.value
                }

        val result =
            serviceUser
                .createToken(user.username, "invalidPassword")

        assertTrue(result is Failure)
        assertEquals(result.value, TokenCreationError.UserOrPasswordAreInvalid)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `revokeToken should revoke the token successfully`(trxManager: TransactionManager) {
        val serviceUser = createUserService(trxManager, TestClock())

        serviceUser.createUser("validUser", "Pa$\$w0rd1!#")

        val token =
            serviceUser.createToken(
                "validUser",
                "Pa$\$w0rd1!#",
            )

        assertIs<Success<TokenExternalInfo>>(token)
        val tokenValue = token.value.tokenValue

        val revokeResult = serviceUser.revokeToken(tokenValue)

        assertEquals(true, revokeResult)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `revokeToken should handle well non-existent token`(trxManager: TransactionManager) {
        val serviceUser = createUserService(trxManager, TestClock())
        val nonExistentToken = "nonExistentTokenValue"

        val revokeResult = serviceUser.revokeToken(nonExistentToken)

        assertEquals(true, revokeResult)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `getUserByToken should return the user when token is valid`(trxManager: TransactionManager) {
        val serviceUser = createUserService(trxManager, TestClock())
        val username = "validUser"
        val password = "ValidPass1#"

        serviceUser.createUser("validUser", password)

        val token =
            serviceUser.createToken(
                username,
                password,
            )

        assertIs<Success<TokenExternalInfo>>(token)
        val tokenValue = token.value.tokenValue

        val userByToken = serviceUser.getUserByToken(tokenValue)

        assertEquals(username, userByToken?.username)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `getUserByToken should return null when token is invalid`(trxManager: TransactionManager) {
        val serviceUser = createUserService(trxManager, TestClock())
        val invalidToken = "invalidTokenValue"

        val user = serviceUser.getUserByToken(invalidToken)

        assertEquals(null, user)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `getUserByToken should return null when token is expired`(trxManager: TransactionManager) {
        val testClock = TestClock()
        val userService = createUserService(trxManager, testClock)
        val username = "validUser"
        val password = "ValidPass1#"

        userService.createUser(username, password)

        val token =
            userService.createToken(
                username,
                password,
            )

        assertIs<Success<TokenExternalInfo>>(token)
        val tokenValue = token.value.tokenValue

        testClock.advance(7.days)

        val user = userService.getUserByToken(tokenValue)

        assertEquals(null, user)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `getAllUsers should return all users`(trxManager: TransactionManager) {
        val serviceUser = createUserService(trxManager, TestClock())
        val username1 = "Alice"
        val username2 = "Bob"
        val password = "Alice5678#"

        serviceUser.createUser(username1, password)
        serviceUser.createUser(username2, password)

        val users = serviceUser.getAllUsers()

        assertEquals(2, users.size)
        assertEquals(username1, users[0].username)
        assertEquals(username2, users[1].username)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `getAllUsers should return an empty list when there are no users`(trxManager: TransactionManager) {
        val serviceUser = createUserService(trxManager, TestClock())

        val users = serviceUser.getAllUsers()

        assertEquals(0, users.size)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `getUserById should return the user when it exists`(trxManager: TransactionManager) {
        val serviceUser = createUserService(trxManager, TestClock())
        val username = "Alice"
        val password = "Alice5678#"

        val user =
            serviceUser
                .createUser(username, password)
                .let {
                    check(it is Success)
                    it.value
                }

        val userById = serviceUser.getUserById(user.id)

        assertEquals(username, userById?.username)
    }
}
