package com.perchikhot.novagame

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageSamplingTest {
    @Test fun previewsUsePowerOfTwoSamplesToLimitMemory() {
        assertEquals(1, ImageSampling.sampleSize(320, 240, 512))
        assertEquals(2, ImageSampling.sampleSize(1000, 1000, 512))
        assertEquals(8, ImageSampling.sampleSize(4000, 3000, 512))
        assertEquals(32, ImageSampling.sampleSize(12000, 8000, 512))
    }

    @Test fun malformedDimensionsDoNotLoop() {
        assertEquals(1, ImageSampling.sampleSize(0, 8000, 512))
        assertEquals(1, ImageSampling.sampleSize(8000, 8000, 0))
    }
}
