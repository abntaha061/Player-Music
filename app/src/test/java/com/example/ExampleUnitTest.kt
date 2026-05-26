package com.example

import android.graphics.Bitmap
import com.example.util.PaletteHelper
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testPaletteHelper() {
    try {
      val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
      // fill with some color
      bitmap.setPixel(5, 5, 0xFFFF0000.toInt())
      val color = PaletteHelper.extractDominantColor(bitmap)
      println("Extracted color: $color")
    } catch (e: Exception) {
      e.printStackTrace()
      fail("PaletteHelper threw an exception: ${e.message}")
    }
  }
}
