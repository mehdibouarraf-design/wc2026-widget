package com.mavis.wc2026.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("wc2026_prefs")

class WcPrefs(private val context: Context) {

    companion object {
        val KEY_GROUP = stringPreferencesKey("group")        // "A" .. "L"
        val KEY_FAV_TEAM = stringPreferencesKey("fav_team")  // team id
    }

    suspend fun getGroup(): String = context.dataStore.data
        .map { it[KEY_GROUP] ?: "A" }.first()

    suspend fun setGroup(g: String) {
        context.dataStore.edit { it[KEY_GROUP] = g }
    }

    suspend fun getFavTeam(): String? = context.dataStore.data
        .map { it[KEY_FAV_TEAM] }.first()

    suspend fun setFavTeam(id: String?) {
        context.dataStore.edit {
            if (id == null) it.remove(KEY_FAV_TEAM) else it[KEY_FAV_TEAM] = id
        }
    }
}
