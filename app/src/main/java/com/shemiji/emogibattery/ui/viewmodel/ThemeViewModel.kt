package com.shemiji.emogibattery.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shemiji.emogibattery.ui.theme.AppTheme
import com.shemiji.emogibattery.ui.theme.ThemePreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themePreferences: ThemePreferences,
) : ViewModel() {

    val theme: StateFlow<AppTheme> = themePreferences.themeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = themePreferences.getThemeSync()
        )

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            themePreferences.setTheme(theme)
        }
    }
}
