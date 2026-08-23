package com.shemiji.emogibattery.ui.viewmodel.content

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shemiji.emogibattery.data.model.BatteryEmoji
import com.shemiji.emogibattery.data.model.ContentSourceMode
import com.shemiji.emogibattery.data.model.ToolbarStyle
import com.shemiji.emogibattery.data.preferences.SelectionPreferences
import com.shemiji.emogibattery.data.repository.ContentRepository
import com.shemiji.emogibattery.data.repository.ContentResult
import com.shemiji.emogibattery.system.OverlayServiceController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BatteryCustomizationUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val batteryEmojis: List<BatteryEmoji> = emptyList(),
    val toolbarStyles: List<ToolbarStyle> = emptyList(),
    val selectedBatteryId: String? = null,
    val selectedToolbarId: String? = null,
    val isToolbarEnabled: Boolean = false,
    val isAccessibilityEnabled: Boolean = false,
    val errorMessage: String? = null,
    val message: String? = null,
    val sourceMode: ContentSourceMode = ContentSourceMode.LOCAL_DRAWABLES,
)

@HiltViewModel
class BatteryCustomizationViewModel @Inject constructor(
    private val repository: ContentRepository,
    private val preferences: SelectionPreferences,
    private val overlayController: OverlayServiceController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        BatteryCustomizationUiState(sourceMode = repository.sourceMode),
    )
    val uiState: StateFlow<BatteryCustomizationUiState> = _uiState.asStateFlow()

    init {
        observeSelections()
        loadContent()
        checkAccessibility()
    }

    fun checkAccessibility() {
        _uiState.update { it.copy(isAccessibilityEnabled = overlayController.isAccessibilityServiceEnabled()) }
    }

    fun refresh() = loadContent(refreshing = true)

    fun selectBattery(id: String) {
        _uiState.update { it.copy(selectedBatteryId = id, message = null) }
    }

    fun selectToolbar(id: String) {
        _uiState.update { it.copy(selectedToolbarId = id, message = null) }
    }

    fun enableToolbar() {
        val state = _uiState.value
        val battery = state.batteryEmojis.firstOrNull { it.id == state.selectedBatteryId }
            ?: return showMessage("Select a battery emoji first")
        val toolbar = state.toolbarStyles.firstOrNull { it.id == state.selectedToolbarId }
            ?: return showMessage("Select a toolbar style first")

        viewModelScope.launch {
            overlayController.startBatteryToolbar(battery, toolbar)
                .onSuccess {
                    preferences.setBatteryCustomization(battery.id, toolbar.id, enabled = true)
                    showMessage("Battery toolbar enabled")
                }
                .onFailure { throwable ->
                    showMessage(throwable.message ?: "Unable to enable the battery toolbar")
                }
        }
    }

    fun disableToolbar() {
        viewModelScope.launch {
            overlayController.stopBatteryToolbar()
                .onSuccess {
                    preferences.setBatteryToolbarEnabled(false)
                    showMessage("Battery toolbar disabled")
                }
                .onFailure { throwable ->
                    showMessage(throwable.message ?: "Unable to disable the battery toolbar")
                }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun loadContent(refreshing: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !refreshing && it.batteryEmojis.isEmpty(),
                    isRefreshing = refreshing,
                    errorMessage = null,
                )
            }

            val batteriesRequest = async { repository.getBatteryEmojis() }
            val toolbarsRequest = async { repository.getToolbarStyles() }
            val batteriesResult = batteriesRequest.await()
            val toolbarsResult = toolbarsRequest.await()

            val batteries = when (batteriesResult) {
                is ContentResult.Success -> batteriesResult.value
                is ContentResult.Error -> emptyList()
            }
            val toolbars = when (toolbarsResult) {
                is ContentResult.Success -> toolbarsResult.value
                is ContentResult.Error -> emptyList()
            }
            val errors = listOfNotNull(
                (batteriesResult as? ContentResult.Error)?.message,
                (toolbarsResult as? ContentResult.Error)?.message,
            ).distinct().toMutableList().apply {
                if (isEmpty() && batteries.isEmpty() && toolbars.isEmpty()) {
                    add("No battery or toolbar content is available")
                }
            }

            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    isRefreshing = false,
                    batteryEmojis = batteries.ifEmpty { current.batteryEmojis },
                    toolbarStyles = toolbars.ifEmpty { current.toolbarStyles },
                    selectedBatteryId = current.selectedBatteryId
                        ?.takeIf { id -> batteries.any { it.id == id } }
                        ?: batteries.firstOrNull()?.id,
                    selectedToolbarId = current.selectedToolbarId
                        ?.takeIf { id -> toolbars.any { it.id == id } }
                        ?: toolbars.firstOrNull()?.id,
                    errorMessage = errors.takeIf { it.isNotEmpty() }?.joinToString("\n"),
                )
            }
        }
    }

    private fun observeSelections() {
        viewModelScope.launch {
            preferences.selections.collect { selections ->
                _uiState.update {
                    it.copy(
                        selectedBatteryId = selections.batteryEmojiId ?: it.selectedBatteryId,
                        selectedToolbarId = selections.toolbarStyleId ?: it.selectedToolbarId,
                        isToolbarEnabled = selections.batteryToolbarEnabled,
                    )
                }
            }
        }
    }

    private fun showMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }
}
