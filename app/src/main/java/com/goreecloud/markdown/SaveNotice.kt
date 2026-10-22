package com.goreecloud.markdown

/** A successful provider write stays successful even if private cleanup fails. */
internal object SaveNotice {
    fun message(newerEditsRemain: Boolean, recoveryCleanupPending: Boolean): String = when {
        newerEditsRemain && recoveryCleanupPending ->
            "Saved earlier edit; newer changes remain. Private recovery cleanup is pending."
        newerEditsRemain ->
            "Saved earlier edit; newer changes remain."
        recoveryCleanupPending ->
            "Save verified. A redundant private recovery draft could not be removed."
        else -> "Save verified."
    }
}
