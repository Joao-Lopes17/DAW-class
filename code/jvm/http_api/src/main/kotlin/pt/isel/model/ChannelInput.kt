package pt.isel.model

import pt.isel.ChannelKind

data class ChannelInput(
    //val authToken: String,
    val name: String,
    val ownerName: String,
    val type: ChannelKind,
)
