package me.wlko.algorithmd

import kotlinx.serialization.Serializable

/**
 * Data model representing client's request to save a code fragment
 */
@Serializable
data class NewCodeRecord(
    val title: String,
    val language: String,
    val tagItems: List<String>,
    val filename: String,
    val full_content: String
) {
    /**
     * Validates [NewCodeRecord].
     */
    fun simpleValidate() {
        if (title.isEmpty() || title.length > 100)
            error("Title invalid")
        if (language.isEmpty() || language.length > 100)
            error("Language invalid")
        if (filename.isEmpty() || filename.length > 100)
            error("Filename invalid")
        if (tagItems.size > 100)
            error("Tag items invalid")
        if (full_content.isEmpty())
            error("No content")
    }

    fun quotaValidate(quota: UploadQuota) {
        if (quota.cur_amount > quota.max_amount)
            error("Upload quota exceeded")
        if (1L * full_content.length * Char.SIZE_BYTES >= quota.max_upload_size_KB * 1024)
            error("Exceeded maximum upload size (${quota.max_upload_size_KB}KB)")
    }
}

/**
 * Data model representing code record with preview content
 * limited to 10 lines and 100 columns (for faster list load times)
 */
@Serializable
data class CodeRecord(
    val uid: String,
    val title: String,
    val language: String,
    val preview_content: String,
    val tagItems: List<String>,
    val filename: String
) {
    constructor(newCodeRecord: NewCodeRecord, uid: String, previewLines: Int = 10, previewColumns: Int = 100) : this(
        uid,
        newCodeRecord.title,
        newCodeRecord.language,
        newCodeRecord.full_content
            .split('\n')
            .take(previewLines) // limit lines
            .joinToString("\n") {
                // limit columns
                if (it.length <= previewColumns)
                    it
                else
                    "${it.take(previewColumns)}..."
            },
        newCodeRecord.tagItems,
        newCodeRecord.filename
    )
}

/**
 * Data model representing code record with full content
 */
@Serializable
data class FullCodeRecord(
    val full_content: String,
    val info: CodeRecord
)

/**
 * Data model representing upload quota for every user
 */
@Serializable
data class UploadQuota(
    val cur_amount: Int,
    val max_amount: Int,
    val max_upload_size_KB: Int
)