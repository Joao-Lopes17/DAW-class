@file:Suppress("ktlint:standard:no-wildcard-imports")

package pt.isel

import kotlinx.datetime.Instant
import org.junit.jupiter.api.BeforeEach
import pt.isel.mem.*
import java.sql.Timestamp
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

private fun newTokenValidationData() = "token-${abs(Random.nextLong())}"

class TestRepositoryInMem {
    private lateinit var repoUsers: RepositoryUserInMem
    private lateinit var user: User
    private lateinit var user2: User
    private lateinit var repoChannels: RepositoryChannelInMem
    private lateinit var channel: Channel
    private lateinit var repoParticipant: RepositoryParticipantInMem
    private lateinit var participant: Participant
    private lateinit var repoMessages: RepositoryMessageInMem

    @BeforeEach
    fun setUp() {
        repoUsers =
            RepositoryUserInMem().apply {
                createUser("Alice", PasswordValidationInfo(newTokenValidationData()))
            }
        user = repoUsers.createUser("Alex", PasswordValidationInfo(newTokenValidationData()))
        user2 = repoUsers.createUser("Joana", PasswordValidationInfo(newTokenValidationData()))

        repoChannels =
            RepositoryChannelInMem().apply {
                createChannel("channel", user, ChannelKind.PUBLIC)
            }
        channel = repoChannels.createChannel("channel2", user, ChannelKind.PRIVATE)

        repoParticipant =
            RepositoryParticipantInMem().apply {
                createParticipant(channel, user, Permissions.READ_ONLY)
            }
        participant = repoParticipant.createParticipant(channel, user2, Permissions.READ_WRITE)

        repoMessages = RepositoryMessageInMem()
    }

    // RepositoryUserInMem Tests

    @Test
    fun `find user`() {
        val users = repoUsers.findAll()
        val userDbById = repoUsers.findById(user.id)
        val userDbByUsername = repoUsers.findUserByUsername(user.username)
        assertEquals(4, users.size)
        assertEquals(user, userDbById)
        assertEquals(user, userDbByUsername)
    }

    @Test
    fun `get all users`() {
        val users = repoUsers.getAllUsers()
        assertEquals(4, users.size)
    }

    @Test
    fun `delete user`() {
        repoUsers.deleteById(user.id)
        val users = repoUsers.findAll()
        assertEquals(3, users.size)
    }

    @Test
    fun `update user`() {
        val updatedUser = User(user.id, "Alexandre", PasswordValidationInfo(newTokenValidationData()))
        repoUsers.save(updatedUser)
        val userDb = repoUsers.findById(user.id)
        assertEquals(updatedUser, userDb)
    }

    @Test
    fun `clear users`() {
        repoUsers.clear()
        val users = repoUsers.findAll()
        assertEquals(0, users.size)
    }

    @Test
    fun `create user`() {
        println("Initial users: ${repoUsers.findAll().size}")
        val newUser = repoUsers.createUser("Júlio", PasswordValidationInfo(newTokenValidationData()))
        val users = repoUsers.findAll()
        println("Users after creation: ${users.size}")
        assertEquals(5, users.size)
        assertEquals(newUser, users.last())
    }

    @Test
    fun `create token`() {
        val token =
            Token(
                userId = user.id,
                tokenValidationInfo = TokenValidationInfo(newTokenValidationData()),
                createdAt = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
                lastUsedAt = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
            )
        repoUsers.createToken(token, maxTokens = 5)
        val tokens = repoUsers.getTokenByTokenValidationInfo(token.tokenValidationInfo)
        assertEquals(user, tokens?.first)
        assertEquals(token, tokens?.second)
    }

    @Test
    fun `update Token Last Used`() {
        val token =
            Token(
                userId = user.id,
                tokenValidationInfo = TokenValidationInfo(newTokenValidationData()),
                createdAt = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
                lastUsedAt = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
            )
        repoUsers.createToken(token, maxTokens = 5)
        val newLastUsedAt = Instant.fromEpochMilliseconds(System.currentTimeMillis() + 1000)
        val updatedToken = token.copy(lastUsedAt = newLastUsedAt)
        repoUsers.updateTokenLastUsed(updatedToken, newLastUsedAt)
        val fetchedToken = repoUsers.getTokenByTokenValidationInfo(updatedToken.tokenValidationInfo)
        assertEquals(newLastUsedAt, fetchedToken?.second?.lastUsedAt)
    }

    @Test
    fun `remove Token By ValidationInfo`() {
        val token =
            Token(
                userId = user.id,
                tokenValidationInfo = TokenValidationInfo(newTokenValidationData()),
                createdAt = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
                lastUsedAt = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
            )
        repoUsers.createToken(token, maxTokens = 5)
        val removedCount = repoUsers.removeTokenByValidationInfo(token.tokenValidationInfo)
        assertEquals(1, removedCount)
        val fetchedToken = repoUsers.getTokenByTokenValidationInfo(token.tokenValidationInfo)
        assertEquals(null, fetchedToken)
    }

    // RepositoryChannelInMem Tests

    @Test
    fun `find channel`() {
        val channelDb = repoChannels.findById(channel.id)
        val channels = repoChannels.findAll()
        assertEquals(3, channels.size)
        assertEquals(channel, channelDb)
    }

    @Test
    fun `delete channel`() {
        repoChannels.deleteById(channel.id)
        val channels = repoChannels.findAll()
        assertEquals(2, channels.size)
    }

    @Test
    fun `update channel`() {
        val updatedChannel = Channel(channel.id, "updatedChannel", channel.owner, channel.type)
        repoChannels.save(updatedChannel)
        val channelDb = repoChannels.findById(channel.id)
        assertEquals(updatedChannel, channelDb)
    }

    @Test
    fun `clear channels`() {
        repoChannels.clear()
        val channels = repoChannels.findAll()
        assertEquals(0, channels.size)
    }

    @Test
    fun `create channel`() {
        val newChannel = repoChannels.createChannel("Meeting", user, ChannelKind.PUBLIC)
        val channels = repoChannels.findAll()
        assertEquals(4, channels.size)
        assertEquals(newChannel, channels[3])
    }

    // RepositoryParticipantInMem Tests

    @Test
    fun `find participant`() {
        val participants = repoParticipant.findAll()
        val participantDbId = repoParticipant.findById(participant.id)
        val participantDbUsername = repoParticipant.findByUsername(participant.user.username, participant.channel)
        assertEquals(3, participants.size)
        assertEquals(participant, participantDbId)
        assertEquals(participant, participantDbUsername)
    }

    @Test
    fun `find participants by channel`() {
        val channel1 = repoChannels.createChannel("channel1", user2, ChannelKind.PRIVATE)
        val user1 = repoUsers.createUser("Miguel", PasswordValidationInfo(newTokenValidationData()))
        val newParticipant = repoParticipant.createParticipant(channel1, user1, Permissions.READ_ONLY)
        val participants = repoParticipant.findAllByChannel(channel1)
        assertEquals(1, participants.size)
        assertEquals(newParticipant, participants[0])
    }

    @Test
    fun `delete participant`() {
        repoParticipant.deleteById(participant.id)
        val participants = repoParticipant.findAll()
        assertEquals(2, participants.size)
    }

    @Test
    fun `update participant`() {
        val updatedParticipant = Participant(participant.id, participant.channel, participant.user, Permissions.READ_ONLY)
        repoParticipant.save(updatedParticipant)
        val participantDb = repoParticipant.findById(participant.id)
        assertEquals(updatedParticipant, participantDb)
    }

    @Test
    fun `clear participant`() {
        repoParticipant.clear()
        val participants = repoParticipant.findAll()
        assertEquals(0, participants.size)
    }

    // RepositoryMessageInMem Tests

    @Test
    fun `find message`() {
        val msg1 = repoMessages.createMessage(channel.owner, channel, "Hi, welcome to the channel!")
        assertEquals(1, repoMessages.findAll().size)
        assertEquals(msg1, repoMessages.findById(msg1.id))
    }

    @Test
    fun `test replacing a message in a channel`() {
        val msg1 = repoMessages.createMessage(channel.owner, channel, "Hi, welcome to the channel!")
        val msg2 =
            repoMessages.createMessage(
                channel.owner,
                channel,
                "The meeting will start in 6 minutes",

            )
        assertEquals(setOf(msg1, msg2), repoMessages.getLatestMessages(channel).toSet())
        val newMessage =
            Message(msg2.id, msg2.user, msg2.channel, "The meeting will start in 5 minutes", Timestamp(System.currentTimeMillis()))
        repoMessages.save(newMessage)
        val messages = repoMessages.getLatestMessages(channel).toSet()
        assertEquals(2, messages.size)
        assertEquals(listOf(newMessage, msg1), repoMessages.getLatestMessages(channel))
    }

    @Test
    fun `delete message`() {
        val msg1 = repoMessages.createMessage(channel.owner, channel, "Hi, welcome to the channel!")
        val msg2 =
            repoMessages.createMessage(
                channel.owner,
                channel,
                "The meeting will start in 6 minutes",

            )
        repoMessages.deleteById(msg2.id)
        assertEquals(1, repoMessages.findAll().size)
    }

    @Test
    fun `clear messages`() {
        repoMessages.createMessage(channel.owner, channel, "Hi, welcome to the channel!")
        repoMessages.createMessage(channel.owner, channel, "The meeting will start in 6 minutes")
        repoMessages.clear()
        assertEquals(0, repoMessages.findAll().size)
    }

    @Test
    fun `create messages`() {
        val msg1 = repoMessages.createMessage(channel.owner, channel, "Hi, welcome to the channel!")
        val msg2 =
            repoMessages.createMessage(
                channel.owner,
                channel,
                "The meeting will start in 6 minutes",
            )
        assertEquals(2, repoMessages.findAll().size)
        assertEquals(msg1, repoMessages.findById(0))
        assertEquals(msg2, repoMessages.findById(1))
    }

    @Test
    fun `find user messages in channel`() {
        val msg1 = repoMessages.createMessage(channel.owner, channel, "Hi, welcome to the channel!")
        val msg2 =
            repoMessages.createMessage(
                channel.owner,
                channel,
                "The meeting will start in 6 minutes",
            )
        assertEquals(2, repoMessages.findAll().size)
        assertEquals(listOf(msg1, msg2), repoMessages.findUserMessagesInChannel(channel.owner, channel))
    }

    // RepositoryInvitationInMem Tests

    @Test
    fun `create invitation`() {
        val repoInvitation = RepositoryInvitationInMem()
        val invitation =
            repoInvitation.createInvitation(
                Invitation(
                    id = 0,
                    code = "code",
                    inviter = user,
                    invitee = user2,
                    channelid = channel.id,
                    used = false,
                    type = Permissions.READ_ONLY,
                ),
            )
        assertEquals(1, repoInvitation.getAllInvitations().size)
    }

    @Test
    fun `get all invitations`() {
        val repoInvitation = RepositoryInvitationInMem()
        repoInvitation.createInvitation(
            Invitation(
                id = 0,
                code = "code",
                inviter = user,
                invitee = user2,
                channelid = channel.id,
                used = false,
                type = Permissions.READ_ONLY,
            ),
        )
        assertEquals(1, repoInvitation.getAllInvitations().size)
    }

    @Test
    fun `delete invitation by Id`() {
        val repoInvitation = RepositoryInvitationInMem()
        val invitation =
            repoInvitation.createInvitation(
                Invitation(
                    id = 0,
                    code = "code",
                    inviter = user,
                    invitee = user2,
                    channelid = channel.id,
                    used = false,
                    type = Permissions.READ_ONLY,
                ),
            )
        repoInvitation.deleteById(invitation)
        assertEquals(0, repoInvitation.getAllInvitations().size)
    }

    @Test
    fun `get invitation by Id`() {
        val repoInvitation = RepositoryInvitationInMem()
        val invitationId =
            repoInvitation.createInvitation(
                Invitation(
                    id = 1,
                    code = "code",
                    inviter = user,
                    invitee = user2,
                    channelid = channel.id,
                    used = false,
                    type = Permissions.READ_ONLY,
                ),
            )
        val fetchedInvitation = repoInvitation.getInvitationById(invitationId)
        assertEquals(invitationId, fetchedInvitation?.id)
        assertEquals("code", fetchedInvitation?.code)
        assertEquals(user, fetchedInvitation?.inviter)
        assertEquals(user2, fetchedInvitation?.invitee)
        assertEquals(channel.id, fetchedInvitation?.channelid)
        assertEquals(false, fetchedInvitation?.used)
        assertEquals(Permissions.READ_ONLY, fetchedInvitation?.type)
    }

    @Test
    fun `get invitation by code`() {
        val repoInvitation = RepositoryInvitationInMem()
        val invitationId =
            repoInvitation.createInvitation(
                Invitation(
                    id = 1,
                    code = "code",
                    inviter = user,
                    invitee = user2,
                    channelid = channel.id,
                    used = false,
                    type = Permissions.READ_ONLY,
                ),
            )
        val fetchedInvitation = repoInvitation.getInvitationByCode("code")
        assertEquals(invitationId, fetchedInvitation?.id)
        assertEquals("code", fetchedInvitation?.code)
        assertEquals(user, fetchedInvitation?.inviter)
        assertEquals(user2, fetchedInvitation?.invitee)
        assertEquals(channel.id, fetchedInvitation?.channelid)
        assertEquals(false, fetchedInvitation?.used)
        assertEquals(Permissions.READ_ONLY, fetchedInvitation?.type)
    }

    @Test
    fun `accept invitation`() {
        val repoInvitation = RepositoryInvitationInMem()
        val invitationId =
            repoInvitation.createInvitation(
                Invitation(
                    id = 1,
                    code = "code",
                    inviter = user,
                    invitee = user2,
                    channelid = channel.id,
                    used = false,
                    type = Permissions.READ_ONLY,
                ),
            )
        val accepted = repoInvitation.acceptInvitation(user2.id,invitationId )
        assertEquals(true, accepted)
        val fetchedInvitation = repoInvitation.getInvitationById(invitationId)
        assertEquals(true, fetchedInvitation?.used)
    }

    @Test
    fun `reject invitation`() {
        val repoInvitation = RepositoryInvitationInMem()
        val invitationId =
            repoInvitation.createInvitation(
                Invitation(
                    id = 1,
                    code = "code",
                    inviter = user,
                    invitee = user2,
                    channelid = channel.id,
                    used = false,
                    type = Permissions.READ_ONLY,
                ),
            )
        val rejected = repoInvitation.rejectInvitation(user2.id,invitationId)
        assertEquals(true, rejected)
        val fetchedInvitation = repoInvitation.getInvitationById(invitationId)
        assertEquals(true, fetchedInvitation?.used)
    }

    @Test
    fun `mark invitation as used`() {
        val repoInvitation = RepositoryInvitationInMem()
        val invitationId =
            repoInvitation.createInvitation(
                Invitation(
                    id = 1,
                    code = "code",
                    inviter = user,
                    invitee = user2,
                    channelid = channel.id,
                    used = false,
                    type = Permissions.READ_ONLY,
                ),
            )
        val marked = repoInvitation.markInvitationAsUsed(invitationId)
        assertEquals(true, marked)
        val fetchedInvitation = repoInvitation.getInvitationById(invitationId)
        assertEquals(true, fetchedInvitation?.used)
    }

    @Test
    fun `update invitation status`() {
        val repoInvitation = RepositoryInvitationInMem()
        val invitationId =
            repoInvitation.createInvitation(
                Invitation(
                    id = 1,
                    code = "code",
                    inviter = user,
                    invitee = user2,
                    channelid = channel.id,
                    used = false,
                    type = Permissions.READ_ONLY,
                ),
            )
        val updated = repoInvitation.updateInvitationStatus(invitationId, true)
        assertEquals(true, updated)
        val fetchedInvitation = repoInvitation.getInvitationById(invitationId)
        assertEquals(true, fetchedInvitation?.used)
    }
}
