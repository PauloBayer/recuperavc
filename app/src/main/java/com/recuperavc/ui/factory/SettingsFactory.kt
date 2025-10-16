package com.recuperavc.ui.factory

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.recuperavc.data.db.UserPreferences
import com.recuperavc.models.SettingsViewModel

class SettingsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val userPreferences = UserPreferences(context)
        return SettingsViewModel(userPreferences) as T
    }
}
