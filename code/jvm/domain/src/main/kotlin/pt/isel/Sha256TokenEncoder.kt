@file:Suppress("ktlint:standard:filename")

package pt.isel

import java.security.MessageDigest
import java.util.Base64

class Sha256TokenEncoder : TokenEncoder {
    override fun createValidationInformation(token: String): TokenValidationInfo {
        val validationInfo = hash(token)
        return TokenValidationInfo(validationInfo)
    }

    private fun hash(input: String): String {
        val messageDigest = MessageDigest.getInstance("SHA256")
        return Base64.getUrlEncoder().encodeToString(
            messageDigest.digest(
                Charsets.UTF_8.encode(input).array(),
            ),
        )
    }
}
