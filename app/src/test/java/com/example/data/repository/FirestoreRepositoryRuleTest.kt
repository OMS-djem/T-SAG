package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.FirestoreUser
import com.example.data.model.UserRole
import com.example.data.remote.FirestoreRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirestoreRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveUser_authenticatedUser_succeeds() = runBlocking {
        val email = "tech_${System.currentTimeMillis()}@mobiserve.com"
        val uid = signInTestUser(email)
        val repository = FirestoreRepository(firestore)

        val user = FirestoreUser(
            uid = uid,
            name = "Test Field Technician",
            email = email,
            phone = "+213550112233",
            role = UserRole.TECHNICIAN.code,
            department = "O&M Regional",
            region = "Algiers",
            isActive = true
        )

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveUser(user)
        }

        val observed = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.observeUserProfile(uid).first { it != null }
        }

        assertNotNull(observed)
        assertEquals(uid, observed?.uid)
        assertEquals("Test Field Technician", observed?.name)
        assertEquals(UserRole.TECHNICIAN, observed?.roleEnum)
    }

    @Test
    fun saveSiteReport_authenticatedUser_createsDocument() = runBlocking {
        val email = "auditor_${System.currentTimeMillis()}@mobiserve.com"
        val uid = signInTestUser(email)
        val repository = FirestoreRepository(firestore)

        val reportId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveSiteReport(
                siteId = "ALG042",
                siteName = "Bab Ezzouar Hub",
                status = "ACCORDED",
                technicianName = "Audit Tech",
                technicianPhone = "+213550998877",
                latitude = 36.721,
                longitude = 3.184
            )
        }

        assertTrue(reportId.isNotEmpty())
    }

    @Test(expected = IllegalStateException::class)
    fun requireUserId_unauthenticated_throwsException() {
        auth.signOut()
        val repository = FirestoreRepository(firestore)
        repository.requireUserId()
    }
}
