package pt.isel

import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import pt.isel.mem.TransactionManagerInMem
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

class TestChannelService {
    companion object {
        private val jdbi =
            Jdbi
                .create(
                    PGSimpleDataSource().apply {
                        setURL(Environment.getDbUrl())
                    },
                ).configureWithAppRequirements()

        @JvmStatic
        fun transactionManagers(): Stream<TransactionManager> =
            Stream.of(
                TransactionManagerInMem().also { cleanup(it) },
                TransactionManagerJdbi(jdbi).also { cleanup(it) },
            )

        private fun cleanup(trxManager: TransactionManager) {
            trxManager.run {
                repoMessage.clear()
                repoParticipant.clear()
                repoChannel.clear()
                repoUser.clear()
            }
        }

        private fun createUserService(
            trxManager: TransactionManager,
            testClock: TestClock,
            tokenTtl: Duration = 30.days,
            tokenRollingTtl: Duration = 30.minutes,
            maxTokensPerUser: Int = 3,
        ) = UserService(
            trxManager,
            UsersDomain(
                BCryptPasswordEncoder(),
                Sha256TokenEncoder(),
                UsersDomainConfig(
                    tokenSizeInBytes = 256 / 8,
                    tokenTtl = tokenTtl,
                    tokenRollingTtl,
                    maxTokensPerUser = maxTokensPerUser,
                ),
            ),
            testClock,
        )

        private fun createChannelService(trxManager: TransactionManager): ChannelService {
            val tokenEncoder = Sha256TokenEncoder() // Assuming this is the correct encoder
            return ChannelService(trxManager, tokenEncoder)
        }
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `addParticipantToChannel should add participant to a channel`(trxManager: TransactionManager) {
        val serviceChannel = createChannelService(trxManager)
        val serviceUser = createUserService(trxManager, TestClock())

        val owner =
            serviceUser
                .createUser("John", "John1234#")
                .let {
                    check(it is Success)
                    it.value
                }

        val authToken = serviceUser.createToken("John", "John1234#") // Generate a valid auth token
        val channel =
            serviceChannel
                .createChannel(authToken.toString(), "Meeting", owner.username, ChannelKind.PRIVATE)
                .let {
                    check(it is Success)
                    it
                }

        val otherUser =
            serviceUser
                .createUser("paul", "Paul4321#")
                .let {
                    check(it is Success)
                    it.value
                }
        val participants =
            serviceChannel
                .addParticipantToChannel(otherUser.username, channel.value.id, Permissions.READ_ONLY)
        val usersOnChannel = serviceChannel.getUsersOfChannel(channel.value.id)
        assertIs<Success<List<Participant>>>(participants)
        assertEquals(2, usersOnChannel.size)
        assertEquals(owner, usersOnChannel[0].user)
        assertEquals(otherUser, usersOnChannel[1].user)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `addParticipantToChannel should return UserIsAlreadyOnChannel error if user already on channel`(trxManager: TransactionManager) {
        val serviceChannel = createChannelService(trxManager)
        val serviceUser = createUserService(trxManager, TestClock())

        val owner =
            serviceUser.createUser(
                "John",
                "John1234#",
            )
        assertIs<Success<User>>(owner)

        val authToken = serviceUser.createToken("John", "John1234#") // Generate a valid auth token
        val channel =
            serviceChannel
                .createChannel(authToken.toString(), "Meeting", owner.value.username, ChannelKind.PRIVATE)
                .let { it as Success<Channel> }

        val res =
            serviceChannel
                .addParticipantToChannel(
                    owner.value.username,
                    channel.value.id,
                    Permissions.READ_ONLY,
                )

        assertIs<Failure<ChannelError>>(res)
        assertIs<ChannelError.UserIsAlreadyOnChannel>(res.value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `addParticipantToChannel should return UserNotFound if participant isn't found`(trxManager: TransactionManager) {
        val serviceChannel = createChannelService(trxManager)
        val serviceUser = createUserService(trxManager, TestClock())

        val owner =
            serviceUser
                .createUser("John", "John1234#")
                .let {
                    check(it is Success)
                    it.value
                }

        val authToken = serviceUser.createToken("John", "John1234#") // Generate a valid auth token
        val channel =
            serviceChannel
                .createChannel(authToken.toString(), "Meeting", owner.username, ChannelKind.PRIVATE)
                .let {
                    check(it is Success)
                    it
                }

        // Try to add unknown participant
        val result =
            serviceChannel.addParticipantToChannel(
                username = "-9999",
                channel.value.id,
                Permissions.READ_ONLY,
            )

        assertTrue(result is Failure)
        assertEquals(result.value, ChannelError.UserNotFound)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `addParticipantToChannel should return ChannelNotFound if channel isn't found`(trxManager: TransactionManager) {
        val serviceChannel = createChannelService(trxManager)
        val serviceUser = createUserService(trxManager, TestClock())

        val owner =
            serviceUser
                .createUser("John", "John1234#")
                .let {
                    check(it is Success)
                    it.value
                }

        val authToken = serviceUser.createToken("John", "John1234#") // Generate a valid auth token
        val channel =
            serviceChannel
                .createChannel(authToken.toString(), "Meeting", owner.username, ChannelKind.PRIVATE)
                .let {
                    check(it is Success)
                    it
                }

        val otherUser =
            serviceUser
                .createUser(
                    "paul",
                    "Paul4321#",
                ).let {
                    check(it is Success)
                    it.value
                }

        // Try to add unknown participant
        val result =
            serviceChannel.addParticipantToChannel(
                otherUser.username,
                channelId = 9999,
                Permissions.READ_ONLY,
            )

        assertTrue(result is Failure)
        assertEquals(result.value, ChannelError.ChannelNotFound)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `removeParticipantOfChannel should return UserIsNotOnChannel error if user is not on channel`(
        trxManager: TransactionManager
    ) {
        val serviceChannel = createChannelService(trxManager)
        val serviceUser = createUserService(trxManager, TestClock())

        val owner =
            serviceUser
                .createUser("John", "John1234#")
                .let {
                    check(it is Success)
                    it.value
                }

        val authToken = serviceUser.createToken("John", "John1234#") // Generate a valid auth token
        val channel =
            serviceChannel
                .createChannel(authToken.toString(), "Meeting", owner.username, ChannelKind.PRIVATE)
                .let {
                    check(it is Success)
                    it
                }

        val otherUser =
            serviceUser
                .createUser("paul", "Paul4321#")
                .let {
                    check(it is Success)
                    it.value
                }

        val remParticipant =
            serviceChannel
                .removeParticipantToChannel(otherUser.username, channel.value.id)

        val usersOnChannel = serviceChannel.getUsersOfChannel(channel.value.id)
        assertEquals(1, usersOnChannel.size)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `removeParticipantOfChannel should remove participant of a channel`(trxManager: TransactionManager) {
        val serviceChannel = createChannelService(trxManager)
        val serviceUser = createUserService(trxManager, TestClock())

        val owner =
            serviceUser
                .createUser("John", "John1234#")
                .let {
                    check(it is Success)
                    it.value
                }

        val authToken = serviceUser.createToken("John", "John1234#") // Generate a valid auth token
        val channel =
            serviceChannel
                .createChannel(authToken.toString(), "Meeting", owner.username, ChannelKind.PRIVATE)
                .let {
                    check(it is Success)
                    it
                }

        val otherUser =
            serviceUser
                .createUser("paul", "Paul4321#")
                .let {
                    check(it is Success)
                    it.value
                }
        val participants =
            serviceChannel
                .addParticipantToChannel(otherUser.username, channel.value.id, Permissions.READ_ONLY)

        val remParticipant =
            serviceChannel
                .removeParticipantToChannel(otherUser.username, channel.value.id)

        assertIs<Failure<ChannelError>>(remParticipant)
        assertIs<ChannelError.UserIsNotOnChannel>(remParticipant.value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `removeParticipantOfChannel should return UserNotFound if participant isn't found`(
        trxManager: TransactionManager
    ) {
        val serviceChannel = createChannelService(trxManager)
        val serviceUser = createUserService(trxManager, TestClock())

        val owner =
            serviceUser
                .createUser("John", "John1234#")
                .let {
                    check(it is Success)
                    it.value
                }

        val authToken = serviceUser.createToken("John", "John1234#") // Generate a valid auth token
        val channel =
            serviceChannel
                .createChannel(authToken.toString(), "Meeting", owner.username, ChannelKind.PRIVATE)
                .let {
                    check(it is Success)
                    it
                }

        val otherUser =
            serviceUser
                .createUser("paul", "Paul4321#")
                .let {
                    check(it is Success)
                    it.value
                }

        val remParticipant =
            serviceChannel
                .removeParticipantToChannel("OtherUser", channel.value.id)

        assertIs<Failure<ChannelError>>(remParticipant)
        assertIs<ChannelError.UserNotFound>(remParticipant.value)
    }


    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `removeParticipantOfChannel should return ChannelNotFound if channel isn't found`(
        trxManager: TransactionManager
    ) {
        val serviceChannel = createChannelService(trxManager)
        val serviceUser = createUserService(trxManager, TestClock())

        val owner =
            serviceUser
                .createUser("John", "John1234#")
                .let {
                    check(it is Success)
                    it.value
                }

        val authToken = serviceUser.createToken("John", "John1234#") // Generate a valid auth token
        val channel =
            serviceChannel
                .createChannel(authToken.toString(), "Meeting", owner.username, ChannelKind.PRIVATE)
                .let {
                    check(it is Success)
                    it
                }

        val otherUser =
            serviceUser
                .createUser("paul", "Paul4321#")
                .let {
                    check(it is Success)
                    it.value
                }

        val remParticipant =
            serviceChannel
                .removeParticipantToChannel(otherUser.username, 9999)

        assertIs<Failure<ChannelError>>(remParticipant)
        assertIs<ChannelError.ChannelNotFound>(remParticipant.value)
    }


    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `createChannel should create a channel based on a channel type PUBLIC`(trxManager: TransactionManager) {
        val serviceChannel = createChannelService(trxManager)
        val serviceUser = createUserService(trxManager, TestClock())

        val owner =
            serviceUser
                .createUser("Organizer", "orgOwner1#")
                .let {
                    check(it is Success)
                    it.value
                }
        val authToken = serviceUser.createToken("Organizer", "orgOwner1#") // Generate a valid auth token
        val expected = Channel (0,  "Meeting", owner, ChannelKind.PUBLIC)


        val channel =
            serviceChannel
                .createChannel(
                    authToken.toString(),
                    "Meeting",
                    ownername = owner.username,
                    ChannelKind.PUBLIC,
                ).let {
                    check(it is Success)
                    it.value
                }

        assertEquals(expected.type, channel.type)
        assertEquals(expected.name, channel.name)
        assertEquals(expected.owner, channel.owner)
        assertEquals(expected.users, channel.users)

    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `createChannel should return UserNotFound when user is not found`(trxManager: TransactionManager) {
        val serviceChannel = createChannelService(trxManager)

        val result =
            serviceChannel.createChannel(
                "authToken",
                "Meeting",
                ownername = "9999",
                ChannelKind.PUBLIC,
            )

        assertTrue(result is Failure)
        assertEquals(result.value, ChannelError.UserNotFound)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `deleteChannel should delete a channel if user is owner`(trxManager: TransactionManager) {
        val serviceChannel = createChannelService(trxManager)
        val serviceUser = createUserService(trxManager, TestClock())

        val owner =
            serviceUser
                .createUser("Organizer", "orgOwner1#")
                .let {
                    check(it is Success)
                    it.value
                }
        val authToken = serviceUser.createToken("Organizer", "orgOwner1#") // Generate a valid auth token
        val expected = Channel (0,  "Meeting", owner, ChannelKind.PUBLIC)


        val channel =
            serviceChannel
                .createChannel(
                    authToken.toString(),
                    "Meeting",
                    ownername = owner.username,
                    ChannelKind.PUBLIC,
                ).let {
                    check(it is Success)
                    it.value
                }

        assertEquals(expected.type, channel.type)
        assertEquals(expected.name, channel.name)
        assertEquals(expected.owner, channel.owner)
        assertEquals(expected.users, channel.users)

        val deletedChannel =
            serviceChannel
                .deleteChannel(
                    authToken.toString(),
                    channel.id,
                    "Organizer"
                )
        //TODO()
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `getUsersOfChannel should return all users in the channel`(trxManager: TransactionManager) {
        val serviceChannel = createChannelService(trxManager)
        val serviceUser = createUserService(trxManager, TestClock())

        val owner =
            serviceUser
                .createUser("Organizer", "orgOwner1#")
                .let {
                    check(it is Success)
                    it.value
                }

        val normalUser1 =
            serviceUser
                .createUser("normal_user1", "Normal#1")
                .let {
                    check(it is Success)
                    it.value
                }

        val authToken = serviceUser.createToken("Organizer", "orgOwner1#") // Generate a valid auth token
        val channel =
            serviceChannel
                .createChannel(authToken.toString(), "Meeting", owner.username, ChannelKind.PRIVATE)
                .let {
                    check(it is Success)
                    it.value
                }
        serviceChannel
            .addParticipantToChannel(normalUser1.username, channel.id, Permissions.READ_WRITE)

        val usersOnChannel = serviceChannel.getUsersOfChannel(channel.id)
        assertEquals(2, usersOnChannel.size)
        assertEquals(owner, usersOnChannel[0].user)
        assertEquals(normalUser1, usersOnChannel[1].user)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `getAllChannels should return all channels`(trxManager: TransactionManager) {
        val serviceChannel = createChannelService(trxManager)
        val serviceUser = createUserService(trxManager, TestClock())

        val owner1 =
            serviceUser
                .createUser("Organizer", "orgOwner1#")
                .let {
                    check(it is Success)
                    it.value
                }

        val authToken = serviceUser.createToken("Organizer", "orgOwner1#") // Generate a valid auth token
        val channel1 =
            serviceChannel
                .createChannel(authToken.toString(), "Meeting", owner1.username, ChannelKind.PUBLIC)
                .let {
                    check(it is Success)
                    it.value
                }
        val channel2 =
            serviceChannel
                .createChannel(authToken.toString(), "Private Meeting", owner1.username, ChannelKind.PRIVATE)
                .let {
                    check(it is Success)
                    it.value
                }

        val expected = listOf(channel1, channel2)

        val channels = serviceChannel.getAllChannels()
        assertEquals(expected, channels)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `createChannel should return UserNotFound if auth token is invalid`(trxManager: TransactionManager) {
        val serviceChannel = createChannelService(trxManager)

        val result =
            serviceChannel.createChannel(
                "invalidAuthToken",
                "Meeting",
                ownername = "1",
                ChannelKind.PUBLIC,
            )

        assertTrue(result is Failure)
        assertEquals(result.value, ChannelError.UserNotFound)
    }


    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `getChannelsOfUser should return all channels of a user`(trxManager: TransactionManager) {
        val serviceChannel = createChannelService(trxManager)
        val serviceUser = createUserService(trxManager, TestClock())

        val owner =
            serviceUser
                .createUser("Organizer", "orgOwner1#")
                .let {
                    check(it is Success)
                    it.value
                }

        val authToken = serviceUser.createToken("Organizer", "orgOwner1#") // Generate a valid auth token
        val channel1 =
            serviceChannel
                .createChannel(authToken.toString(), "Meeting", owner.username, ChannelKind.PUBLIC)
                .let {
                    check(it is Success)
                    it.value
                }
        val channel2 =
            serviceChannel
                .createChannel(authToken.toString(), "Private Meeting", owner.username, ChannelKind.PRIVATE)
                .let {
                    check(it is Success)
                    it.value
                }

        val channels = serviceChannel.getChannelsOfUser(owner.id)
        assertTrue(channels is Success)
        assertEquals(listOf(channel1, channel2), channels.value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `getChannelsOfUser should return empty list if user has no channels`(trxManager: TransactionManager) {
        val serviceChannel = createChannelService(trxManager)
        val serviceUser = createUserService(trxManager, TestClock())

        val user =
            serviceUser
                .createUser("UserWithoutChannels", "Pa\$w0rd1!")
                .let {
                    check(it is Success)
                    it.value
                }

        val channels = serviceChannel.getChannelsOfUser(user.id)
        assertTrue(channels is Success)
        assertEquals(0, channels.value.size)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `getChannelsOfUser should return UserNotFound if user does not exist`(trxManager: TransactionManager) {
        val serviceChannel = createChannelService(trxManager)

        val result = serviceChannel.getChannelsOfUser(-9999)
        assertTrue(result is Failure)
        assertEquals(result.value, ChannelError.UserNotFound)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `getChannelsByOwner should return all channels of a specific owner`(trxManager: TransactionManager) {
        val serviceChannel = createChannelService(trxManager)
        val serviceUser = createUserService(trxManager, TestClock())

        val owner =
            serviceUser
                .createUser("Owner", "Owner1234#")
                .let {
                    check(it is Success)
                    it.value
                }

        val authToken = serviceUser.createToken("Owner", "Owner1234#") // Generate a valid auth token
        val channel1 =
            serviceChannel
                .createChannel(authToken.toString(), "Channel1", owner.username, ChannelKind.PUBLIC)
                .let {
                    check(it is Success)
                    it.value
                }
        val channel2 =
            serviceChannel
                .createChannel(authToken.toString(), "Channel2", owner.username, ChannelKind.PRIVATE)
                .let {
                    check(it is Success)
                    it.value
                }

        val channels = serviceChannel.getChannelsByOwner(owner.username)
        assertTrue(channels is Success)
        assertEquals(listOf(channel1, channel2), channels.value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `getChannelsByOwner should return empty list if owner has no channels`(trxManager: TransactionManager) {
        val serviceChannel = createChannelService(trxManager)
        val serviceUser = createUserService(trxManager, TestClock())

        val owner =
            serviceUser
                .createUser("OwnerWithoutChannels", "Pa$\$w0rd1!")
                .let {
                    check(it is Success)
                    it.value
                }

        val channels = serviceChannel.getChannelsByOwner(owner.username)
        assertTrue(channels is Success)
        assertTrue(channels.value.isEmpty())
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `getChannelsByOwner should return UserNotFound if owner does not exist`(trxManager: TransactionManager) {
        val serviceChannel = createChannelService(trxManager)

        val result = serviceChannel.getChannelsByOwner("alice")
        assertTrue(result is Failure)
        assertEquals(ChannelError.UserNotFound, result.value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `getChannelById should return the channel with the given id`(trxManager: TransactionManager) {
        val serviceChannel = createChannelService(trxManager)
        val serviceUser = createUserService(trxManager, TestClock())

        val owner =
            serviceUser
                .createUser("Owner", "Owner1234#")
                .let {
                    check(it is Success)
                    it.value
                }

        val authToken = serviceUser.createToken("Owner", "Owner1234#") // Generate a valid auth token
        val channel =
            serviceChannel
                .createChannel(authToken.toString(), "Channel1", owner.username, ChannelKind.PUBLIC)
                .let {
                    check(it is Success)
                    it.value
                }

        val result = serviceChannel.getChannelById(channel.id)

        assertTrue(result is ChannelInfo)
        assertEquals(channel.name, result.name)
        assertEquals(channel.type, result.type)
        assertEquals(owner.username, result.owner)
    }
}
