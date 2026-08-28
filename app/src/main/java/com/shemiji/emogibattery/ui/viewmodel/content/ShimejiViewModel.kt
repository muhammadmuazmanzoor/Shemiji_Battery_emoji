package com.shemiji.emogibattery.ui.viewmodel.content

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shemiji.emogibattery.data.model.ContentSourceMode
import com.shemiji.emogibattery.data.model.ShimejiCharacter
import com.shemiji.emogibattery.data.preferences.SelectionPreferences
import com.shemiji.emogibattery.data.repository.ContentRepository
import com.shemiji.emogibattery.data.repository.ContentResult
import com.shemiji.emogibattery.system.OverlayServiceController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShimejiUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val characters: List<ShimejiCharacter> = emptyList(),
    val selectedCharacterId: String? = null,
    val isEnabled: Boolean = false,
    val isAccessibilityEnabled: Boolean = false,
    val shimejiSizeDp: Int = 112,
    val shimejiSpeed: Float = 1f,
    val errorMessage: String? = null,
    val message: String? = null,
    val sourceMode: ContentSourceMode = ContentSourceMode.LOCAL_DRAWABLES,
)

@HiltViewModel
class ShimejiViewModel @Inject constructor(
    private val repository: ContentRepository,
    private val preferences: SelectionPreferences,
    private val overlayController: OverlayServiceController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShimejiUiState(sourceMode = repository.sourceMode))
    val uiState: StateFlow<ShimejiUiState> = _uiState.asStateFlow()

    init {
        observeSelections()
        loadCharacters()
        checkAccessibility()
    }

    fun checkAccessibility() {
        _uiState.update { it.copy(isAccessibilityEnabled = overlayController.isAccessibilityServiceEnabled()) }
    }

    fun refresh() = loadCharacters(refreshing = true)

    fun selectCharacter(id: String) {
        _uiState.update { it.copy(selectedCharacterId = id, message = null) }
    }

    fun enableShimeji() {
        val character = _uiState.value.characters.firstOrNull {
            it.id == _uiState.value.selectedCharacterId
        } ?: return showMessage("Select a Shimeji character first")

        viewModelScope.launch {
            overlayController.startShimeji(character)
                .onSuccess {
                    preferences.setShimeji(character.id, enabled = true)
                    showMessage("${character.name} is now active")
                }
                .onFailure { throwable ->
                    showMessage(throwable.message ?: "Unable to start the Shimeji character")
                }
        }
    }

    fun enableShimeji(
        characterId: String,
        sizeDp: Int = _uiState.value.shimejiSizeDp,
        speed: Float = _uiState.value.shimejiSpeed,
    ) {
        val character = _uiState.value.characters.firstOrNull { it.id == characterId }
            ?: return showMessage("This Shimeji character is unavailable")
        _uiState.update { it.copy(selectedCharacterId = characterId, message = null) }

        viewModelScope.launch {
            val safeSize = sizeDp.coerceIn(72, 176)
            val safeSpeed = speed.coerceIn(0.5f, 3f)
            overlayController.startShimeji(character, safeSize, safeSpeed)
                .onSuccess {
                    preferences.setShimeji(
                        characterId = character.id,
                        enabled = true,
                        sizeDp = safeSize,
                        speed = safeSpeed,
                    )
                    showMessage("${character.name} is now active")
                }
                .onFailure { throwable ->
                    showMessage(throwable.message ?: "Unable to start the Shimeji character")
                }
        }
    }

    fun disableShimeji() {
        viewModelScope.launch {
            overlayController.stopShimeji()
                .onSuccess {
                    preferences.setShimejiEnabled(false)
                    showMessage("Shimeji character stopped")
                }
                .onFailure { throwable ->
                    showMessage(throwable.message ?: "Unable to stop the Shimeji character")
                }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun loadCharacters(refreshing: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !refreshing && it.characters.isEmpty(),
                    isRefreshing = refreshing,
                    errorMessage = null,
                )
            }
            when (val result = repository.getShimejiCharacters()) {
                is ContentResult.Success -> _uiState.update { current ->
                    val contentError = if (result.value.isEmpty()) {
                        "No Shimeji characters are available"
                    } else {
                        null
                    }
                    current.copy(
                        isLoading = false,
                        isRefreshing = false,
                        characters = result.value,
                        selectedCharacterId = current.selectedCharacterId
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
                    it.copy(
                        selectedCharacterId = selections.shimejiCharacterId
                            ?: it.selectedCharacterId,
                        isEnabled = selections.shimejiEnabled,
                        shimejiSizeDp = selections.shimejiSizeDp,
                        shimejiSpeed = selections.shimejiSpeed,
                    )
                }
            }
        }
    }

    private fun showMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }
}
