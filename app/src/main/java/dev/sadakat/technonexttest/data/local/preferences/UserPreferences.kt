package dev.sadakat.technonexttest.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferences @Inject constructor(
    private val context: Context
) {
    companion object {
        private val LOGGED_IN_USER_EMAIL = stringPreferencesKey("logged_in_user_email")
    }

    suspend fun setLoggedInUser(email: String) {
        context.dataStore.edit { preferences ->
            preferences[LOGGED_IN_USER_EMAIL] = email
        }
    }

    fun getLoggedInUser(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[LOGGED_IN_USER_EMAIL]
        }
    }

    suspend fun getLoggedInUserSync(): String? {
        return context.dataStore.data.first()[LOGGED_IN_USER_EMAIL]
    }

    suspend fun clearLoggedInUser() {
        context.dataStore.edit { preferences ->
            preferences.remove(LOGGED_IN_USER_EMAIL)
        }
    }
}