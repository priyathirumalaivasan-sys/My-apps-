package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.VideoJob
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class StudioRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureUserProfile_andCreateProject_succeedsForOwner() = runBlocking {
        val uid = signInTestUser("alice_${UUID.randomUUID()}@test.com")
        val repository = StudioFirestoreRepository(firestore, auth)

        val profileResult = withTimeout(TIMEOUT_MS) { repository.ensureUserProfile() }
        assertTrue(profileResult.isSuccess)
        assertEquals(100L, profileResult.getOrThrow().credits)

        val projectResult = withTimeout(TIMEOUT_MS) {
            repository.createProject(
                title = "Chennai Cyberpunk",
                description = "Futuristic short film",
                characterSeed = "Vasan, jacket with yellow neon trim",
                defaultStyle = "Cinematic",
                defaultAspectRatio = "16:9"
            )
        }
        assertTrue(projectResult.isSuccess)
        val projectId = projectResult.getOrThrow()

        val observedProjects = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeProjects(uid).first { list -> list.any { it.id == projectId } }
        }
        assertTrue(observedProjects.any { it.id == projectId })
    }

    @Test
    fun createVideoJob_deductsCredits_andUpdatesStatus() = runBlocking {
        val uid = signInTestUser("creator_${UUID.randomUUID()}@test.com")
        val repository = StudioFirestoreRepository(firestore, auth)
        withTimeout(TIMEOUT_MS) { repository.ensureUserProfile().getOrThrow() }

        val jobId = withTimeout(TIMEOUT_MS) {
            repository.createVideoJob(
                job = VideoJob(
                    modelProvider = "Google Veo",
                    generationMode = "Text to Video",
                    prompt = "Golden drone flight over Marina Beach",
                    negativePrompt = "blurry, text",
                    creditsCost = 20L
                ),
                deductCredits = true
            ).getOrThrow()
        }

        val profile = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserProfile(uid).first { it?.credits == 80L }
        }
        assertEquals(80L, profile?.credits)

        val updateRes = withTimeout(TIMEOUT_MS) {
            repository.updateVideoJobStatus(
                jobId = jobId,
                status = "completed",
                progress = 100L,
                videoUrl = "https://example.com/sample.mp4"
            )
        }
        assertTrue(updateRes.isSuccess)
    }

    @Test
    fun observeProjects_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = StudioFirestoreRepository(firestore, auth)

        var caught: Exception? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeProjects("unauth_user_123").first()
            }
        } catch (e: Exception) {
            caught = e
        }
        assertNotNull(caught)
        val firestoreEx = caught?.unwrapFirestoreException()
        assertNotNull("Expected nested FirebaseFirestoreException, got $caught", firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun observeProjects_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser("alice_iso_${UUID.randomUUID()}@test.com")
        val aliceRepo = StudioFirestoreRepository(firestore, auth)
        withTimeout(TIMEOUT_MS) { aliceRepo.ensureUserProfile().getOrThrow() }

        signInTestUser("bob_iso_${UUID.randomUUID()}@test.com")
        val bobRepo = StudioFirestoreRepository(firestore, auth)

        var caught: Exception? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeProjects(aliceUid).first()
            }
        } catch (e: Exception) {
            caught = e
        }
        assertNotNull(caught)
        val firestoreEx = caught?.unwrapFirestoreException()
        assertNotNull("Expected nested FirebaseFirestoreException, got $caught", firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    private companion object {
        const val TIMEOUT_MS = 8000L
        const val FLOW_TIMEOUT_MS = 5000L
    }
}
