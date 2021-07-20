package me.wlko.algorithmd.utils

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Transaction
import kotlinx.coroutines.runBlocking
import me.wlko.algorithmd.UploadQuota

/**
 * Increments [UploadQuota.cur_amount] atomically and returns quota for user [uid].
 *
 * If no custom quota is present, copies value from default value.
 */
suspend fun incrementAndGetUploadQuota(db: FirebaseDatabase, uid: String): UploadQuota {
    // check default quotas for everyone
    val defaultQuota =
        db.getReference("/limits/defaultLimit").readSingle<UploadQuota>() ?: error("Default quota not set!")
    val newQuotaSnapshot = db.getReference("/limits/customQuotas/${uid}").runTransactionSuspend { currentData ->
        // get current quota value from the database
        val currentQuotaValue: UploadQuota = runBlocking {
            // check custom quotas set to user or return default if custom is not yet set
            return@runBlocking currentData?.getValue(UploadQuota::class) ?: defaultQuota
        }
        // increment current amount for quota
        val newQuotaValue = currentQuotaValue.run { copy(cur_amount = cur_amount + 1) }
        // set new value
        currentData?.value = newQuotaValue
        return@runTransactionSuspend Transaction.success(currentData)
    }
    // return updated quota value
    return newQuotaSnapshot?.getValue(UploadQuota::class) ?: error("Incremented quota is null")
}

suspend fun incrementQuota(db: FirebaseDatabase, uid: String) = addToQuotaCount(db, uid, 1)
suspend fun decrementQuota(db: FirebaseDatabase, uid: String) = addToQuotaCount(db, uid, -1)

private suspend fun addToQuotaCount(db: FirebaseDatabase, uid: String, value: Int) {
    db.getReference("/limits/customQuotas/${uid}/cur_amount")
        .runTransactionSuspend { currentData ->
            val curValue: Int = currentData?.getValue(Int::class.java) ?: 0
            currentData?.value = curValue + value
            return@runTransactionSuspend Transaction.success(currentData)
        }
}
