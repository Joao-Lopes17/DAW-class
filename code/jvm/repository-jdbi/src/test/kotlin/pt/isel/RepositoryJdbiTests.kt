package pt.isel

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.postgresql.ds.PGSimpleDataSource
import java.sql.Timestamp
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.assertEquals

class RepositoryJdbiTests {
    companion object {
        private fun runWithHandle(block: (Handle) -> Unit) = jdbi.useTransaction<Exception>(block)

        private fun newTokenValidationData() = "token-${abs(Random.nextLong())}"

        private val jdbi =
            Jdbi
                .create(
                    PGSimpleDataSource().apply {
                        setURL(Environment.getDbUrl())
                    },
                ).configureWithAppRequirements()
    }

    @BeforeEach
    fun clean() {
        runWithHandle { handle: Handle ->
            RepositoryParticipantJdbi(handle).clear()
            RepositoryMessageJdbi(handle).clear()
            RepositoryChannelJdbi(handle).clear()
            RepositoryUserJdbi(handle).clear()
        }
    }

    // RepositoryUserJdbi Tests
    @Test
    fun `create user and find it`() =
        runWithHandle { handle ->
            val user1 = RepositoryUserJdbi(handle).createUser("Paulo", PasswordValidationInfo(newTokenValidationData()))
            val user2 = RepositoryUserJdbi(handle).createUser("Inês", PasswordValidationInfo(newTokenValidationData()))
            val users = RepositoryUserJdbi(handle).findAll()
            val user1db = RepositoryUserJdbi(handle).findById(user1.id)
            val user1ById = RepositoryUserJdbi(handle).findUserByUsername("Paulo")
            assertEquals(2, users.size)
            assertEquals(user1, user1db)
            assertEquals(user1, user1ById)
        }

    @Test
    fun `getAllUsers`() =
        runWithHandle { handle ->
            val user1 = RepositoryUserJdbi(handle).createUser("Paulo", PasswordValidationInfo(newTokenValidationData()))
            val user2 = RepositoryUserJdbi(handle).createUser("Inês", PasswordValidationInfo(newTokenValidationData()))
            val users = RepositoryUserJdbi(handle).findAll()
            assertEquals(2, users.size)
        }

    @Test
    fun `delete user from db`() =
        runWithHandle { handle ->
            val user1 = RepositoryUserJdbi(handle).createUser("Paulo", PasswordValidationInfo(newTokenValidationData()))
            RepositoryUserJdbi(handle).deleteById(user1.id)
            val users = RepositoryUserJdbi(handle).findAll()
            assertTrue(users.isEmpty())
        }

    @Test
    fun `update user`() =
        runWithHandle { handle ->
            val user = RepositoryUserJdbi(handle).createUser("Paulo", PasswordValidationInfo(newTokenValidationData()))
            val updatedUser = user.copy(username = "PauloUpdated")
            RepositoryUserJdbi(handle).save(updatedUser)
            val userdb = RepositoryUserJdbi(handle).findById(user.id)
            assertEquals(updatedUser.id, userdb?.id)
            assertEquals(updatedUser.username, userdb?.username)
        }

    @Test
    fun `clear user table`() =
        runWithHandle { handle ->
            RepositoryUserJdbi(handle).createUser("Paulo", PasswordValidationInfo(newTokenValidationData()))
            RepositoryUserJdbi(handle).createUser("Inês", PasswordValidationInfo(newTokenValidationData()))
            RepositoryUserJdbi(handle).clear()
            val users = RepositoryUserJdbi(handle).findAll()
            assertEquals(0, users.size)
        }

    // RepositoryChannelJdbi Tests
    @Test
    fun `create channel and find it`() =
        runWithHandle { handle ->
            val user = RepositoryUserJdbi(handle).createUser("Paulo", PasswordValidationInfo(newTokenValidationData()))
            val channel = RepositoryChannelJdbi(handle).createChannel("Channel1", user, ChannelKind.PUBLIC)
            val channels = RepositoryChannelJdbi(handle).findAll()
            assertEquals(1, channels.size)
            assertEquals(channel, channels[0])
        }

    @Test
    fun `delete channel`() =
        runWithHandle { handle ->
            val owner = RepositoryUserJdbi(handle).createUser("Paulo", PasswordValidationInfo(newTokenValidationData()))
            val channel = RepositoryChannelJdbi(handle).createChannel("Channel1", owner, ChannelKind.PUBLIC)
            RepositoryChannelJdbi(handle).deleteById(channel.id)
            val channels = RepositoryChannelJdbi(handle).findAll()
            assertEquals(0, channels.size)
        }

    @Test
    fun `clear channel table`() =
        runWithHandle { handle ->
            val owner = RepositoryUserJdbi(handle).createUser("Paulo", PasswordValidationInfo(newTokenValidationData()))
            RepositoryChannelJdbi(handle).createChannel("Channel1", owner, ChannelKind.PUBLIC)
            RepositoryChannelJdbi(handle).createChannel("Channel2", owner, ChannelKind.PUBLIC)
            RepositoryChannelJdbi(handle).clear()
            val channels = RepositoryChannelJdbi(handle).findAll()
            assertEquals(0, channels.size)
        }

    @Test
    fun `update channel`() =
        runWithHandle { handle ->
            val owner = RepositoryUserJdbi(handle).createUser("Paulo", PasswordValidationInfo(newTokenValidationData()))
            val channel = RepositoryChannelJdbi(handle).createChannel("Channel1", owner, ChannelKind.PUBLIC)
            val updatedChannel = channel.copy(name = "UpdatedChannel")
            RepositoryChannelJdbi(handle).save(updatedChannel)
            val channeldb = RepositoryChannelJdbi(handle).findById(channel.id)
            assertEquals(updatedChannel, channeldb)
        }

    // RepositoryParticipantJdbi Tests
    @Test
    fun `create participant and find it`() =
        runWithHandle { handle ->
            val user = RepositoryUserJdbi(handle).createUser("Paulo", PasswordValidationInfo(newTokenValidationData()))
            val channel = RepositoryChannelJdbi(handle).createChannel("Channel1", user, ChannelKind.PUBLIC)
            val participant = RepositoryParticipantJdbi(handle).createParticipant(channel, user, Permissions.READ_ONLY)
            val participants = RepositoryParticipantJdbi(handle).findAll()
            val participantDb = RepositoryParticipantJdbi(handle).findById(participant.id)
            val partByUsername = RepositoryParticipantJdbi(handle).findByUsername(user.username, channel)
            assertEquals(1, participants.size)
            assertEquals(participant, participantDb)
            assertEquals(participant, partByUsername)
        }

    @Test
    fun `find all participants in channel`() =
        runWithHandle { handle ->
            val user1 = RepositoryUserJdbi(handle).createUser("Paulo", PasswordValidationInfo(newTokenValidationData()))
            val channel = RepositoryChannelJdbi(handle).createChannel("Channel1", user1, ChannelKind.PUBLIC)
            val participant1 = RepositoryParticipantJdbi(handle).createParticipant(channel, user1, Permissions.READ_ONLY)
            val user2 = RepositoryUserJdbi(handle).createUser("Inês", PasswordValidationInfo(newTokenValidationData()))
            val participant2 = RepositoryParticipantJdbi(handle).createParticipant(channel, user2, Permissions.READ_ONLY)
            val participants = RepositoryParticipantJdbi(handle).findAllByChannel(channel)
            assertEquals(2, participants.size)
            assertEquals(participant2, participants[1])
        }

    @Test
    fun `delete participant`() =
        runWithHandle { handle ->
            val user1 = RepositoryUserJdbi(handle).createUser("Paulo", PasswordValidationInfo(newTokenValidationData()))
            val channel = RepositoryChannelJdbi(handle).createChannel("Channel1", user1, ChannelKind.PUBLIC)
            val participant1 = RepositoryParticipantJdbi(handle).createParticipant(channel, user1, Permissions.READ_ONLY)
            val user2 = RepositoryUserJdbi(handle).createUser("Inês", PasswordValidationInfo(newTokenValidationData()))
            val participant2 = RepositoryParticipantJdbi(handle).createParticipant(channel, user2, Permissions.READ_ONLY)
            RepositoryParticipantJdbi(handle).deleteById(participant2.id)
            val participants = RepositoryParticipantJdbi(handle).findAll()
            assertEquals(1, participants.size)
        }

    @Test
    fun `clear participants table`() =
        runWithHandle { handle ->
            val user1 = RepositoryUserJdbi(handle).createUser("Paulo", PasswordValidationInfo(newTokenValidationData()))
            val channel = RepositoryChannelJdbi(handle).createChannel("Channel1", user1, ChannelKind.PUBLIC)
            val participant1 = RepositoryParticipantJdbi(handle).createParticipant(channel, user1, Permissions.READ_ONLY)
            val user2 = RepositoryUserJdbi(handle).createUser("Inês", PasswordValidationInfo(newTokenValidationData()))
            val participant2 = RepositoryParticipantJdbi(handle).createParticipant(channel, user2, Permissions.READ_ONLY)
            RepositoryParticipantJdbi(handle).clear()
            val participants = RepositoryParticipantJdbi(handle).findAll()
            assertEquals(0, participants.size)
        }

    @Test
    fun `update participant`() =
        runWithHandle { handle ->
            val user = RepositoryUserJdbi(handle).createUser("Paulo", PasswordValidationInfo(newTokenValidationData()))
            val channel = RepositoryChannelJdbi(handle).createChannel("Channel1", user, ChannelKind.PUBLIC)
            val participant = RepositoryParticipantJdbi(handle).createParticipant(channel, user, Permissions.READ_ONLY)
            val updatedParticipant = participant.copy(permissions = Permissions.READ_WRITE)
            RepositoryParticipantJdbi(handle).save(updatedParticipant)
            val participantDb = RepositoryParticipantJdbi(handle).findById(participant.id)
            assertEquals(updatedParticipant, participantDb)
        }

    // RepositoryMessageJdbi Tests
    @Test
    fun `create message and find it`() =
        runWithHandle { handle ->
            val user1 = RepositoryUserJdbi(handle).createUser("Paulo", PasswordValidationInfo(newTokenValidationData()))
            val user2 = RepositoryUserJdbi(handle).createUser("Inês", PasswordValidationInfo(newTokenValidationData()))
            val channel = RepositoryChannelJdbi(handle).createChannel("Channel1", user1, ChannelKind.PUBLIC)
            val time = System.currentTimeMillis()
            val t = Timestamp(time)
            val message1 = RepositoryMessageJdbi(handle).createMessage(user1, channel, "Message1")
            val message2 = RepositoryMessageJdbi(handle).createMessage(user2, channel, "Message2")
            val messages = RepositoryMessageJdbi(handle).findAll()
            val messageDB = RepositoryMessageJdbi(handle).findById(message1.id)
            val messageByChannel = RepositoryMessageJdbi(handle).findByChannel(channel)
            assertEquals(2, messages.size)
            assertEquals(message1, messageDB)
            assertEquals(2, messageByChannel.size)
        }

    @Test
    fun `find User Messages In Channel`() =
        runWithHandle { handle ->
            val user1 = RepositoryUserJdbi(handle).createUser("Paulo", PasswordValidationInfo(newTokenValidationData()))
            val user2 = RepositoryUserJdbi(handle).createUser("Inês", PasswordValidationInfo(newTokenValidationData()))
            val channel = RepositoryChannelJdbi(handle).createChannel("Channel1", user1, ChannelKind.PUBLIC)
            val time = System.currentTimeMillis()
            val t = Timestamp(time)
            val message1 = RepositoryMessageJdbi(handle).createMessage(user1, channel, "Message1")
            val message2 = RepositoryMessageJdbi(handle).createMessage(user2, channel, "Message2")
            val user2Messages = RepositoryMessageJdbi(handle).findUserMessagesInChannel(user2, channel)
            assertEquals(1, user2Messages.size)
            assertEquals(message2, user2Messages[0])
        }

    @Test
    fun `update message`() =
        runWithHandle { handle ->
            val user1 = RepositoryUserJdbi(handle).createUser("Paulo", PasswordValidationInfo(newTokenValidationData()))
            val channel = RepositoryChannelJdbi(handle).createChannel("Channel1", user1, ChannelKind.PUBLIC)
            val time = System.currentTimeMillis()
            val t = Timestamp(time)
            val message = RepositoryMessageJdbi(handle).createMessage(user1, channel, "Message1")
            val updatedMessage = message.copy(content = "UpdatedMessage")
            RepositoryMessageJdbi(handle).save(updatedMessage)
            val messages = RepositoryMessageJdbi(handle).findAll()
            val messageDb = RepositoryMessageJdbi(handle).findById(message.id)
            assertEquals(1, messages.size)
            assertEquals(updatedMessage, messageDb)
        }

    @Test
    fun `get latest messages`() =
        runWithHandle { handle ->
            val user = RepositoryUserJdbi(handle).createUser("Paulo", PasswordValidationInfo(newTokenValidationData()))
            val channel = RepositoryChannelJdbi(handle).createChannel("Channel1", user, ChannelKind.PUBLIC)
            repeat(15) {
                RepositoryMessageJdbi(handle).createMessage(user, channel, "Message$it")
            }
            val latestMessages = RepositoryMessageJdbi(handle).getLatestMessages(channel)
            assertEquals(10, latestMessages.size)
        }

    @Test
    fun `delete message from channel`() =
        runWithHandle { handle ->
            val user1 = RepositoryUserJdbi(handle).createUser("Paulo", PasswordValidationInfo(newTokenValidationData()))
            val user2 = RepositoryUserJdbi(handle).createUser("Inês", PasswordValidationInfo(newTokenValidationData()))
            val channel = RepositoryChannelJdbi(handle).createChannel("Channel1", user1, ChannelKind.PUBLIC)
            val time = System.currentTimeMillis()
            val t = Timestamp(time)
            val message1 = RepositoryMessageJdbi(handle).createMessage(user1, channel, "Message1")
            val message2 = RepositoryMessageJdbi(handle).createMessage(user2, channel, "Message2")
            RepositoryMessageJdbi(handle).deleteById(message1.id)
            val messages = RepositoryMessageJdbi(handle).findAll()
            assertEquals(1, messages.size)
            assertEquals(message2, messages[0])
        }

    @Test
    fun `clear messages table`() =
        runWithHandle { handle ->
            val user1 = RepositoryUserJdbi(handle).createUser("Paulo", PasswordValidationInfo(newTokenValidationData()))
            val user2 = RepositoryUserJdbi(handle).createUser("Inês", PasswordValidationInfo(newTokenValidationData()))
            val channel = RepositoryChannelJdbi(handle).createChannel("Channel1", user1, ChannelKind.PUBLIC)
            val time = System.currentTimeMillis()
            val t = Timestamp(time)
            RepositoryMessageJdbi(handle).createMessage(user1, channel, "Message1")
            RepositoryMessageJdbi(handle).createMessage(user2, channel, "Message2")
            RepositoryMessageJdbi(handle).clear()
            val messages = RepositoryMessageJdbi(handle).findAll()
            assertEquals(0, messages.size)
        }

    // RepositoryInvitationJdbi Tests

    @Test
    fun `create invitation and find it`() =
        runWithHandle { handle ->
            val user1 = RepositoryUserJdbi(handle).createUser("Paulo", PasswordValidationInfo(newTokenValidationData()))
            val user2 = RepositoryUserJdbi(handle).createUser("Inês", PasswordValidationInfo(newTokenValidationData()))
            val channel = RepositoryChannelJdbi(handle).createChannel("channel",user1, ChannelKind.PUBLIC)
            val invitationId =
                RepositoryInvitationJdbi(handle).createInvitation(
                    Invitation(0, "code", user1, user2, channel.id, false, Permissions.READ_ONLY),
                )
            val invitations = RepositoryInvitationJdbi(handle).getAllInvitations()
            val invitationDb = RepositoryInvitationJdbi(handle).getInvitationById(invitationId)
            assertEquals(1, invitations.size)
            assertEquals(invitationId, invitationDb?.id)
        }

    @Test
    fun `get all invitations`() =
        runWithHandle { handle ->
            val user1 = RepositoryUserJdbi(handle).createUser("Paulo", PasswordValidationInfo(newTokenValidationData()))
            val user2 = RepositoryUserJdbi(handle).createUser("Inês", PasswordValidationInfo(newTokenValidationData()))
            val channel = RepositoryChannelJdbi(handle).createChannel("channel",user1, ChannelKind.PUBLIC)
            RepositoryInvitationJdbi(handle).createInvitation(
                Invitation(0, "code1", user1, user2, channel.id, false, Permissions.READ_ONLY),
            )
            RepositoryInvitationJdbi(handle).createInvitation(
                Invitation(0, "code2", user1, user2, channel.id, false, Permissions.READ_ONLY),
            )
            val invitations = RepositoryInvitationJdbi(handle).getAllInvitations()
            assertEquals(2, invitations.size)
        }

    @Test
    fun `get invitation by id`() =
        runWithHandle { handle ->
            val user1 = RepositoryUserJdbi(handle).createUser("Paulo", PasswordValidationInfo(newTokenValidationData()))
            val user2 = RepositoryUserJdbi(handle).createUser("Inês", PasswordValidationInfo(newTokenValidationData()))
            val channel = RepositoryChannelJdbi(handle).createChannel("channel",user1, ChannelKind.PUBLIC)
            val invitationId =
                RepositoryInvitationJdbi(handle).createInvitation(
                    Invitation(0, "code", user1, user2, channel.id, false, Permissions.READ_ONLY),
                )
            val invitation = RepositoryInvitationJdbi(handle).getInvitationById(invitationId)
            assertEquals(invitationId, invitation?.id)
        }



    @Test
    fun `delete invitation`() =
        runWithHandle { handle ->
            val user1 = RepositoryUserJdbi(handle).createUser("Paulo", PasswordValidationInfo(newTokenValidationData()))
            val user2 = RepositoryUserJdbi(handle).createUser("Inês", PasswordValidationInfo(newTokenValidationData()))
            val channel = RepositoryChannelJdbi(handle).createChannel("channel",user1, ChannelKind.PUBLIC)
            val invitationId =
                RepositoryInvitationJdbi(handle).createInvitation(
                    Invitation(0, "code1", user1, user2, channel.id, false, Permissions.READ_ONLY),
                )
            RepositoryInvitationJdbi(handle).deleteById(invitationId)
            val invitations = RepositoryInvitationJdbi(handle).getAllInvitations()
            assertEquals(0, invitations.size)
        }
}
