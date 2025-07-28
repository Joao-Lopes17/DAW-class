package pt.isel

/**
 * The lifecycle of a pt.isel.pt.isel.Transaction is managed outside the scope of the IoC/DI container.
 * Transactions are instantiated by a pt.isel.pt.isel.TransactionManager,
 * which is managed by the IoC/DI container (e.g., Spring).
 * The implementation of pt.isel.pt.isel.Transaction is responsible for creating the
 * necessary repository instances in its constructor.
 */
interface Transaction {
    val repoChannel: RepositoryChannel
    val repoUser: RepositoryUser
    val repoMessage: RepositoryMessage
    val repoParticipant: RepositoryParticipant
    val repoInvitation: RepositoryInvitation

    fun rollback()
}
