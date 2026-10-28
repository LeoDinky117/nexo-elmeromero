package com.nexo.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore("settings")

class SessionManager(context: Context) {
    val applicationContext: Context = context.applicationContext
    companion object {
        val USER_ID = intPreferencesKey("user_id")
        val USER_POINTS = intPreferencesKey("user_points")
        val USER_STREAK = intPreferencesKey("user_streak")
        val USER_THEME_PALETTE = stringPreferencesKey("user_theme_palette")
        val USER_PROFILE_ICON = stringPreferencesKey("user_profile_icon")
    }

    // Leemos el ID. Si no existe, devolvemos 0.
    val userIdFlow: Flow<Int> = applicationContext.dataStore.data
        .map { preferences ->
            preferences[USER_ID] ?: 0
        }
    val puntosFlow: Flow<Int> = applicationContext.dataStore.data.map { it[USER_POINTS] ?: 0 }
    val rachaFlow: Flow<Int> = applicationContext.dataStore.data.map { it[USER_STREAK] ?: 0 }
    val temaPaletteFlow: Flow<String> = applicationContext.dataStore.data.map { it[USER_THEME_PALETTE] ?: "#C837AB|#0B1019|#161C28|#FF508E" }
    val profileIconFlow: Flow<String> = applicationContext.dataStore.data.map { it[USER_PROFILE_ICON] ?: "👤" }

    // Guardamos el ID que obtuviste del Login exitoso
    suspend fun saveUserId(id: Int) {
        applicationContext.dataStore.edit { settings ->
            settings[USER_ID] = id
        }
    }
    suspend fun saveGamification(puntos: Int, racha: Int) {
        applicationContext.dataStore.edit { settings ->
            settings[USER_POINTS] = puntos
            settings[USER_STREAK] = racha
        }
    }
    suspend fun saveStreak(racha: Int) {
        applicationContext.dataStore.edit { settings -> settings[USER_STREAK] = racha }
    }
    suspend fun saveRewardStyle(palette: String, icon: String) {
        applicationContext.dataStore.edit { settings ->
            settings[USER_THEME_PALETTE] = palette
            settings[USER_PROFILE_ICON] = icon
        }
    }
    suspend fun getUserId(): Int{
        return applicationContext.dataStore.data.map { it[USER_ID] ?: 0 }.first()
    }
}
