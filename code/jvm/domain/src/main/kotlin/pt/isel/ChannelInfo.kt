package pt.isel

data class ChannelInfo(
    val name: String,
    val type: ChannelKind,
    val owner: String,
)
