package pt.isel

import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import pt.isel.Controllers.UserController
import pt.isel.mem.TransactionManagerInMem
import pt.isel.model.UserCreateTokenInputModel
import pt.isel.model.UserCreateTokenOutputModel
import pt.isel.model.UserHomeOutputModel
import pt.isel.model.UserInput
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

class UserControllerTest {
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
                    tokenRollingTtl = tokenRollingTtl,
                    maxTokensPerUser = maxTokensPerUser,
                ),
            ),
            testClock,
        )
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `can create an user successfully`(trxManager: TransactionManager) {
        val controllerUser = UserController(createUserService(trxManager, TestClock()))
        val name = "John Fox"
        val password = "Pa$\$w0rd1!"

        // when: creating an user
        // then: the response is a 201 with a proper Location header
        controllerUser.signUp(UserInput(name, password)).let { resp ->
            assertEquals(HttpStatus.CREATED, resp.statusCode)
            val location = resp.headers.getFirst(HttpHeaders.LOCATION)
            assertNotNull(location)
            assertTrue(location.startsWith("/api/users"))
            location.split("/").last().toInt()
        }
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `cannot create user with duplicate username`(trxManager: TransactionManager) {
        val controllerUser = UserController(createUserService(trxManager, TestClock()))
        val name = "John Fox"
        val password = "Pa$\$w0rd1!"

        // given: an existing user
        controllerUser.signUp(UserInput(name, password)).let { resp ->
            assertEquals(HttpStatus.CREATED, resp.statusCode)
        }

        // when: trying to create another user with the same username
        // then: the response is a 409 CONFLICT
        controllerUser.signUp(UserInput(name, "AnotherPassword123")).also { resp ->
            assertEquals(HttpStatus.CONFLICT, resp.statusCode)
        }
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `cannot create user with insecure password`(trxManager: TransactionManager) {
        val controllerUser = UserController(createUserService(trxManager, TestClock()))
        val name = "Jane Doe"
        val insecurePassword = "123"

        // when: trying to create a user with an insecure password
        // then: the response is a 400 BAD_REQUEST
        controllerUser.signUp(UserInput(name, insecurePassword)).also { resp ->
            assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode)
        }
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `can login successfully with correct credentials`(trxManager: TransactionManager) {
        val controllerUser = UserController(createUserService(trxManager, TestClock()))
        val name = "John Fox"
        val password = "Pa$\$w0rd1!"

        controllerUser.signUp(UserInput(name, password))

        // when: logging in with correct credentials
        // then: the response is a 200 with a token
        controllerUser.login(UserCreateTokenInputModel(name, password)).also { resp ->
            assertEquals(HttpStatus.OK, resp.statusCode)
            assertIs<UserCreateTokenOutputModel>(resp.body)
        }
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `login fails with incorrect password`(trxManager: TransactionManager) {
        val controllerUser = UserController(createUserService(trxManager, TestClock()))
        val name = "John Fox"
        val password = "Pa$\$w0rd1!"

        // given: an existing user
        controllerUser.signUp(UserInput(name, password))

        // when: logging in with incorrect password
        // then: the response is a 400 BAD_REQUEST
        controllerUser.login(UserCreateTokenInputModel(name, "WrongPassword")).also { resp ->
            assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode)
        }
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `login fails with non-existent user`(trxManager: TransactionManager) {
        val controllerUser = UserController(createUserService(trxManager, TestClock()))

        // when: logging in with a non-existent user
        // then: the response is a 400 BAD_REQUEST
        controllerUser.login(UserCreateTokenInputModel("NonExistentUser", "password")).also { resp ->
            assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode)
        }
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `can logout successfully`(trxManager: TransactionManager) {
        val controllerUser = UserController(createUserService(trxManager, TestClock()))
        val name = "John Fox"
        val password = "Pa$\$w0rd1!"

        // given: an existing user and a valid token
        controllerUser.signUp(UserInput(name, password))
        val token =
            controllerUser.login(UserCreateTokenInputModel(name, password)).let { resp ->
                assertIs<UserCreateTokenOutputModel>(resp.body)
                (resp.body as UserCreateTokenOutputModel).token
            }

        val user = User(1, name, PasswordValidationInfo(password)) // Simulating user creation

        // when: logging out with a valid token
        // then: the response is a 204 NO_CONTENT
        controllerUser.logout(AuthenticatedUser(user, token)).also { resp ->
            assertEquals(HttpStatus.NO_CONTENT, resp.statusCode)
        }
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `can sign up and login directly`(trxManager: TransactionManager) {
        val controllerUser = UserController(createUserService(trxManager, TestClock()))
        val name = "Jane Doe"
        val password = "Pa$\$w0rd1!"

        // when: signing up a new user
        // then: the response is a 201 with a token
        controllerUser.signUp(UserInput(name, password)).also { resp ->
            assertEquals(HttpStatus.CREATED, resp.statusCode)
            val token = (resp.body as UserCreateTokenOutputModel).token
            assertNotNull(token)
        }
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `user home is accessible after login`(trxManager: TransactionManager) {
        val controllerUser = UserController(createUserService(trxManager, TestClock()))
        val name = "John Fox"
        val password = "Pa$\$w0rd1!"

        // given: a created user
        val userId =
            controllerUser.signUp(UserInput(name, password)).let { resp ->
                assertEquals(HttpStatus.CREATED, resp.statusCode)
                val location = resp.headers.getFirst(HttpHeaders.LOCATION)
                location?.split("/")?.last()?.toInt() ?: throw IllegalStateException("User ID missing")
            }

        val token =
            controllerUser.login(UserCreateTokenInputModel(name, password)).let { resp ->
                assertEquals(HttpStatus.OK, resp.statusCode)
                (resp.body as UserCreateTokenOutputModel).token
            }

        val user = User(userId, name, PasswordValidationInfo(password))

        // when: accessing user home
        // then: the response is a 200 with user details
        controllerUser.userHome(AuthenticatedUser(user, token)).also { resp ->
            assertEquals(HttpStatus.OK, resp.statusCode)
            assertEquals(UserHomeOutputModel(userId, name), resp.body)
        }
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `remove user by id`(trxManager: TransactionManager) {
        val controllerUser = UserController(createUserService(trxManager, TestClock()))
        val name = "John Fox"
        val password = "Pa$\$w0rd1!"

        // given: a created user
        val userId =
            controllerUser.signUp(UserInput(name, password)).let { resp ->
                assertEquals(HttpStatus.CREATED, resp.statusCode)
                val location = resp.headers.getFirst(HttpHeaders.LOCATION)
                location?.split("/")?.last()?.toInt() ?: throw IllegalStateException("User ID missing")
            }

        val token =
            controllerUser.login(UserCreateTokenInputModel(name, password)).let { resp ->
                assertEquals(HttpStatus.OK, resp.statusCode)
                (resp.body as UserCreateTokenOutputModel).token
            }

        val user = User(userId, name, PasswordValidationInfo(password))

        // when: removing the user
        // then: the response is a 204 NO_CONTENT
        controllerUser.removeUserById(AuthenticatedUser(user, token)).also { resp ->
            assertEquals(HttpStatus.NO_CONTENT, resp.statusCode)
        }
    }
}
