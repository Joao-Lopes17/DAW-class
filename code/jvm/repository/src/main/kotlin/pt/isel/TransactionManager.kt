package pt.isel

/**
 * Yuml class diagram

// Define interfaces
[pt.isel.pt.isel.TransactionManager|block: (pt.isel.pt.isel.Transaction) \-\> R]
[<<pt.isel.pt.isel.TransactionManager>>;TransactionManagerJdbi|block: (pt.isel.pt.isel.Transaction) \-\> R]
[<<pt.isel.pt.isel.Transaction>>;TransactionJdbi|handle: Handle]
[<<RepositoryParticipants>>;RepositoryParticipantsJdbi|handle: Handle]
[<<RepositoryEvents>>;RepositoryEventsJdbi|handle: Handle]
[<<RepositoryTimeSlot>>;RepositoryTimeSlotJdbi|handle: Handle]

// Relations
[TransactionManager]uses-.->[Transaction]
[TransactionManagerJdbi]new-.->[TransactionJdbi]

[Transaction]->[RepositoryChannel]
[Transaction]->[RepositoryMessage]
[Transaction]->[RepositoryUser]
[Transaction]->[RepositoryParticipant]
[Transaction]->[RepositoryInvitation]
 */
interface TransactionManager {
    /**
     * This method creates an instance of pt.isel.pt.isel.Transaction, potentially
     * initializing a JDBC Connection,a JDBI Handle, or another resource,
     * which is then passed as an argument to the pt.isel.pt.isel.Transaction constructor.
     */
    fun <R> run(block: Transaction.() -> R): R
}
