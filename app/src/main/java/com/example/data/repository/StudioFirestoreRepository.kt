package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CreditTransaction
import com.example.data.model.StudioProject
import com.example.data.model.UserProfile
import com.example.data.model.VideoJob
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class StudioFirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    suspend fun ensureUserProfile(): Result<UserProfile> {
        val currentUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("User must be signed in."))
        val uid = currentUser.uid
        val docRef = db.collection("users").document(uid)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                val existing = snap.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: UserProfile(id = uid, userId = uid)
                Result.success(existing)
            } else {
                val displayName = currentUser.displayName?.takeIf { it.isNotBlank() }?.take(80)
                    ?: "Studio Creator"
                val email = currentUser.email?.takeIf { it.isNotBlank() }?.take(160)
                    ?: "$uid@creator.local"
                val photoUrl = currentUser.photoUrl?.toString()?.take(500)
                val payload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "displayName" to displayName,
                    "email" to email,
                    "credits" to 100L,
                    "plan" to "starter",
                    "role" to "user",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (photoUrl != null) {
                    payload["photoUrl"] = photoUrl
                }
                docRef.set(payload).await()
                val created = docRef.get().await().toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: UserProfile(
                    id = uid,
                    userId = uid,
                    displayName = displayName,
                    email = email,
                    photoUrl = photoUrl,
                    credits = 100L,
                    plan = "starter",
                    role = "user"
                )
                Result.success(created)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else {
                    null
                }
            }
            .catch { error ->
                logIfAuthenticated(error, OperationType.GET, path, userId)
                throw error
            }
    }

    fun observeProjects(userId: String): Flow<List<StudioProject>> {
        val path = "users/$userId/projects"
        return db.collection("users").document(userId).collection("projects")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    StudioProject::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                logIfAuthenticated(error, OperationType.LIST, path, userId)
                throw error
            }
    }

    fun observeVideoJobs(userId: String): Flow<List<VideoJob>> {
        val path = "users/$userId/videoJobs"
        return db.collection("users").document(userId).collection("videoJobs")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    VideoJob::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                logIfAuthenticated(error, OperationType.LIST, path, userId)
                throw error
            }
    }

    fun observeCreditTransactions(userId: String): Flow<List<CreditTransaction>> {
        val path = "users/$userId/creditTransactions"
        return db.collection("users").document(userId).collection("creditTransactions")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CreditTransaction::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                logIfAuthenticated(error, OperationType.LIST, path, userId)
                throw error
            }
    }

    fun observeAllUsersForAdmin(): Flow<List<UserProfile>> {
        val path = "users"
        return db.collection("users")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                logIfAuthenticated(error, OperationType.LIST, path, auth.currentUser?.uid)
                throw error
            }
    }

    suspend fun createProject(
        title: String,
        description: String,
        characterSeed: String,
        defaultStyle: String,
        defaultAspectRatio: String
    ): Result<String> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("projects").document()
        val payload = mapOf(
            "userId" to uid,
            "title" to title.trim().take(100),
            "description" to description.trim().take(500),
            "characterSeed" to characterSeed.trim().take(1000),
            "defaultStyle" to defaultStyle,
            "defaultAspectRatio" to defaultAspectRatio,
            "videoCount" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun updateProject(project: StudioProject): Result<Unit> {
        val uid = requireUserId()
        require(project.id.isNotBlank()) { "Project ID is required" }
        val docRef = db.collection("users").document(uid).collection("projects").document(project.id)
        return try {
            docRef.update(
                mapOf(
                    "title" to project.title.trim().take(100),
                    "description" to project.description.trim().take(500),
                    "characterSeed" to project.characterSeed.trim().take(1000),
                    "defaultStyle" to project.defaultStyle,
                    "defaultAspectRatio" to project.defaultAspectRatio,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun deleteProject(projectId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("projects").document(projectId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun createVideoJob(job: VideoJob, deductCredits: Boolean): Result<String> {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        val jobRef = userRef.collection("videoJobs").document()
        val txRef = userRef.collection("creditTransactions").document()

        return try {
            db.runTransaction { transaction ->
                val userSnap = transaction.get(userRef)
                val currentCredits = userSnap.getLong("credits") ?: 0L
                val cost = if (deductCredits) job.creditsCost else 0L
                if (deductCredits && currentCredits < cost) {
                    throw IllegalStateException("Insufficient credits ($currentCredits available, $cost required).")
                }

                val jobPayload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "modelProvider" to job.modelProvider,
                    "generationMode" to job.generationMode,
                    "prompt" to job.prompt.trim().take(2000),
                    "negativePrompt" to job.negativePrompt.trim().take(1000),
                    "language" to job.language,
                    "durationSeconds" to job.durationSeconds,
                    "aspectRatio" to job.aspectRatio,
                    "resolution" to job.resolution,
                    "fps" to job.fps,
                    "cameraMovement" to job.cameraMovement,
                    "motionIntensity" to job.motionIntensity,
                    "visualStyle" to job.visualStyle,
                    "audioTrack" to job.audioTrack,
                    "status" to job.status,
                    "progress" to job.progress,
                    "creditsCost" to cost,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                job.projectId?.takeIf { it.isNotBlank() }?.let { jobPayload["projectId"] = it }
                job.remoteJobId?.takeIf { it.isNotBlank() }?.let { jobPayload["remoteJobId"] = it }
                job.videoUrl?.takeIf { it.isNotBlank() }?.let { jobPayload["videoUrl"] = it }
                job.thumbnailUrl?.takeIf { it.isNotBlank() }?.let { jobPayload["thumbnailUrl"] = it }
                job.errorMessage?.takeIf { it.isNotBlank() }?.let { jobPayload["errorMessage"] = it }

                transaction.set(jobRef, jobPayload)

                if (deductCredits && cost > 0L) {
                    transaction.update(
                        userRef,
                        mapOf(
                            "credits" to (currentCredits - cost),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    transaction.set(
                        txRef,
                        mapOf(
                            "userId" to uid,
                            "amount" to -cost,
                            "reason" to "${job.modelProvider} (${job.generationMode})",
                            "jobId" to jobRef.id,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(jobRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, jobRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun updateVideoJobStatus(
        jobId: String,
        status: String,
        progress: Long,
        remoteJobId: String? = null,
        videoUrl: String? = null,
        thumbnailUrl: String? = null,
        errorMessage: String? = null
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("videoJobs").document(jobId)
        val updates = mutableMapOf<String, Any>(
            "status" to status,
            "progress" to progress.coerceIn(0L, 100L),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        remoteJobId?.takeIf { it.isNotBlank() }?.let { updates["remoteJobId"] = it }
        videoUrl?.takeIf { it.isNotBlank() }?.let { updates["videoUrl"] = it }
        thumbnailUrl?.takeIf { it.isNotBlank() }?.let { updates["thumbnailUrl"] = it }
        errorMessage?.takeIf { it.isNotBlank() }?.let { updates["errorMessage"] = it.take(1000) }

        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun deleteVideoJob(jobId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("videoJobs").document(jobId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun adminUpdateUserCredits(
        targetUserId: String,
        newCredits: Long,
        newPlan: String,
        reason: String
    ): Result<Unit> {
        requireUserId()
        val userRef = db.collection("users").document(targetUserId)
        val txRef = userRef.collection("creditTransactions").document()
        return try {
            db.runTransaction { transaction ->
                val snap = transaction.get(userRef)
                val oldCredits = snap.getLong("credits") ?: 0L
                val delta = newCredits - oldCredits
                transaction.update(
                    userRef,
                    mapOf(
                        "credits" to newCredits.coerceAtLeast(0L),
                        "plan" to newPlan,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.set(
                    txRef,
                    mapOf(
                        "userId" to targetUserId,
                        "amount" to delta,
                        "reason" to reason.take(200),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path, auth)
            Result.failure(e)
        }
    }

    private fun logIfAuthenticated(
        error: Throwable,
        operationType: OperationType,
        path: String,
        expectedUid: String?
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path, auth)
        }
    }
}
