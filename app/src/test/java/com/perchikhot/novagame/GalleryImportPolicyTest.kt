package com.perchikhot.novagame

import org.junit.Assert.*
import org.junit.Test

class GalleryImportPolicyTest {
    @Test fun onlyPersistedDocumentsSurviveRestart() {
        assertTrue(GalleryImportPolicy.shouldSave(
            "content://com.android.providers.media.documents/document/image%3A123", true))
        assertFalse(GalleryImportPolicy.shouldSave(
            "content://com.android.providers.media.documents/document/image%3A123", false))
        assertFalse(GalleryImportPolicy.shouldSave("file:///sdcard/photo.jpg", true))
        assertFalse(GalleryImportPolicy.shouldSave("https://example.org/photo.jpg", true))
        assertFalse(GalleryImportPolicy.shouldSave("content://", true))
        // Malformed content authorities used to survive the naive prefix
        // check and create permanently broken tiles after app restart.
        for (bad in listOf(
            "content:////document/image%3A123",
            "content://@provider/document/123",
            "content://provider:443/document/123",
            "content://provider?token=secret",
            "content://provider/document/123#fragment",
            "content://provider with space/document/123"
        )) assertFalse(bad, GalleryImportPolicy.shouldSave(bad, true))
        assertFalse(GalleryImportPolicy.shouldSave(
            "content://provider/photo" + "\n" + "another", true))
        assertFalse(GalleryImportPolicy.shouldSave(
            "content://provider/" + "x".repeat(2049), true))
    }
}
