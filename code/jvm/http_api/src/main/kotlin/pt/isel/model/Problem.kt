package pt.isel.model

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.net.URI

private const val MEDIA_TYPE = "application/problem+json"
private const val PROBLEM_URI_PATH =
    "https://github.com/isel-leic-daw/2024-daw-leic53d-g11-53d/tree/1.1/docs"

sealed class Problem(
    typeUri: URI,
) {
    @Suppress("unused")
    val type = typeUri.toString()
    val title = typeUri.toString().split("/").last()

    fun response(status: HttpStatus): ResponseEntity<Any> =
        ResponseEntity
            .status(status)
            .header("Content-Type", MEDIA_TYPE)
            .body(this)

    data object UsernameAlreadyInUse : Problem(URI("$PROBLEM_URI_PATH/username-already-in-use"))

    data object ParticipantNotFound : Problem(URI("$PROBLEM_URI_PATH/participant-not-found"))

    data object UserOrPasswordAreInvalid : Problem(URI("$PROBLEM_URI_PATH/user-or-password-are-invalid"))

    data object ChannelNotFound : Problem(URI("$PROBLEM_URI_PATH/channel-not-found"))

    data object InsecurePassword : Problem(URI("$PROBLEM_URI_PATH/insecure-password"))

    data object UserIsAlreadyParticipantInChannel :
        Problem(URI("$PROBLEM_URI_PATH/user-is-already-participant-in-channel"))

    data object  InvalidRequestContent : Problem(URI("$PROBLEM_URI_PATH/invalid-request-content"))
}
