package com.dailychallenge.app.sync

import android.app.Activity
import com.dailychallenge.app.data.CloudPrefsData
import com.dailychallenge.app.data.CloudSaveData
import com.dailychallenge.app.data.CustomGoalJson
import com.dailychallenge.app.data.HistoryDataSource
import com.dailychallenge.app.data.PreferencesDataSource
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.SnapshotsClient
import com.google.android.gms.games.snapshot.SnapshotMetadataChange
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber

private const val SNAPSHOT_NAME = "progress"

class PlayGamesSync(
    private val activity: Activity,
    private val history: HistoryDataSource,
    private val prefs: PreferencesDataSource
) {
    private val gson = Gson()

    private fun signInClient() = PlayGames.getGamesSignInClient(activity)
    private fun snapshotsClient(): SnapshotsClient = PlayGames.getSnapshotsClient(activity)

    suspend fun isSignedIn(): Boolean = withContext(Dispatchers.IO) {
        try {
            val result = signInClient().isAuthenticated().await()
            result?.isAuthenticated == true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun signIn(): Boolean = withContext(Dispatchers.IO) {
        try {
            signInClient().signIn().await()
            isSignedIn()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun saveProgress(): Result<Unit> = saveProgressInternal(retriesLeft = 3)

    private suspend fun saveProgressInternal(retriesLeft: Int): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isSignedIn()) return@withContext Result.failure(SecurityException("Not signed in"))
        try {
            val records = history.getRecords()
            val p = prefs.preferences.first()
            val customGoals = prefs.getCustomGoals().map { (a, b) -> CustomGoalJson(a, b) }
            val pending = prefs.getPendingChallenges()
            val freezeCount = prefs.getFreezeCount()
            val cloudPrefs = CloudPrefsData(
                reminderHour = p.reminderHour,
                reminderMinute = p.reminderMinute,
                selectedCategoryIds = p.selectedCategoryIds,
                onboardingDone = p.onboardingDone,
                theme = p.theme,
                soundEnabled = p.soundEnabled,
                vibrationEnabled = p.vibrationEnabled,
                isPremium = p.isPremium,
                secondReminderHour = p.secondReminderHour,
                secondReminderMinute = p.secondReminderMinute,
                remindWeekdaysOnly = p.remindWeekdaysOnly,
                languageCode = p.languageCode,
                difficulty = p.difficulty,
                customGoals = customGoals,
                pendingChallenges = pending,
                freezeCount = freezeCount
            )
            val data = CloudSaveData(records = records, prefs = cloudPrefs)
            val json = gson.toJson(data)
            val bytes = json.toByteArray(Charsets.UTF_8)

            val client = snapshotsClient()
            val openTask = client.open(SNAPSHOT_NAME, true, SnapshotsClient.RESOLUTION_POLICY_LAST_KNOWN_GOOD)
            val dataOrConflict = openTask.await()
            if (dataOrConflict.isConflict) {
                if (retriesLeft <= 0) return@withContext Result.failure(IllegalStateException("Snapshot conflict not resolved"))
                val conflict = dataOrConflict.conflict
                if (conflict != null) {
                    client.resolveConflict(conflict.conflictId, conflict.snapshot).await()
                }
                return@withContext saveProgressInternal(retriesLeft - 1)
            }
            val snapshot = dataOrConflict.data ?: return@withContext Result.failure(IllegalStateException("No snapshot"))
            snapshot.snapshotContents.writeBytes(bytes)
            val metadataChange = SnapshotMetadataChange.Builder().build()
            client.commitAndClose(snapshot, metadataChange).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "saveProgress failed")
            Result.failure(e)
        }
    }

    suspend fun loadProgress(): Result<Unit> = loadProgressInternal(retriesLeft = 3)

    private suspend fun loadProgressInternal(retriesLeft: Int): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isSignedIn()) return@withContext Result.failure(SecurityException("Not signed in"))
        try {
            val client = snapshotsClient()
            val openTask = client.open(SNAPSHOT_NAME, false, SnapshotsClient.RESOLUTION_POLICY_LAST_KNOWN_GOOD)
            val dataOrConflict = openTask.await()
            if (dataOrConflict.isConflict) {
                if (retriesLeft <= 0) return@withContext Result.failure(IllegalStateException("Snapshot conflict not resolved"))
                val conflict = dataOrConflict.conflict
                if (conflict != null) {
                    client.resolveConflict(conflict.conflictId, conflict.snapshot).await()
                }
                return@withContext loadProgressInternal(retriesLeft - 1)
            }
            val snapshot = dataOrConflict.data ?: return@withContext Result.success(Unit)
            val bytes = snapshot.snapshotContents.readFully()
            client.discardAndClose(snapshot).await()
            val json = String(bytes, Charsets.UTF_8)
            if (json.isBlank()) return@withContext Result.success(Unit)
            val data = try {
                gson.fromJson(json, CloudSaveData::class.java)
            } catch (_: Exception) {
                return@withContext Result.failure(IllegalStateException("Invalid cloud data format"))
            }
            if (data == null) {
                Timber.w("Cloud data parsed as null")
                return@withContext Result.failure(IllegalStateException("Cloud data is null"))
            }
            if (data.records.isEmpty() && !data.prefs.onboardingDone) {
                Timber.w("Cloud data appears empty (no records, onboarding not done)")
                return@withContext Result.failure(IllegalStateException("Cloud data appears empty, refusing to overwrite"))
            }
            history.setRecords(data.records)
            prefs.applyFromCloud(data.prefs)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
