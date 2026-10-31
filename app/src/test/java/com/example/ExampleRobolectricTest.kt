package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.LauncherRepository
import com.example.util.MathEvaluator
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Flux Launcher", appName)
  }

  @Test
  fun `math evaluation calculates expressions`() {
    assertEquals("4", MathEvaluator.evaluate("2+2"))
    assertEquals("42", MathEvaluator.evaluate("6 * 7"))
    assertEquals("25", MathEvaluator.evaluate("50 / 2"))
  }

  @Test
  fun `launcher repository initializes default apps`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = LauncherRepository(context)
    assertNotNull(repo.allApps.value)
    assertNotNull(repo.favorites.value)
    assertNotNull(repo.carouselWidgets.value)
    assertTrue("Carousel widgets should be initialized", repo.carouselWidgets.value.isNotEmpty())
  }
}
