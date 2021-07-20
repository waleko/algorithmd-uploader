package me.wlko.algorithmd.utils

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.MutableData
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.reflect.KClass

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
        continuation.resumeWithException(error?.toException() ?: Exception("$this could not single-read value"))
    }

}

/**
 * Get value of [DataSnapshot] and deserialize it using [Gson].
 */
internal fun <T : Any> DataSnapshot.getValue(clazz: KClass<T>): T? = parseObject(value, clazz)

/**
 * Get value of [MutableData] and deserialize it using [Gson].
 */
internal fun <T : Any> MutableData.getValue(clazz: KClass<T>): T? = parseObject(value, clazz)

/**
 * Recursively deserialize [obj] as nullable [T] using [Gson].
 */
fun <T : Any> parseObject(obj: Any?, clazz: KClass<T>): T? {
    return if (obj != null)
        parseObjectNotNull(obj, clazz)
    else
        null
}

/**
 * Recursively deserialize [obj] as non-nullable [T] using [Gson]
 */
fun <T : Any> parseObjectNotNull(obj: Any, clazz: KClass<T>): T {
    // TODO: maybe make this cleaner? e.g. a self-written tree walk
    val jsonElement = Gson().toJsonTree(obj)
    return Gson().fromJson(jsonElement, clazz.java)
}