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

  @Test
  fun testWatermarkHelperCreatesValidStampedPhoto() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val testBitmap = android.graphics.Bitmap.createBitmap(800, 600, android.graphics.Bitmap.Config.ARGB_8888)
    val gps = GpsData(latitude = 36.7538, longitude = 3.0588, altitude = 50.0, accuracy = 4.2f)
    val gsm = GsmData(operatorName = "Djezzy", networkType = "LTE 4G", cellId = "48192", lacTac = "1042")

    val file = com.example.camera.WatermarkHelper.createStampedPhoto(
      context = context,
      sourceBitmap = testBitmap,
      siteId = "MOB-DZ-1044",
      operator = "Djezzy",
      status = SiteAccessStatus.ACCORDED,
      gps = gps,
      gsm = gsm,
      tag = "Site Entrance"
    )

    assertTrue(file.exists())
    assertTrue(file.length() > 0)
    assertTrue(file.name.startsWith("IMG_SITE_"))
    assertTrue(file.name.endsWith(".jpg"))
  }

  @Test
  fun testUserCrudAndRoles() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getDatabase(context)
    val userDao = db.userDao()

    val testUser = com.example.data.model.UserEntity(
      name = "NOC Supervisor",
      email = "supervisor@mobiserveholding.com",
      phone = "+213 550 99 88 77",
      role = "SUPERVISION",
      department = "NOC",
      region = "National"
    )

    val id = userDao.insertUser(testUser)
    assertTrue(id > 0)

    val fetched = userDao.getUserById(id)
    assertEquals("NOC Supervisor", fetched?.name)
    assertEquals("SUPERVISION", fetched?.role)
    assertEquals(com.example.data.model.UserRole.SUPERVISION, fetched?.roleEnum)

    // Update
    val updated = fetched!!.copy(role = "OM", department = "Operations & Maintenance")
    userDao.updateUser(updated)
    val fetchedAfterUpdate = userDao.getUserById(id)
    assertEquals("OM", fetchedAfterUpdate?.role)

    // Delete
    userDao.deleteUser(fetchedAfterUpdate!!)
    val fetchedAfterDelete = userDao.getUserById(id)
    assertTrue(fetchedAfterDelete == null)
  }

  @Test
  fun testGpsDataFormattingAndMapsUrl() {
    val gps = GpsData(
      latitude = 36.753820,
      longitude = 3.058810,
      altitude = 125.0,
      accuracy = 3.8f,
      address = "Bab Ezzouar, Algiers"
    )

    assertTrue(gps.isValid)
    assertEquals("36.753820, 3.058810", gps.coordinatesString)
    assertTrue(gps.formattedLatitude.contains("36.753820° N"))
    assertTrue(gps.formattedLongitude.contains("3.058810° E"))
    assertEquals("https://maps.google.com/?q=36.75382,3.05881", gps.mapsUrl)
    assertTrue(gps.geoUriString.startsWith("geo:36.75382,3.05881"))
  }

  @Test
  fun testGpsManagerLocationAcquisition() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val gpsManager = com.example.location.GpsManager(context)

    val gpsData = gpsManager.getCurrentLocation()
    assertTrue(gpsData.isValid)
    assertTrue(gpsData.latitude != 0.0)
    assertTrue(gpsData.longitude != 0.0)
    assertTrue(gpsData.address.isNotBlank())
  }

  @Test
  fun testAuditLogIncludesGpsCoordinates() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getDatabase(context)
    val dao = db.siteReportDao()

    val testReport = SiteReportEntity(
      siteId = "DZ-ALG-0492",
      siteName = "Bab Ezzouar Rooftop Hub",
      operator = "Mobilis",
      status = "ACCORDED",
      refusalReason = null,
      technicianName = "Karim Field",
      technicianPhone = "+213 550 11 22 33",
      recipientEmail = "reports@mobiserveholding.com",
      notes = "Cabinet inspected and secured",
      latitude = 36.718290,
      longitude = 3.185200,
      altitude = 64.5,
      accuracy = 4.2f,
      address = "Bab Ezzouar, Algiers Province",
      gsmOperator = "Mobilis DZ",
      networkType = "LTE 4G",
      mcc = "603",
      mnc = "01",
      lac = "1204",
      cellId = "58491",
      signalDbm = -72,
      photoPaths = ""
    )

    val id = dao.insertReport(testReport)
    assertTrue(id > 0)

    val retrieved = dao.getReportById(id)
    assertTrue(retrieved != null)
    assertEquals("DZ-ALG-0492", retrieved?.siteId)
    assertEquals(36.718290, retrieved!!.latitude, 0.000001)
    assertEquals(3.185200, retrieved.longitude, 0.000001)
    assertEquals(64.5, retrieved.altitude, 0.1)
    assertEquals(4.2f, retrieved.accuracy, 0.1f)
    assertEquals("Bab Ezzouar, Algiers Province", retrieved.address)
  }

  @Test
  fun `test proximity helper distance and bearing calculations`() {
    // Technician at Hydra Center (36.753820, 3.058810)
    // Nearby site 200m away (36.754800, 3.056900)
    val proximityNear = com.example.location.ProximityHelper.calculateProximity(
      fromLat = 36.753820,
      fromLng = 3.058810,
      toLat = 36.754800,
      toLng = 3.056900
    )
    assertTrue(proximityNear.distanceMeters > 0f)
    assertTrue(proximityNear.distanceMeters < 500f)
    assertEquals(com.example.location.ProximityLevel.NEARBY, proximityNear.proximityLevel)
    assertTrue(proximityNear.formattedDistance.endsWith("m"))

    // Far site in Oran (~350 km)
    val proximityFar = com.example.location.ProximityHelper.calculateProximity(
      fromLat = 36.753820,
      fromLng = 3.058810,
      toLat = 35.631100,
      toLng = -0.621000
    )
    assertTrue(proximityFar.distanceMeters > 300000f)
    assertEquals(com.example.location.ProximityLevel.REGIONAL, proximityFar.proximityLevel)
    assertTrue(proximityFar.formattedDistance.contains("km"))
  }

  @Test
  fun `test telecom site dao insert and audit status update`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getDatabase(context)
    val siteDao = db.telecomSiteDao()

    val site = com.example.data.model.TelecomSiteEntity(
      siteId = "MOB-TEST-9901",
      siteName = "Test Rooftop Mast",
      operator = "Mobiserve",
      latitude = 36.750000,
      longitude = 3.050000,
      accessStatus = "PENDING"
    )

    val id = siteDao.insertSite(site)
    assertTrue(id > 0)

    val queried = siteDao.getSiteById("MOB-TEST-9901")
    assertTrue(queried != null)
    assertEquals("PENDING", queried?.accessStatus)

    // Update status to REFUSED
    siteDao.updateSiteAuditStatus("MOB-TEST-9901", "REFUSED", "Guard permit expired")
    val updated = siteDao.getSiteById("MOB-TEST-9901")
    assertEquals("REFUSED", updated?.accessStatus)
    assertEquals("Guard permit expired", updated?.refusalReason)
  }
}
