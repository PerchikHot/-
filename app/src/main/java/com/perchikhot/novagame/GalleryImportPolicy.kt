package com.perchikhot.novagame

/** Only keep document URIs whose read permission survives app restarts.
 * ACTION_OPEN_DOCUMENT can return providers that refuse persistable grants.
 * Saving such URIs creates permanently broken gallery tiles on next launch.
 */
internal object GalleryImportPolicy {
    fun shouldSave(uri: String, persistedReadGrant: Boolean): Boolean {
        if (!persistedReadGrant || uri.length !in 11..2048 ||
            !uri.startsWith("content://") ||
            uri.any { it.isISOControl() || it.isWhitespace() }) return false
        // A bare content:// URI or a forged authority is not a document.
        // Persisted grants are scoped to a provider; never save credentials,
        // ports or query/fragment variants that cannot be reopened reliably.
        val authority = uri.removePrefix("content://").substringBefore('/')
            .substringBefore('?').substringBefore('#')
        if (!Regex("[A-Za-z0-9_][A-Za-z0-9_.-]*").matches(authority) ||
            '?' in uri || '#' in uri || '@' in uri) return false
        return true
    }
}
