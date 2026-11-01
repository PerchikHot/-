package com.perchikhot.novagame

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

/** Decode a bounded preview instead of allocating a full-resolution photo.
 *  A 12 MP image can otherwise exhaust the memory-game grid on Android. */
internal object ImageSampling {
    // Even a caller requesting original resolution must not allocate an
    // unbounded bitmap for a memory-game tile on low-RAM Android devices.
    fun previewDimension(requested: Int): Int = requested.coerceIn(0, 2048)

    fun sampleSize(width: Int, height: Int, maxDimension: Int): Int {
        if (width <= 0 || height <= 0 || maxDimension <= 0) return 1
        var sample = 1
        while (maxOf(width / sample, height / sample) > maxDimension &&
            sample <= 1 shl 28) {
            sample *= 2
        }
        return sample
    }

    fun decodePreview(file: File, maxDimension: Int): Bitmap? {
        val boundedDimension = previewDimension(maxDimension)
        if (boundedDimension <= 0) return null
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, boundedDimension)
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
        } catch (_: Exception) {
            null
        } catch (_: OutOfMemoryError) {
            // Bad or enormous image data must not crash the game grid.
            null
        }
    }
}
