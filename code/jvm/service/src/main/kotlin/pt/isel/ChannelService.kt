package pt.isel

import jakarta.inject.Named

sealed class ChannelError {
    data object ChannelNotFound : ChannelError()

    data object UserNotFound : ChannelError()

    data object UserNotOwner : ChannelError()

    data object UserIsAlreadyOnChannel : ChannelError()

    data object UserIsNotOnChannel: ChannelError()
}

@Named
class ChannelService(
    private val trxManager: TransactionManager,
    private val tokenEncoder: TokenEncoder, // Validate token and extract information
) {
    // Create new channel
    fun createChannel(
        authToken: String,
        name: String,
        ownername: String,
        type: ChannelKind,
    ): Either<ChannelError, Channel> =
        trxManager.run {
            // Validate token and extract information from it (userId)
            val validationInfo = tokenEncoder.createValidationInformation(authToken)
            if (!validationInfo.isValid()) {
                return@run failure(ChannelError.UserNotFound)
            }

            // Fetch with token information and validate user
            val authenticatedUser =
                repoUser.findUserByUsername(ownername)
                    ?: return@run failure(ChannelError.UserNotFound)


            val channel = repoChannel.createChannel(name, authenticatedUser, type)
            repoParticipant.createParticipant(channel, authenticatedUser, Permissions.READ_WRITE)
            success(channel)
        }
    fun deleteChannel(
        authToken: String,
        id: Int,
        ownername: String
    ): Either<ChannelError, Boolean> =
        trxManager.run {
            val validationInfo = tokenEncoder.createValidationInformation(authToken)
            if (!validationInfo.isValid()) {
                return@run failure(ChannelError.UserNotFound)
            }

            val authenticatedOwner =
                repoUser.findUserByUsername(ownername)
                    ?: return@run failure(ChannelError.UserNotFound)

            val channel =
                repoChannel.findById(id)
                    ?: return@run failure(ChannelError.ChannelNotFound)

            if(channel.owner.id != authenticatedOwner.id){
                return@run failure(ChannelError.UserNotOwner)
            }

            repoChannel.deleteById(id)
            success(true)
        }

    // Get all channels of a user
    fun getChannelsOfUser(userId: Int): Either<ChannelError,List<Channel>> =
        trxManager.run {
            // Fetch with token information and validate user
            val authenticatedUser =
                repoUser.findById(userId)
                    ?: return@run failure(ChannelError.UserNotFound)
            success(repoChannel.findAllByUser(authenticatedUser))
        }

    fun getChannelById(channelId: Int): ChannelInfo? =
        trxManager.run {
            val channel = repoChannel.findById(channelId) ?: return@run null
            ChannelInfo(channel.name, channel.type, channel.owner.username)
        }

    // Get all channels
    fun getAllChannels(): List<Channel> = trxManager.run { repoChannel.findAll() }

    // Get channel by id
    fun getChannelsByOwner(username: String) = trxManager.run {
        val authenticatedUser =
            repoUser.findUserByUsername(username)
                ?: return@run failure(ChannelError.UserNotFound)
        success(repoChannel.findAllByUser(repoUser.findUserByUsername(username)!!)) }

    // Get public channels
    fun getPublicChannels(): List<Channel> =
        trxManager.run {
            repoChannel.findAll().filter { it.type == ChannelKind.PUBLIC }
        }

    // Add user to channel
    fun addParticipantToChannel(
        username: String,
        channelId: Int,
        permissions: Permissions,
    ): Either<ChannelError, Channel> =
        trxManager.run {
            // Fetch channel
            val channel =
                repoChannel.findById(channelId)
                    ?: return@run failure(ChannelError.ChannelNotFound)

            // Fetch User
            val user =
                repoUser.findUserByUsername(username)
                    ?: return@run failure(ChannelError.UserNotFound)

            // Return Failure if the user is already a participant in that pt.isel.Channel
            if (repoParticipant.findByUsername(user.username, channel) != null) {
                return@run failure(ChannelError.UserIsAlreadyOnChannel)
            }

            // Otherwise, create a new pt.isel.Participant in that pt.isel.Channel for that user.
            repoParticipant.createParticipant(channel, user, permissions)
            success(channel)
        }

    // Remove user on channel
    fun removeParticipantToChannel(
        username: String,
        channelId: Int,
    ): Either<ChannelError, Boolean> =
        trxManager.run {
            // Fetch channel
            val channel =
                repoChannel.findById(channelId)
                    ?: return@run failure(ChannelError.ChannelNotFound)

            // Fetch User
            val user =
                repoUser.findUserByUsername(username)
                    ?: return@run failure(ChannelError.UserNotFound)

            // Return Failure if the user is not a participant in that Channel
            if (repoParticipant.findByUsername(user.username, channel) == null) {
                return@run failure(ChannelError.UserIsNotOnChannel)
            }

            // Otherwise, remove the Participant.
            repoParticipant.removeParticipant(channel, user)
            success(true)
        }

    // Get all participants of a channel
    fun getUsersOfChannel(channelId: Int): List<Participant> =
        trxManager.run {
            repoParticipant.findAllByChannel(repoChannel.findById(channelId)!!)
        }
}
