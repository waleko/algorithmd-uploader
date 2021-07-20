package me.wlko.algorithmd.utils

import io.ktor.application.*
import io.ktor.config.*
import me.wlko.algorithmd.CodeRecord
import me.wlko.algorithmd.FullCodeRecord
import me.wlko.algorithmd.NewCodeRecord
import java.util.*
import kotlin.random.Random

/**
 * Generates random UUID
 */
fun newUUID() = UUID.randomUUID().toString()

/**
 * Generates random alphanumeric string of length [lineLength]
 */
internal fun generateLine(lineLength: Int): String {
    val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
    return (1..lineLength).map { allowedChars.random() }.joinToString("")
}

/**
 * Generates random alphanumeric text
 *
 * @param lines Amount of lines in text
 * @param lineLength Length of every line
 */
fun generateLongText(lines: Int = 100, lineLength: Int = 200): String {
    return (1..lines).joinToString("\n") { generateLine(lineLength) }
}

/**
 * Generate random amount of random tags
 *
 * @param maxAmount Maximum amount of tags
 */
fun randomTags(maxAmount: Int) = exactTags(Random.nextInt(maxAmount))

/**
 * Generate exact amount of random tags
 *
 * @param amount Amount of tags
 */
fun exactTags(amount: Int) = (1..amount).map { newUUID() }

/**
 * Check if this [CodeRecord] could be initialized from [newCodeRecord]. Used for testing.
 */
fun CodeRecord.matches(newCodeRecord: NewCodeRecord): Boolean {
    if (newCodeRecord.filename != filename)
        return false
    if (newCodeRecord.language != language)
        return false
    // FIXME: arrays are stored as objects in realtime database
    //  therefore an empty list is truly `null`
    //  during serialization Gson struggles to serialize `null` as list,
    //  therefore `.orEmpty()` is needed.
    //  Possible workarounds could be perhaps making a custom `JsonDeserializer`
    if (newCodeRecord.tagItems.orEmpty() != tagItems.orEmpty())
        return false
    if (newCodeRecord.title != title)
        return false
    // preview content check
    val zipped = preview_content
        .split("\n")
        .zip(newCodeRecord.full_content.split("\n"))
    return zipped.all { (preview, full) ->
        val trimmed = if (preview.endsWith("..."))
            preview.dropLast(3)
        else
            preview
        return@all full.startsWith(trimmed)
    }
}

/**
 * Check if this [FullCodeRecord] could be initialized from [newCodeRecord]. Used for testing.
 */
fun FullCodeRecord.matches(newCodeRecord: NewCodeRecord): Boolean {
    return full_content == newCodeRecord.full_content && info.matches(newCodeRecord)
}

/**
 * Set application environment properties.
 */
fun ApplicationEnvironment.config(values: Map<String, String>) {
    (config as MapApplicationConfig).apply {
        values.forEach { (path, value) ->
            put(path, value)
        }
    }
}
