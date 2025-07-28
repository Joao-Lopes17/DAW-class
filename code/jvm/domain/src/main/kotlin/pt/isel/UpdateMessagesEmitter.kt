package pt.isel


interface UpdatedMessagesEmitter {
    fun emit(signal: UpdatedMessage)

    fun onCompletion(callback: () -> Unit)

    fun onError(callback: (Throwable) -> Unit)
}