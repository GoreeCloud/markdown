package com.goreecloud.markdown

/**
 * An unreadable private recovery draft must never hide a successfully opened
 * provider document. The draft is left untouched for later inspection.
 */
internal data class RecoveryLoadResult(
    val draft: String?,
    val readFailed: Boolean,
)

internal object RecoveryReader {
    fun readSafely(readDraft: () -> String?): RecoveryLoadResult =
        try {
            RecoveryLoadResult(readDraft(), readFailed = false)
        } catch (_: Exception) {
            RecoveryLoadResult(draft = null, readFailed = true)
        }
}
