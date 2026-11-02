package com.example

import com.example.security.SecurityHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testSecurityHelperCredentialDecoding() {
    val decodedPassword = SecurityHelper.getDefaultSenderPassword()
    assertEquals("Djma@1976++", decodedPassword)
  }

  @Test
  fun testAdminPinVerification() {
    assertTrue(SecurityHelper.verifyAdminPin("1976"))
    assertTrue(SecurityHelper.verifyAdminPin("admin1976"))
    assertFalse(SecurityHelper.verifyAdminPin("wrong_pin"))
  }

  @Test
  fun testGoogleMapsStaticUrlGeneration() {
    val gps = com.example.data.model.GpsData(latitude = 36.753820, longitude = 3.058810)
    val url = gps.getGoogleMapsStaticUrl(zoom = 16, width = 640, height = 340, mapType = "hybrid")
    assertTrue(url.startsWith("https://maps.googleapis.com/maps/api/staticmap?"))
    assertTrue(url.contains("center=36.753820,3.058810"))
    assertTrue(url.contains("zoom=16"))
    assertTrue(url.contains("size=640x340"))
    assertTrue(url.contains("maptype=hybrid"))
    assertTrue(url.contains("markers=color:red%7Clabel:S%7C36.753820,3.058810"))
  }
}
