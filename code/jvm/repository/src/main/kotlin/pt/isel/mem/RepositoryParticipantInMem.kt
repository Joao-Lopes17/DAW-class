package pt.isel.mem

import jakarta.inject.Named
import pt.isel.*

/**
 * Naif in memory repository non thread-safe and basic sequential id.
 * Useful for unit tests purpose.
 */
@Named
class RepositoryParticipantInMem : RepositoryParticipant {
    private val participants = mutableListOf<Participant>(Participant(1, Channel(
        1, "channel", User(
            1, "Roberto", PasswordValidationInfo(
                newTokenValidationData()
            ),
        ),
        type = ChannelKind.PUBLIC
    ),
        User(1, "Roberto", PasswordValidationInfo(newTokenValidationData())),
        Permissions.READ_WRITE
        ))

    override fun createParticipant(
        channel: Channel,
        user: User,
        permission: Permissions
    ): Participant =
        Participant(participants.count(), channel, user, permission)
            .also { participants.add(it) }

    override fun removeParticipant(channel: Channel, user: User) {
        for(i in participants){
            if(i.channel.id == channel.id && i.user.id == user.id) {
                participants.remove(i)
                return
            }
        }
    }


    override fun findByUsername(
        username: String,
        channel: Channel,
    ): Participant? =
        participants.firstOrNull {
            it.user.username == username && it.channel.id == channel.id
        }

    override fun findAllByChannel(channel: Channel): List<Participant> {
        val a = mutableListOf<Participant>()
        for(i in participants){
            if(i.channel == channel) a.add(i)
        }
        return a
    }

    override fun findById(id: Int): Participant? = participants.firstOrNull { it.id == id }

    override fun findAll(): List<Participant> = participants.toList()

    override fun save(entity: Participant) {
        participants.removeIf { it.id == entity.id }
        participants.add(entity)
    }

    override fun deleteById(id: Int) {
        participants.removeIf { it.id == id }
    }

    override fun clear() {
        participants.clear()
    }
}