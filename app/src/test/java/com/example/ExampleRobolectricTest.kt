package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.GpsData
import com.example.data.model.GsmData
import com.example.data.model.SiteAccessStatus
import com.example.data.model.SiteReportEntity
import com.example.email.EmailHelper
import kotlinx.coroutines.runBlocking
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
    assertEquals("Telecom Site Access", appName)
  }

  @Test
  fun `verify email templates generation`() {
    val gps = GpsData(latitude = 36.7538, longitude = 3.0588, accuracy = 4.5f)
    val gsm = GsmData(operatorName = "Djezzy", networkType = "LTE 4G", cellId = "48192", lacTac = "1042")

    // Test Accorded template
    val subjectAccorded = EmailHelper.generateSubject("MOB-DZ-1044", "Djezzy", SiteAccessStatus.ACCORDED)
    assertTrue(subjectAccorded.contains("[ACCESS ACCORDED]"))
    assertTrue(subjectAccorded.contains("MOB-DZ-1044"))

    val bodyAccorded = EmailHelper.generateBody(
      siteId = "MOB-DZ-1044",
      siteName = "Hydra Relay",
      operator = "Djezzy",
      status = SiteAccessStatus.ACCORDED,
      refusalReason = null,
      notes = "Cabinet inspected",
      technicianName = "M. Djemil",
      technicianPhone = "+213 550 12 34 56",
      gps = gps,
      gsm = gsm,
      photoCount = 2
    )
    assertTrue(bodyAccorded.contains("SITE ACCESS REPORT: ACCORDED"))
    assertTrue(bodyAccorded.contains(EmailHelper.DEFAULT_ZOHO_RECIPIENT))
    assertTrue(bodyAccorded.contains("48192"))
    assertTrue(bodyAccorded.contains("36.7538"))

    // Test Refused template
    val subjectRefused = EmailHelper.generateSubject("MOB-DZ-1044", "Djezzy", SiteAccessStatus.REFUSED)
    assertTrue(subjectRefused.contains("[URGENT: NO ACCESS ALERT]"))

    val bodyRefused = EmailHelper.generateBody(
      siteId = "MOB-DZ-1044",
      siteName = "Hydra Relay",
      operator = "Djezzy",
      status = SiteAccessStatus.REFUSED,
      refusalReason = "Guard refused entry",
      notes = "Keys locked",
      technicianName = "M. Djemil",
      technicianPhone = "+213 550 12 34 56",
      gps = gps,
      gsm = gsm,
      photoCount = 1
    )
    assertTrue(bodyRefused.contains("SITE ACCESS ALERT: NO ACCESS"))
    assertTrue(bodyRefused.contains("Guard refused entry"))
    assertTrue(bodyRefused.contains(EmailHelper.DEFAULT_ZOHO_RECIPIENT))
  }

  @Test
  fun `verify room local offline queue and sync status transition`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getDatabase(context)
    val dao = db.siteReportDao()

    val report = SiteReportEntity(
      siteId = "MOB-TEST-01",
      siteName = "Test Tower",
      operator = "Mobiserve",
      status = "ACCORDED",
      refusalReason = null,
      technicianName = "M. Djemil",
      technicianPhone = "+213550123456",
      recipientEmail = "mdjemil@mobiserveholding.com",
      notes = "Offline test entry",
      latitude = 36.75,
      longitude = 3.05,
      altitude = 50.0,
      accuracy = 5f,
      address = "Hydra",
      gsmOperator = "Djezzy",
      networkType = "LTE",
      mcc = "603",
      mnc = "01",
      lac = "1042",
      cellId = "48192",
      signalDbm = -85,
      photoPaths = "/path/to/test.jpg",
      syncStatus = "PENDING"
    )

    val id = dao.insertReport(report)
    assertTrue(id > 0)

    val pendingList = dao.getPendingReportsList()
    assertTrue(pendingList.any { it.id == id && it.syncStatus == "PENDING" })

    // Simulate auto-sync transition to SYNCED
    dao.updateSyncStatus(id, "SYNCED", null, System.currentTimeMillis())

    val pendingAfterSync = dao.getPendingReportsList()
    assertTrue(pendingAfterSync.none { it.id == id })

    val syncedReport = dao.getReportById(id)
    assertEquals("SYNCED", syncedReport?.syncStatus)
  }
}
