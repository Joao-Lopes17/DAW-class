package pt.isel.mem

import jakarta.inject.Named
import pt.isel.*

/**
 * Naif in memory repository non thread-safe and basic sequential id.
 * Useful for unit tests purpose.
 */
@Named
class RepositoryChannelInMem : RepositoryChannel {
    private val channels =
        mutableListOf<Channel>(
            Channel(
                1,
                "channel",
                User(
                    1,
                    "Roberto",
                    PasswordValidationInfo(
                        newTokenValidationData(),
                    ),
                ),
                type = ChannelKind.PUBLIC,
            ),
        )

    override fun createChannel(
        name: String,
        owner: User,
        kind: ChannelKind,
    ): Channel =
        Channel(channels.count(), name, owner, kind)
            .also { channels.add(it) }

    override fun findAllByUser(user: User): List<Channel> = channels.filter { it.owner.id == user.id }

    override fun findById(id: Int): Channel? = channels.firstOrNull { it.id == id }

    override fun findAll(): List<Channel> = channels.toList()

    override fun save(entity: Channel) {
        channels.removeIf { it.id == entity.id }
        channels.add(entity)
    }

    override fun deleteById(id: Int) {
        channels.removeIf { it.id == id }
    }

    override fun clear() {
        channels.clear()
    }
}
