package com.nexo.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore("settings")

class SessionManager(private val context: Context) {
    companion object {
        val USER_ID = intPreferencesKey("user_id")
    }

    // Leemos el ID. Si no existe, devolvemos 0.
    val userIdFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[USER_ID] ?: 0
        }

    // Guardamos el ID que obtuviste del Login exitoso
    suspend fun saveUserId(id: Int) {
        context.dataStore.edit { settings ->
            settings[USER_ID] = id
        }
    }
    suspend fun getUserId(): Int{
        return context.dataStore.data.map { it[USER_ID] ?: 0 }.first()
    }
}