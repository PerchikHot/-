package com.perchikhot.novagame

/** Only keep document URIs whose read permission survives app restarts.
 * ACTION_OPEN_DOCUMENT can return providers that refuse persistable grants.
 * Saving such URIs creates permanently broken gallery tiles on next launch.
 */
internal object GalleryImportPolicy {
    fun shouldSave(uri: String, persistedReadGrant: Boolean): Boolean =
        persistedReadGrant && uri.startsWith("content://") &&
            uri.length in 11..2048 &&
            uri.none { it == '\n' || it == '\r' || it == '\u0000' }
}
