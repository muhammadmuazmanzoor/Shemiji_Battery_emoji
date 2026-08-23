package com.shemiji.emogibattery.ui.viewmodel.content

import androidx.compose.ui.graphics.toArgb
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
    val customToolbarHeight: Float = 34f,
    val customToolbarLeftMargin: Float = 16f,
    val customToolbarRightMargin: Float = 16f,
    val customToolbarIconColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.White,
    val customToolbarBackgroundColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFF4D10F),
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

    fun refresh() {
        checkAccessibility()
        loadContent(refreshing = true)
    }

    fun selectBattery(id: String) {
        _uiState.update { it.copy(selectedBatteryId = id, message = null) }
    }

    fun selectToolbar(id: String) {
        _uiState.update { it.copy(selectedToolbarId = id, message = null) }
    }

    fun enableToolbar(
        customHeight: Float = 34f,
        customLeftMargin: Float = 16f,
        customRightMargin: Float = 16f,
        customIconColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.White,
        customBackgroundColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFF4D10F),
    ) {
        val state = _uiState.value
        val battery = state.batteryEmojis.firstOrNull { it.id == state.selectedBatteryId }
            ?: return showMessage("Select a battery emoji first")
        val toolbar = state.toolbarStyles.firstOrNull { it.id == state.selectedToolbarId }
            ?: buildCustomToolbarStyle(customIconColor, customBackgroundColor)

        viewModelScope.launch {
            val iconHex = colorToHex(customIconColor)
            val backgroundHex = colorToHex(customBackgroundColor)
            overlayController.startBatteryToolbar(
                battery,
                toolbar,
                height = customHeight.toInt(),
                leftMargin = customLeftMargin.toInt(),
                rightMargin = customRightMargin.toInt(),
                iconColor = iconHex,
                backgroundColor = backgroundHex,
            )
                .onSuccess {
                    preferences.setBatteryCustomization(
                        battery.id,
                        toolbar.id,
                        enabled = true,
                        height = customHeight.toInt(),
                        leftMargin = customLeftMargin.toInt(),
                        rightMargin = customRightMargin.toInt(),
                        iconColor = iconHex,
                        backgroundColor = backgroundHex,
                    )
                    showMessage("Battery toolbar enabled")
                }
                .onFailure { throwable ->
                    showMessage(throwable.message ?: "Unable to enable the battery toolbar")
                }
        }
    }

    fun applyCustomToolbarSettings(
        customHeight: Float,
        customLeftMargin: Float,
        customRightMargin: Float,
        customIconColor: androidx.compose.ui.graphics.Color,
        customBackgroundColor: androidx.compose.ui.graphics.Color,
    ) {
        val iconHex = colorToHex(customIconColor)
        val backgroundHex = colorToHex(customBackgroundColor)

        viewModelScope.launch {
            preferences.setToolbarAppearance(
                height = customHeight.toInt(),
                leftMargin = customLeftMargin.toInt(),
                rightMargin = customRightMargin.toInt(),
                iconColor = iconHex,
                backgroundColor = backgroundHex,
            )

            val state = _uiState.value
            if (state.isToolbarEnabled) {
                val battery = state.batteryEmojis.firstOrNull { it.id == state.selectedBatteryId }
                    ?: return@launch showMessage("Select a battery emoji first")
                val toolbar = state.toolbarStyles.firstOrNull { it.id == state.selectedToolbarId }
                    ?: buildCustomToolbarStyle(customIconColor, customBackgroundColor)

                overlayController.startBatteryToolbar(
                    battery,
                    toolbar,
                    height = customHeight.toInt(),
                    leftMargin = customLeftMargin.toInt(),
                    rightMargin = customRightMargin.toInt(),
                    iconColor = iconHex,
                    backgroundColor = backgroundHex,
                ).onSuccess {
                    _uiState.update {
                        it.copy(
                            customToolbarHeight = customHeight,
                            customToolbarLeftMargin = customLeftMargin,
                            customToolbarRightMargin = customRightMargin,
                            customToolbarIconColor = customIconColor,
                            customToolbarBackgroundColor = customBackgroundColor,
                        )
                    }
                    showMessage("Status bar settings applied")
                }.onFailure { throwable ->
                    showMessage(throwable.message ?: "Unable to update the toolbar")
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    customToolbarHeight = customHeight,
                    customToolbarLeftMargin = customLeftMargin,
                    customToolbarRightMargin = customRightMargin,
                    customToolbarIconColor = customIconColor,
                    customToolbarBackgroundColor = customBackgroundColor,
                )
            }
            showMessage("Status bar settings saved")
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
                        customToolbarHeight = selections.toolbarHeight.toFloat().coerceIn(24f, 60f),
                        customToolbarLeftMargin = selections.toolbarLeftMargin.toFloat().coerceIn(0f, 32f),
                        customToolbarRightMargin = selections.toolbarRightMargin.toFloat().coerceIn(0f, 32f),
                        customToolbarIconColor = selections.toolbarIconColor
                            ?.let { colorString ->
                                val parsed = android.graphics.Color.parseColor(colorString)
                                androidx.compose.ui.graphics.Color(parsed)
                            }
                            ?: it.customToolbarIconColor,
                        customToolbarBackgroundColor = selections.toolbarBackgroundColor
                            ?.let { colorString ->
                                val parsed = android.graphics.Color.parseColor(colorString)
                                androidx.compose.ui.graphics.Color(parsed)
                            }
                            ?: it.customToolbarBackgroundColor,
                    )
                }
            }
        }
    }

    private fun showMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }

    private fun colorToHex(color: androidx.compose.ui.graphics.Color): String {
        val argb = color.toArgb()
        val rgb = argb and 0x00FFFFFF
        return String.format("#%06X", rgb)
    }

    private fun buildCustomToolbarStyle(
        customIconColor: androidx.compose.ui.graphics.Color,
        customBackgroundColor: androidx.compose.ui.graphics.Color,
    ): ToolbarStyle = ToolbarStyle(
        id = "custom_toolbar",
        name = "Custom",
        backgroundColor = colorToHex(customBackgroundColor),
        contentColor = colorToHex(customIconColor),
        accentColor = colorToHex(customIconColor),
    )
}
