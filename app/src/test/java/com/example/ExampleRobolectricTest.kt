package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.remote.CompanionScriptProvider
import com.example.model.ConnectionProfile
import com.example.model.ShellType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    assertEquals("Remote CMD", appName)
  }

  @Test
  fun `test companion server scripts generation`() {
    val pyScript = CompanionScriptProvider.getPythonServerScript(8080, "1234")
    assertTrue(pyScript.contains("PORT = 8080"))
    assertTrue(pyScript.contains("AUTH_PIN = \"1234\""))
    assertTrue(pyScript.contains("cmd.exe"))

    val psScript = CompanionScriptProvider.getPowerShellScript(8888)
    assertTrue(psScript.contains("8888"))
    assertTrue(psScript.contains("HttpListener"))

    val jsScript = CompanionScriptProvider.getNodeJsScript(9000)
    assertTrue(jsScript.contains("9000"))
  }

  @Test
  fun `test connection profile creation`() {
    val profile = ConnectionProfile(
      name = "Laptop Dell",
      host = "192.168.1.50",
      port = 8080,
      shellType = ShellType.CMD,
      isDefault = true
    )
    assertEquals("Laptop Dell", profile.name)
    assertEquals("192.168.1.50", profile.host)
    assertEquals(8080, profile.port)
    assertTrue(profile.isDefault)
  }
}
