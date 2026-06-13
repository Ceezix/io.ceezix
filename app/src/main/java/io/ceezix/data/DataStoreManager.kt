package io.ceezix.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ceezix_preferences")

class DataStoreManager(private val context: Context) {

    companion object {
        val TOTAL_SOLVES = intPreferencesKey("total_solves")
        val CURRENT_STREAK = intPreferencesKey("current_streak")
        val ACQUIRED_XP = intPreferencesKey("acquired_xp")
        val SAVED_CODES = stringSetPreferencesKey("saved_codes")
        val SOLVED_CHALLENGE_IDS = stringSetPreferencesKey("solved_challenge_ids")

        // --- GIT VERSION CONTROL PREFERENCES ---
        val GIT_INITIALIZED = androidx.datastore.preferences.core.booleanPreferencesKey("git_initialized")
        val GIT_REMOTE_URL = stringPreferencesKey("git_remote_url")
        val GIT_STAGED_PATHS = stringSetPreferencesKey("git_staged_paths")
        val GIT_PUSHED_COMMITS = stringSetPreferencesKey("git_pushed_commits")
    }

    val totalSolves: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[TOTAL_SOLVES] ?: 0
    }

    val currentStreak: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[CURRENT_STREAK] ?: 0
    }

    val acquiredXp: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[ACQUIRED_XP] ?: 0
    }

    val savedCodes: Flow<List<String>> = context.dataStore.data.map { preferences ->
        preferences[SAVED_CODES]?.toList() ?: emptyList()
    }

    val solvedChallengeIds: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[SOLVED_CHALLENGE_IDS]?.toSet() ?: emptySet()
    }

    // --- GIT FLOW EXPOSURES ---
    val gitInitialized: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[GIT_INITIALIZED] ?: false
    }

    val gitRemoteUrl: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[GIT_REMOTE_URL] ?: ""
    }

    val gitStagedPaths: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[GIT_STAGED_PATHS]?.toSet() ?: emptySet()
    }

    val gitPushedCommits: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[GIT_PUSHED_COMMITS]?.toSet() ?: emptySet()
    }

    suspend fun setGitInitialized(initialized: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[GIT_INITIALIZED] = initialized
        }
    }

    suspend fun setGitRemoteUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[GIT_REMOTE_URL] = url
        }
    }

    suspend fun setGitStagedPaths(paths: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[GIT_STAGED_PATHS] = paths
        }
    }

    suspend fun addGitPushedCommit(hash: String) {
        context.dataStore.edit { preferences ->
            val set = preferences[GIT_PUSHED_COMMITS]?.toMutableSet() ?: mutableSetOf()
            set.add(hash)
            preferences[GIT_PUSHED_COMMITS] = set
        }
    }

    suspend fun clearGitPushedCommits() {
        context.dataStore.edit { preferences ->
            preferences.remove(GIT_PUSHED_COMMITS)
        }
    }

    suspend fun incrementSolves(xpReward: Int) {
        context.dataStore.edit { preferences ->
            val currentSolves = preferences[TOTAL_SOLVES] ?: 0
            val currentXp = preferences[ACQUIRED_XP] ?: 0
            val currentStr = preferences[CURRENT_STREAK] ?: 0
            preferences[TOTAL_SOLVES] = currentSolves + 1
            preferences[ACQUIRED_XP] = currentXp + xpReward
            preferences[CURRENT_STREAK] = currentStr + 1
        }
    }

    suspend fun resetStreak() {
        context.dataStore.edit { preferences ->
            preferences[CURRENT_STREAK] = 0
        }
    }

    suspend fun markChallengeSolved(challengeId: String, xpReward: Int) {
        context.dataStore.edit { preferences ->
            val solved = preferences[SOLVED_CHALLENGE_IDS]?.toMutableSet() ?: mutableSetOf()
            if (!solved.contains(challengeId)) {
                solved.add(challengeId)
                preferences[SOLVED_CHALLENGE_IDS] = solved
                
                val currentSolves = preferences[TOTAL_SOLVES] ?: 0
                val currentXp = preferences[ACQUIRED_XP] ?: 0
                val currentStr = preferences[CURRENT_STREAK] ?: 0
                preferences[TOTAL_SOLVES] = currentSolves + 1
                preferences[ACQUIRED_XP] = currentXp + xpReward
                preferences[CURRENT_STREAK] = currentStr + 1
            }
        }
    }

    suspend fun saveCode(rawText: String, decryptedText: String, shiftUsed: Int) {
        context.dataStore.edit { preferences ->
            val codes = preferences[SAVED_CODES]?.toMutableSet() ?: mutableSetOf()
            // Format as a simple log entry
            val entry = "$shiftUsed|:|:$rawText|:|:$decryptedText"
            codes.add(entry)
            preferences[SAVED_CODES] = codes
        }
    }

    suspend fun deleteCode(entry: String) {
        context.dataStore.edit { preferences ->
            val codes = preferences[SAVED_CODES]?.toMutableSet() ?: mutableSetOf()
            codes.remove(entry)
            preferences[SAVED_CODES] = codes
        }
    }

    suspend fun clearAllData() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
