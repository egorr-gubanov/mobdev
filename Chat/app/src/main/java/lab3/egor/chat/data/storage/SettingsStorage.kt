package lab3.egor.chat.data.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsStorage(private val context: Context) {
    private val nameKey = stringPreferencesKey("name")
    private val passwordKey = stringPreferencesKey("password")
    private val tokenKey = stringPreferencesKey("token")

    val name: Flow<String?> = context.dataStore.data.map { it[nameKey] }
    val password: Flow<String?> = context.dataStore.data.map { it[passwordKey] }
    val token: Flow<String?> = context.dataStore.data.map { it[tokenKey] }

    suspend fun saveCredentials(name: String, password: String) {
        context.dataStore.edit {
            it[nameKey] = name
            it[passwordKey] = password
        }
    }

    suspend fun saveToken(token: String) {
        context.dataStore.edit {
            it[tokenKey] = token
        }
    }

    suspend fun clear() {
        context.dataStore.edit {
            it.remove(tokenKey)
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit {
            it.clear()
        }
    }
}
