package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders the real Compose UI to PNG files so the browser preview (which cannot host an
 * Android app) can show what the screens look like.
 *
 * Only writes files when BASE44_SHOT_DIR is set; otherwise it is a no-op test, so a plain
 * `./gradlew test` is unaffected.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class PreviewScreenshotsTest {

  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  private val outDir: File? = System.getenv("BASE44_SHOT_DIR")?.let { File(it) }

  @Test
  fun captureScreens() {
    val dir = outDir ?: return
    dir.mkdirs()

    compose.setContent {
      MyApplicationTheme(darkTheme = true) {
        MainAppRoot()
      }
    }
    settleAndCapture("01_terminal")

    listOf(
      "nav_tab_pc_setup" to "02_pc_setup",
      "nav_tab_profiles" to "03_profiles",
      "nav_tab_snippets" to "04_snippets",
      "nav_tab_history" to "05_history",
    ).forEach { (tag, name) ->
      compose.onNodeWithTag(tag).performClick()
      settleAndCapture(name)
    }
  }

  private fun settleAndCapture(name: String) {
    compose.waitForIdle()
    val view = compose.activity.window.decorView
    if (view.width == 0 || view.height == 0) {
      throw IllegalStateException("decor view has no size: ${view.width}x${view.height}")
    }
    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
    view.draw(Canvas(bitmap))
    File(outDir!!, "$name.png").outputStream().use {
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
    }
  }
}
