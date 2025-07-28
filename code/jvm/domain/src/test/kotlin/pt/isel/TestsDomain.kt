package pt.isel

import org.junit.jupiter.api.Assertions.assertTrue
import java.sql.Timestamp
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

private fun newTokenValidationData() = "token-${abs(Random.nextLong())}"

class TestsDomain {
    private val passwordUser1 = PasswordValidationInfo(newTokenValidationData())
    private val passwordUser2 = PasswordValidationInfo(newTokenValidationData())

    private val user1 = User(1, "John", passwordUser1)
    private val user2 = User(2, "Mary", passwordUser2)

    private val channelPublic = Channel(1, "pt.isel.Channel 1", user1, ChannelKind.PUBLIC)
    private val channelPrivate = Channel(2, "pt.isel.Channel 2", user1, ChannelKind.PRIVATE)

    private val message1 = Message(1, user1, channelPublic, "Hello", Timestamp(System.currentTimeMillis()))

    @Test
    fun `test user creation`() {
        assertEquals(1, user1.id)
        assertEquals("John", user1.username)
        assertEquals(passwordUser1, user1.passwordValidation)
    }

    @Test
    fun `test channel creation`() {
        assertEquals(1, channelPublic.id)
        assertEquals("pt.isel.Channel 1", channelPublic.name)
        assertEquals(user1, channelPublic.owner)
        assertEquals(ChannelKind.PUBLIC, channelPublic.type)
    }

    @Test
    fun `test message creation`() {
        assertEquals(1, message1.id)
        assertEquals(user1, message1.user)
        assertEquals(channelPublic, message1.channel)
        assertEquals("Hello", message1.content)
    }

    @Test
    fun `test user create channel`() {
        val channel = Channel(1, "pt.isel.Channel 1", user1, ChannelKind.PUBLIC)
        assertEquals(user1, channel.owner)
    }

    @Test
    fun `test user send message in channel`() {
        val message = Message(1, user1, channelPublic, "Hello", Timestamp(System.currentTimeMillis()))
        val message2 = Message(2, user2, channelPrivate, "Hello", Timestamp(System.currentTimeMillis()))

        assertEquals(user1, message.user)
        assertEquals(channelPublic, message.channel)

        assertEquals(user2, message2.user)
        assertEquals(channelPrivate, message2.channel)
    }

    @Test
    fun `test change channel type`() {
        channelPublic.type = ChannelKind.PRIVATE
        assertEquals(ChannelKind.PRIVATE, channelPublic.type)

        channelPrivate.type = ChannelKind.PUBLIC
        assertEquals(ChannelKind.PUBLIC, channelPrivate.type)
    }

    @Test
    fun `test add user to channel`() {
        val channel = Channel(3, "pt.isel.Channel 3", user1, ChannelKind.PUBLIC)
        channel.addUser(user2)
        assertTrue(channel.users.contains(user2))
    }

    @Test
    fun `test remove user from channel`() {
        val channel = Channel(4, "pt.isel.Channel 4", user1, ChannelKind.PUBLIC)
        channel.addUser(user2)
        assertTrue(channel.users.contains(user2))
        channel.removeUser(user2)
        assertFalse(channel.users.contains(user2))
    }

    @Test
    fun `test send message`() {
        val message = Message(3, user2, channelPublic, "Hi everyone", Timestamp(System.currentTimeMillis()))
        assertEquals("Hi everyone", message.content)
        assertEquals(user2, message.user)
        assertEquals(channelPublic, message.channel)
    }

    @Test
    fun `test channel creation with users`() {
        val channel = Channel(5, "pt.isel.Channel 5", user1, ChannelKind.PUBLIC)
        channel.addUser(user1)
        channel.addUser(user2)
        assertTrue(channel.users.contains(user1))
        assertTrue(channel.users.contains(user2))
    }

    @Test
    fun `test token format`() {
        val token = newTokenValidationData()
        assertTrue(token.startsWith("token-"), "pt.isel.Token should start with 'token-'")
    }

    @Test
    fun `test unique tokens`() {
        val token1 = newTokenValidationData()
        val token2 = newTokenValidationData()
        assertNotEquals(token1, token2, "Tokens should be unique")
    }

    @Test
    fun `test token is not empty`() {
        val token = newTokenValidationData()
        assertTrue(token.isNotBlank(), "pt.isel.Token should not be empty or blank")
    }

    @Test
    fun `test create invitation`() {
        val invitation = Invitation(1, "code", user1, user2, 1, false, Permissions.READ_ONLY)
        assertEquals(1, invitation.id)
        assertEquals("code", invitation.code)
        assertEquals(user1, invitation.inviter)
        assertEquals(user2, invitation.invitee)
        assertEquals(1, invitation.channelid)
        assertEquals(false, invitation.used)
        assertEquals(Permissions.READ_ONLY, invitation.type)
    }

    @Test
    fun `test create invitation without invitee`() {
        val invitation = Invitation(1, "code", user1, null, 2, false, Permissions.READ_ONLY)
        assertEquals(1, invitation.id)
        assertEquals("code", invitation.code)
        assertEquals(user1, invitation.inviter)
        assertEquals(null, invitation.invitee)
        assertEquals(2, invitation.channelid)
        assertEquals(false, invitation.used)
        assertEquals(Permissions.READ_ONLY, invitation.type)
    }
}
