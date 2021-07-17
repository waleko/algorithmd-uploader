package me.wlko.algorithmd.utils

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.MutableData
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
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
        println(error)
    }

}

/**
 * Get value of [DataSnapshot] and deserialize it using [Gson].
 */
internal fun <T : Any> DataSnapshot.getValue(clazz: KClass<T>): T = parseMap(value as Map<*, *>, clazz)

/**
 * Get value of [MutableData] and deserialize it using [Gson].
 */
internal fun <T : Any> MutableData.getValue(clazz: KClass<T>): T = parseMap(value as Map<*, *>, clazz)

/**
 * Recursively deserialize [Map] as [T] using [Gson]
 */
private fun <T : Any> parseMap(map: Map<*, *>, clazz: KClass<T>): T {
    // TODO: maybe make this cleaner? e.g. a self-written tree walk
    val jsonElement = Gson().toJsonTree(map)
    return Gson().fromJson(jsonElement, clazz.java)
}