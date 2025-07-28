package pt.isel

/**
 * pt.isel.pt.isel.Repository interface for managing participants, extends the generic pt.isel.pt.isel.Repository
 */
interface RepositoryParticipant : Repository<Participant> {
    /**
     * For a pt.isel.Participant in a TimeSlotSingle then this slot argument is null.
     */
    fun createParticipant(
        channel: Channel,
        user: User,
        permission: Permissions,
    ): Participant

    fun removeParticipant(
        channel: Channel,
        user: User
    )

    fun findByUsername(
        username: String,
        channel: Channel,
    ): Participant?

    fun findAllByChannel(channel: Channel): List<Participant>
}