package pt.isel

import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component

@Component
class RequestTokenProcessor(
    private val usersService: UserService,
) {
    fun processAuthorizationHeaderValue(authorizationValue: String?): AuthenticatedUser? {
        if (authorizationValue == null) {
            return null
        }
        val parts = authorizationValue.trim().split(" ")
        if (parts.size != 2) {
            return null
        }
        if (parts[0].lowercase() != SCHEME) {
            return null
        }
        return usersService.getUserByToken(parts[1])?.let {
            AuthenticatedUser(
                it,
                parts[1],
            )
        }
    }

    fun processAuthorizationCookie(request: HttpServletRequest): AuthenticatedUser? {
        val cookies = request.cookies ?: return null
        val authToken = cookies.firstOrNull { it.name == AUTH_COOKIE_NAME }?.value ?: return null
        return usersService.getUserByToken(authToken)?.let {
            AuthenticatedUser(it, authToken)
        }
    }
    companion object {
        const val SCHEME = "bearer"
        const val AUTH_COOKIE_NAME = "authToken"
    }
}
