package me.wlko.algorithmd.utils

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.reflect.KClass

/**
 * Sets value in [DatabaseReference] implemented as a suspend function
 *
 * @see setValue
 */
suspend fun DatabaseReference.setValueSuspend(obj: Any): Unit = suspendCoroutine { cont ->
    setValue(obj) { _, _ -> cont.resume(Unit) }
}

/**
 * Get value of [DataSnapshot] and deserialize it using [Gson].
 */
internal fun <T : Any> DataSnapshot.getValue(clazz: KClass<T>): T {
    val res = value as Map<*, *>
    // TODO: make this cleaner, e.g. a self-written tree-walk
    val jsonElement = Gson().toJsonTree(res)
    return Gson().fromJson(jsonElement, clazz.java)
}

/**
 * Simple [ValueEventListener] for [readSingle]
 */
class SuspendCoroutineSingleValueListener<T : Any>(
    private val continuation: Continuation<T?>,
    private val clazz: KClass<T>
) : ValueEventListener {
    override fun onDataChange(snapshot: DataSnapshot?) {
        val res = snapshot?.getValue(clazz)
        continuation.resume(res)
    }

    override fun onCancelled(error: DatabaseError?) {
        println(error)
    }

}

/**
 * Reads single value of [DatabaseReference] and deserializes. If reference is not present, returns `null`.
 */
suspend inline fun <reified T : Any> DatabaseReference.readSingle(): T? = suspendCoroutine { cont ->
    addListenerForSingleValueEvent(SuspendCoroutineSingleValueListener(cont, T::class))
}