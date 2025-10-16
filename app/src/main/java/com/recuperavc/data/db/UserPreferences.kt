package com.recuperavc.data.db

import android.R
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "configuracoes")
class UserPreferences (private val context: Context){
    //chaves de acessos das configurações
    companion object {
        val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        val CONTRAST_KEY = booleanPreferencesKey("contrast")
        val SIZE_TEXT_KEY = floatPreferencesKey("size_text")
    }

    //metodos de salvamento
    suspend fun setDarkMode(enable: Boolean){
        context.dataStore.edit { prefs -> prefs[DARK_MODE_KEY] = enable }
    }

    suspend fun setContrastText(enable: Boolean){
        context.dataStore.edit{ prefs -> prefs[CONTRAST_KEY] = enable}
    }

    suspend fun setSizeText(size : Float){
        context.dataStore.edit{prefs -> prefs[SIZE_TEXT_KEY] = size}
    }

    //metodos de busca das configs
    val darkModeFlow : Flow<Boolean> =
        context.dataStore.data.map{prefs -> prefs[DARK_MODE_KEY]?:false}

    val contrastFlow :Flow<Boolean> =
        context.dataStore.data.map {prefs -> prefs[CONTRAST_KEY]?:false}

    val sizeTextFlow : Flow<Float> =
        context.dataStore.data.map { prefs -> prefs[SIZE_TEXT_KEY]?:1.0f}

}