package me.wlko.algorithmd.utils

import com.google.firebase.database.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Sets value in [DatabaseReference] implemented as a suspend function
 *
 * @see setValue
 */
suspend fun DatabaseReference.setValueSuspend(obj: Any): Unit = suspendCoroutine { cont ->
    setValue(obj) { _, _ -> cont.resume(Unit) }
}

/**
 * Reads single value of [DatabaseReference] and deserializes. If reference is not present, returns `null`.
 */
suspend inline fun <reified T : Any> DatabaseReference.readSingle(): T? = suspendCoroutine { cont ->
    addListenerForSingleValueEvent(SuspendCoroutineSingleValueListener(cont, T::class))
}

/**
 * Reads single value of [DatabaseReference] as list of [T]. If null, returns empty list.
 */
suspend inline fun <reified T : Any> DatabaseReference.readList(): List<T> {
    val map = this.readSingle<Map<*, *>>()
    return map.orEmpty().values.filterNotNull().map { parseObjectNotNull(it, T::class) }
}

/**
 * Simple [Transaction.Handler] for [runTransactionSuspend].
 *
 * Returns final [DataSnapshot] to [Continuation].
 *
 * Executes [doTransactionFunc] as transaction
 */
private class ContinuationTransactionHandler(
    private val cont: Continuation<DataSnapshot?>,
    private val doTransactionFunc: (currentData: MutableData?) -> Transaction.Result
) : Transaction.Handler {
    override fun doTransaction(currentData: MutableData?): Transaction.Result = doTransactionFunc(currentData)

    override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
        // if no error, return snapshot; otherwise throw exception
        if (error == null)
            cont.resume(currentData)
        else
            cont.resumeWithException(error.toException())
    }
}

/**
 * Runs [Transaction] in a suspend.
 *
 * @param doTransaction Transaction function
 *
 * @return [DataSnapshot]
 */
internal suspend fun DatabaseReference.runTransactionSuspend(
    doTransaction: (currentData: MutableData?) -> Transaction.Result
): DataSnapshot? = suspendCoroutine { cont ->
    runTransaction(ContinuationTransactionHandler(cont, doTransaction))
}
