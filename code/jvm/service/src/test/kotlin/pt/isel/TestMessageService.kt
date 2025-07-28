package pt.isel

import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import pt.isel.mem.TransactionManagerInMem
import java.sql.Timestamp
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

class TestMessageService {
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
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `createMessage should return create a message by user on the channel`(trxManager: TransactionManager) {
        val serviceChannel = ChannelService(trxManager, Sha256TokenEncoder())
        val serviceUser = createUserService(trxManager, TestClock())
        val serviceMessage = MessageService(trxManager, Sha256TokenEncoder())

        val time = Timestamp(System.currentTimeMillis())

        val owner =
            serviceUser
                .createUser("Alice", "Alice1234#")
                .let {
                    check(it is Success)
                    it.value
                }

        val authToken = serviceUser.createToken("Alice", "Alice1234#") // Generate a valid auth token

        val channel =
            serviceChannel
                .createChannel(authToken.toString(), "Meeting", owner.username, ChannelKind.PRIVATE)
                .let {
                    check(it is Success)
                    it.value
                }

        val result =
            serviceMessage
                .createMessage(
                    authToken.toString(),
                    owner.username,
                    channel.id,
                    "Good morning everyone!",

                ).let {
                    check(it is Success)
                    it as Success<Message>
                }

        assertIs<Success<Message>>(result)
        assertEquals("Good morning everyone!", result.value.content)
       // assertEquals(time, result.value.time)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `createMessage should return UserNotFound if user doesn't exist`(trxManager: TransactionManager) {
        val serviceChannel = ChannelService(trxManager, Sha256TokenEncoder())
        val serviceMessage = MessageService(trxManager, Sha256TokenEncoder())
        val serviceUser = createUserService(trxManager, TestClock())

        val time = Timestamp(System.currentTimeMillis())
        val owner =
            serviceUser
                .createUser("Bob", "Bob1234#")
                .let {
                    check(it is Success)
                    it.value
                }

        val authToken = serviceUser.createToken("Bob", "Bob1234#") // Generate a valid auth token

        val channel =
            serviceChannel
                .createChannel(authToken.toString(), "Meeting", owner.username, ChannelKind.PRIVATE)
                .let {
                    check(it is Success)
                    it.value
                }

        val result: Either<MessageError, Message> =
            serviceMessage.createMessage(
                "invalidToken",
                "999",
                channel.id,
                "Good morning everyone!",

            )

        assertIs<Failure<MessageError>>(result)
        assertIs<MessageError.UserNotFound>(result.value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `createMessage should return ChannelNotFound if channel doesn't exist`(trxManager: TransactionManager) {
        val serviceUser = createUserService(trxManager, TestClock())
        val serviceMessage = MessageService(trxManager, Sha256TokenEncoder())

        val time = Timestamp(System.currentTimeMillis())

        val owner =
            serviceUser
                .createUser("Charlie", "Charlie1234#")
                .let {
                    check(it is Success)
                    it.value
                }

        val result: Either<MessageError, Message> =
            serviceMessage.createMessage(
                "validToken",
                owner.username,
                999,
                "Good morning everyone!",
            )

        assertIs<Failure<MessageError>>(result)
        assertIs<MessageError.ChannelNotFound>(result.value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `createMessage should return UserIsNotParticipant if user is not a participant`(trxManager: TransactionManager) {
        val serviceChannel = ChannelService(trxManager, Sha256TokenEncoder())
        val serviceUser = createUserService(trxManager, TestClock())
        val serviceMessage = MessageService(trxManager, Sha256TokenEncoder())

        val time = Timestamp(System.currentTimeMillis())

        val owner =
            serviceUser
                .createUser("David", "David1234#")
                .let {
                    check(it is Success)
                    it.value
                }

        val user1 =
            serviceUser
                .createUser("Emilia", "Emilia1234#")
                .let {
                    check(it is Success)
                    it.value
                }

        val authToken = serviceUser.createToken("David", "David1234#") // Generate a valid auth token

        val channel =
            serviceChannel
                .createChannel(authToken.toString(), "Meeting", owner.username, ChannelKind.PRIVATE)
                .let {
                    check(it is Success)
                    it.value
                }

        val result =
            serviceMessage.createMessage(
                "validToken",
                user1.username,
                channel.id,
                "Good morning everyone!",

            )
        assertIs<Failure<MessageError>>(result)
        assertIs<MessageError.UserIsNotParticipant>(result.value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `deleteMessage should return delete a message by user on the channel`(trxManager: TransactionManager) {
        val serviceChannel = ChannelService(trxManager, Sha256TokenEncoder())
        val serviceUser = createUserService(trxManager, TestClock())
        val serviceMessage = MessageService(trxManager, Sha256TokenEncoder())

        val time = Timestamp(System.currentTimeMillis())

        val owner =
            serviceUser
                .createUser("Eve", "Eve1234#")
                .let {
                    check(it is Success)
                    it.value
                }

        val authToken = serviceUser.createToken("Eve", "Eve1234#") // Generate a valid auth token

        val channel =
            serviceChannel
                .createChannel(authToken.toString(), "Meeting", owner.username, ChannelKind.PRIVATE)
                .let {
                    check(it is Success)
                    it.value
                }
        val message =
            serviceMessage.createMessage(
                "validToken",
                owner.username,
                channel.id,
                "Good morning everyone!",
            )
        val validatedMessage =
            message.let {
                check(it is Success)
                it.value
            }

        assertIs<Success<Message>>(message)

        serviceMessage.deleteMessage(validatedMessage.id)

        // assertTrue()
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `deleteMessage should return MessageNotFound if message doesn't exist`(trxManager: TransactionManager) {
        val serviceChannel = ChannelService(trxManager, Sha256TokenEncoder())
        val serviceUser = createUserService(trxManager, TestClock())
        val serviceMessage = MessageService(trxManager, Sha256TokenEncoder())

        val owner =
            serviceUser
                .createUser("Frank", "Frank1234#")
                .let {
                    check(it is Success)
                    it.value
                }

        val authToken = serviceUser.createToken("Frank", "Frank1234#") // Generate a valid auth token

        val channel =
            serviceChannel
                .createChannel(authToken.toString(), "Meeting", owner.username, ChannelKind.PRIVATE)
                .let {
                    check(it is Success)
                    it.value
                }

        val result = serviceMessage.deleteMessage(999)

        assertIs<Failure<MessageError>>(result)
        assertIs<MessageError.MessageNotFound>(result.value)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `getMessagesByChannelId should return all the messages on the channel`(trxManager: TransactionManager) {
        val serviceChannel = ChannelService(trxManager, Sha256TokenEncoder())
        val serviceUser = createUserService(trxManager, TestClock())
        val serviceMessage = MessageService(trxManager, Sha256TokenEncoder())

        val time1 = Timestamp(System.currentTimeMillis())

        val owner =
            serviceUser
                .createUser("Grace", "Grace1234#")
                .let {
                    check(it is Success)
                    it.value
                }

        val authToken = serviceUser.createToken("Grace", "Grace1234#") // Generate a valid auth token

        val channel =
            serviceChannel
                .createChannel(authToken.toString(), "Meeting", owner.username, ChannelKind.PRIVATE)
                .let {
                    check(it is Success)
                    it.value
                }
        val time2 = Timestamp(System.currentTimeMillis())

        val message1 =
            serviceMessage
                .createMessage(
                    "validToken",
                    owner.username,
                    channel.id,
                    "Good morning everyone!",
                ).let {
                    check(it is Success)
                    it.value
                }

        val message2 =
            serviceMessage
                .createMessage(
                    "validToken",
                    owner.username,
                    channel.id,
                    "How is everyone doing?",
                ).let {
                    check(it is Success)
                    it.value
                }

        val result = serviceMessage.getMessagesByChannelId(channel.id)

        val messages =
            result
                .let {
                    check(it is Success)
                    it.value as List<Message>
                }

        assertIs<Success<List<Message>>>(result)
        assertEquals(listOf(message1, message2), messages)
    }

    @ParameterizedTest
    @MethodSource("transactionManagers")
    fun `getMessagesByChannelId should return ChannelNotFound if channel doesn't exist`(trxManager: TransactionManager) {
        val serviceMessage = MessageService(trxManager, Sha256TokenEncoder())

        val result = serviceMessage.getMessagesByChannelId(999)

        assertIs<Failure<MessageError>>(result)
        assertIs<MessageError.ChannelNotFound>(result.value)
    }
}
