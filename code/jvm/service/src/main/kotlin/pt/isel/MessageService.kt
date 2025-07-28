
package pt.isel

import jakarta.inject.Named


sealed class MessageError {
    data object ChannelNotFound : MessageError()

    data object UserNotFound : MessageError()

    data object UserIsNotParticipant : MessageError()

    data object MessageNotFound : MessageError()

    data object UserDoesntHavePermission : MessageError()
}

@Named
class MessageService(
    private val trxManager: TransactionManager,
    private val tokenEncoder: TokenEncoder, // Validate token and extract information
) {

    // Create new message
    fun createMessage(
        authToken: String,
        username: String,
        channelId: Int,
        message: String,
    ): Either<MessageError, Message> =
        trxManager.run {
            // Validate token and extract information from it (userId)
            val validationInfo = tokenEncoder.createValidationInformation(authToken)
            if (!validationInfo.isValid()) {
                return@run failure(MessageError.UserNotFound)
            }

            // Fetch with token information and validate user
            val authenticatedUser =
                repoUser.findUserByUsername(username)
                    ?: return@run failure(MessageError.UserNotFound)

            val channel =
                repoChannel.findById(channelId)
                    ?: return@run failure(MessageError.ChannelNotFound)

            val participant = repoParticipant.findByUsername(authenticatedUser.username, channel)
            if (participant == null) {
                return@run failure(MessageError.UserIsNotParticipant)
            }
            if (participant.permissions == Permissions.READ_ONLY){
                return@run failure(MessageError.UserDoesntHavePermission)
            }
            val newMessage = repoMessage.createMessage(authenticatedUser, channel, message)
            //sendEventToAll(channel,UpdatedMessage.Message(newMessage.id.toLong(), newMessage))
            success(newMessage)
        }

    // Delete message
    fun deleteMessage(messageId: Int): Either<MessageError, Unit> =
        trxManager.run {
            val message =
                repoMessage.findById(messageId)
                    ?: return@run failure(MessageError.MessageNotFound)
            repoMessage.deleteMessage(message)
            success(Unit)
        }

    // Get messages by channel ID
    fun getMessagesByChannelId(channelId: Int): Either<MessageError, List<Message>> =
        trxManager.run {
            val channel =
                repoChannel.findById(channelId)
                    ?: return@run failure(MessageError.ChannelNotFound)
            success(repoMessage.findByChannel(channel))
        }
}
