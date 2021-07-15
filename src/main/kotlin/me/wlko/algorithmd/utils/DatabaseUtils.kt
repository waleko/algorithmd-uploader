package me.wlko.algorithmd.utils

import com.google.firebase.database.DatabaseReference
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Sets value in [DatabaseReference] implemented as a suspend function
 *
 * @see setValue
 */
suspend fun DatabaseReference.setValueSuspend(obj: Any): Unit = suspendCoroutine { cont ->
    setValue(obj) { _, _ -> cont.resume(Unit) }
}