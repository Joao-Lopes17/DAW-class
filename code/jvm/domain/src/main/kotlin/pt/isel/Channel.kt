package pt.isel

data class Channel(
    val id: Int,
    val name: String,
    val owner: User,
    var type: ChannelKind,
) {
    val users = mutableListOf<User>()

    fun addUser(user: User) {
        users.add(user)
    }

    fun removeUser(user: User) {
        users.remove(user)
    }
}
