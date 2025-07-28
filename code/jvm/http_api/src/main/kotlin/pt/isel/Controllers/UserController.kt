package pt.isel.Controllers

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pt.isel.AuthenticatedUser
import pt.isel.Either
import pt.isel.Failure
import pt.isel.Success
import pt.isel.TokenCreationError
import pt.isel.User
import pt.isel.UserError
import pt.isel.UserInfo
import pt.isel.UserService
import pt.isel.model.Problem
import pt.isel.model.UserCreateTokenInputModel
import pt.isel.model.UserCreateTokenOutputModel
import pt.isel.model.UserHomeOutputModel
import pt.isel.model.UserInput

data class UsernameLoginModel(
    val username: String,
)


@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService,
) {
    @PostMapping("/signup")
    fun signUp(
        @RequestBody userInput: UserInput,
    ): ResponseEntity<*> {
        val result: Either<UserError, User> =
            userService.createUser(userInput.username, userInput.password)

        return when (result) {
            is Success -> {
                val tokenResult = userService.createToken(userInput.username, userInput.password)
                when (tokenResult) {
                    is Success -> {
                        val authCookie =
                            ResponseCookie
                                .from("authToken", tokenResult.value.tokenValue)
                                .httpOnly(true)
                                .secure(true)
                                .path("/")
                                .build()

                        ResponseEntity
                            .status(HttpStatus.CREATED)
                            .header("Location", "/api/users/${result.value.id}")
                            .header(HttpHeaders.SET_COOKIE, authCookie.toString())
                            .body(UsernameLoginModel(userInput.username))
                    }
                    is Failure ->
                        Problem.UserOrPasswordAreInvalid.response(HttpStatus.BAD_REQUEST)
                }
            }
            is Failure ->
                when (result.value) {
                    is UserError.AlreadyUsedUsername ->
                        Problem.UsernameAlreadyInUse.response(HttpStatus.CONFLICT)
                    UserError.InsecurePassword ->
                        Problem.InsecurePassword.response(HttpStatus.BAD_REQUEST)
                }
        }
    }

    @PostMapping("/logout")
    fun logout(user: AuthenticatedUser): ResponseEntity<Unit> {
        userService.revokeToken(user.token)

        val authCookie =
            ResponseCookie
                .from("authToken", "")
                .maxAge(0)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .build()

        return ResponseEntity
            .noContent()
            .header(HttpHeaders.SET_COOKIE, authCookie.toString())
            .build()
    }

    @GetMapping("/me")
    fun userHome(userAuthenticatedUser: AuthenticatedUser): ResponseEntity<UserHomeOutputModel> =
        ResponseEntity
            .status(HttpStatus.OK)
            .body(
                UserHomeOutputModel(
                    id = userAuthenticatedUser.user.id,
                    username = userAuthenticatedUser.user.username,
                ),
            )

    @PostMapping("/login")
    fun login(
        @RequestBody input: UserCreateTokenInputModel,
    ): ResponseEntity<*> {
        val res = userService.createToken(input.username, input.password)
        return when (res) {
            is Success -> {
                val authCookie =
                    ResponseCookie
                        .from("authToken", res.value.tokenValue)
                        .httpOnly(true)
                        .secure(true)
                        .path("/")
                        .build()

                ResponseEntity
                    .status(HttpStatus.OK)
                    .header(HttpHeaders.SET_COOKIE, authCookie.toString())
                    .body(UsernameLoginModel(input.username))

            }

            is Failure ->
                when (res.value) {
                    TokenCreationError.UserOrPasswordAreInvalid ->
                        Problem.UserOrPasswordAreInvalid.response(HttpStatus.BAD_REQUEST)
                }
        }
    }

    @DeleteMapping
    fun removeUserById(authenticatedUser: AuthenticatedUser): ResponseEntity<Void> {
        val userId = authenticatedUser.user.id
        return if (userService.removeUserById(userId)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/{userId}")
    fun getUserById(
        @PathVariable userId: Int,
    ): ResponseEntity<UserInfo> {
        val user = userService.getUserById(userId)
        return if (user != null) {
            ResponseEntity.ok(user)
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        }
    }

    @GetMapping
    fun getAllUsers():ResponseEntity<List<User>>{
        val users = userService.getAllUsers()
        return ResponseEntity.ok(users)
    }

}
