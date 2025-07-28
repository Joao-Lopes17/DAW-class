package pt.isel.mem

import pt.isel.RepositoryChannel
import pt.isel.RepositoryInvitation
import pt.isel.RepositoryMessage
import pt.isel.RepositoryParticipant
import pt.isel.RepositoryUser
import pt.isel.Transaction

class TransactionInMem(
    override val repoChannel: RepositoryChannel,
    override val repoUser: RepositoryUser,
    override val repoMessage: RepositoryMessage,
    override val repoParticipant: RepositoryParticipant,
    override val repoInvitation: RepositoryInvitation
) : Transaction {
    override fun rollback(): Unit = throw UnsupportedOperationException() }
