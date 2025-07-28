package pt.isel

import jakarta.inject.Named
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.springframework.security.crypto.password.PasswordEncoder
import java.security.SecureRandom
import java.util.Base64.getUrlDecoder
import java.util.Base64.getUrlEncoder

@Named
class UsersDomain(
    private val passwordEncoder: PasswordEncoder,
    private val tokenEncoder: TokenEncoder,
    private val config: UsersDomainConfig,
) {
    fun generateTokenValue(): String =
        ByteArray(config.tokenSizeInBytes).let { byteArray ->
            SecureRandom.getInstanceStrong().nextBytes(byteArray)
            getUrlEncoder().encodeToString(byteArray)
        }

    fun canBeToken(token: String): Boolean =
        try {
            getUrlDecoder().decode(token).size == config.tokenSizeInBytes
        } catch (ex: IllegalArgumentException) {
            false
        }

    fun validatePassword(
        password: String,
        validationInfo: PasswordValidationInfo,
    ) = passwordEncoder.matches(
        password,
        validationInfo.validationInfo,
    )

    fun createPasswordValidationInformation(password: String) =
        PasswordValidationInfo(
            validationInfo = passwordEncoder.encode(password),
        )

    fun isSafePassword(password: String): Boolean {
        val minLength = 8
        val maxLength = 12
        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }

        return password.length in minLength..maxLength
                && hasUppercase
                && hasLowercase
                && hasDigit
                && hasSpecialChar
    }



    fun getTokenExpiration(token: Token): Instant {
        val absoluteExpiration = token.createdAt.plus(config.tokenTtl)
        val rollingExpiration = token.lastUsedAt.plus(config.tokenRollingTtl)
        return if (absoluteExpiration < rollingExpiration) {
            absoluteExpiration
        } else {
            rollingExpiration
        }
    }

    fun createTokenValidationInformation(token: String) = tokenEncoder.createValidationInformation(token)

    fun isTokenTimeValid(
        clock: Clock,
        token: Token,
    ): Boolean {
        val now = clock.now()
        return token.createdAt <= now &&
            (now - token.createdAt) <= config.tokenTtl &&
            (now - token.lastUsedAt) <= config.tokenRollingTtl
    }


    val maxNumberOfTokensPerUser = config.maxTokensPerUser
}
