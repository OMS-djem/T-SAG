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
}
