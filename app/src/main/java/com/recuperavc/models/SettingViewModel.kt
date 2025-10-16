package com.recuperavc.models
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recuperavc.data.db.UserPreferences
import kotlinx.coroutines.launch

class SettingsViewModel(private val userPreferences: UserPreferences) : ViewModel() {
    val darkModeFlow = userPreferences.darkModeFlow
    val contrastFlow = userPreferences.contrastFlow
    val sizeTextFlow = userPreferences.sizeTextFlow

    fun setDarkMode(value: Boolean) {
        viewModelScope.launch { userPreferences.setDarkMode(value) }
    }

    fun setContrastText(value: Boolean) {
        viewModelScope.launch { userPreferences.setContrastText(value) }
    }

    fun setSizeText(value: Float) {
        viewModelScope.launch { userPreferences.setSizeText(value) }
    }
}
