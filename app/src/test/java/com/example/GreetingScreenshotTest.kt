package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.ClockStyle
import com.example.data.WeatherInfo
import com.example.ui.components.ClockHeader
import com.example.ui.theme.NiagaraTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun launcher_header_screenshot() {
    composeTestRule.setContent {
      NiagaraTheme {
        ClockHeader(
          clockStyle = ClockStyle.MINIMAL_DIGITAL,
          showSeconds = false,
          showDate = true,
          weather = WeatherInfo(
            temperatureCelsius = 22,
            condition = "Sunny",
            conditionIcon = "☀️",
            location = "San Francisco"
          ),
          upcomingEvent = null,
          fontScale = 1.0f,
          isLeftHanded = false,
          onClockClick = {},
          onDateClick = {},
          onWeatherClick = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
