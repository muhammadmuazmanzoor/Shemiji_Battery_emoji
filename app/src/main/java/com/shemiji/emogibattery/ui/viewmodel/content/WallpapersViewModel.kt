package com.shemiji.emogibattery.ui.viewmodel.content

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shemiji.emogibattery.data.model.ContentSourceMode
import com.shemiji.emogibattery.data.model.WallpaperItem
import com.shemiji.emogibattery.data.preferences.SelectionPreferences
import com.shemiji.emogibattery.data.repository.ContentRepository
import com.shemiji.emogibattery.data.repository.ContentResult
import com.shemiji.emogibattery.system.WallpaperApplier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WallpapersUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isApplying: Boolean = false,
    val wallpapers: List<WallpaperItem> = emptyList(),
    val selectedWallpaperId: String? = null,
    val errorMessage: String? = null,
    val message: String? = null,
    val sourceMode: ContentSourceMode = ContentSourceMode.LOCAL_DRAWABLES,
)

@HiltViewModel
class WallpapersViewModel @Inject constructor(
    private val repository: ContentRepository,
    private val preferences: SelectionPreferences,
    private val wallpaperApplier: WallpaperApplier,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WallpapersUiState(sourceMode = repository.sourceMode))
    val uiState: StateFlow<WallpapersUiState> = _uiState.asStateFlow()

    init {
        observeSelections()
        loadWallpapers()
    }

    fun refresh() = loadWallpapers(refreshing = true)

    fun selectWallpaper(id: String) {
        _uiState.update { it.copy(selectedWallpaperId = id, message = null) }
    }

    fun applySelectedWallpaper() {
        val wallpaper = _uiState.value.wallpapers.firstOrNull {
            it.id == _uiState.value.selectedWallpaperId
        } ?: return showMessage("Select a wallpaper first")

        viewModelScope.launch {
            _uiState.update { it.copy(isApplying = true, message = null) }
            wallpaperApplier.apply(wallpaper)
                .onSuccess {
                    preferences.setWallpaper(wallpaper.id)
                    _uiState.update {
                        it.copy(isApplying = false, message = "Wallpaper applied successfully")
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isApplying = false,
                            message = throwable.message ?: "Unable to apply this wallpaper",
                        )
                    }
                }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun loadWallpapers(refreshing: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !refreshing && it.wallpapers.isEmpty(),
                    isRefreshing = refreshing,
                    errorMessage = null,
                )
            }
            when (val result = repository.getWallpapers()) {
                is ContentResult.Success -> _uiState.update { current ->
                    val contentError = if (result.value.isEmpty()) {
                        "No wallpapers are available"
                    } else {
                        null
                    }
                    current.copy(
                        isLoading = false,
                        isRefreshing = false,
                        wallpapers = result.value,
                        selectedWallpaperId = current.selectedWallpaperId
                            ?.takeIf { id -> result.value.any { it.id == id } }
                            ?: result.value.firstOrNull()?.id,
                        errorMessage = contentError,
                    )
                }

                is ContentResult.Error -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = result.message,
                    )
                }
            }
        }
    }

    private fun observeSelections() {
        viewModelScope.launch {
            preferences.selections.collect { selections ->
                _uiState.update {
                    it.copy(selectedWallpaperId = selections.wallpaperId ?: it.selectedWallpaperId)
                }
            }
        }
    }

    private fun showMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }
}
