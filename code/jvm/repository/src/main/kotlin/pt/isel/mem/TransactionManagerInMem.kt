package pt.isel.mem

import pt.isel.Transaction
import pt.isel.TransactionManager

// @Named
class TransactionManagerInMem : TransactionManager {
    private val repoChannel = RepositoryChannelInMem()
    private val repoUser = RepositoryUserInMem()
    private val repoMessage = RepositoryMessageInMem()
    private val repoParticipant = RepositoryParticipantInMem()
    private val repositoryInvitation = RepositoryInvitationInMem()

    override fun <R> run(block: Transaction.() -> R): R =
        block(TransactionInMem(repoChannel, repoUser, repoMessage, repoParticipant, repositoryInvitation))
}
