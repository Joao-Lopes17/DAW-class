package pt.isel

interface RepositoryChannel : Repository<Channel> {
    fun createChannel(
        name: String,
        owner: User,
        kind: ChannelKind,
    ): Channel

    fun findAllByUser(user: User): List<Channel>
}
