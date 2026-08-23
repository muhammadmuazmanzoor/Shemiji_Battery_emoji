package com.shemiji.emogibattery.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.shemiji.emogibattery.data.model.imageModel
import com.shemiji.emogibattery.ui.components.ErrorState
import com.shemiji.emogibattery.ui.components.FeatureTopBar
import com.shemiji.emogibattery.ui.components.FullWidthActionButton
import com.shemiji.emogibattery.ui.components.LoadingState
import com.shemiji.emogibattery.ui.components.SelectableImageCard
import com.shemiji.emogibattery.ui.components.SourceBadge
import com.shemiji.emogibattery.ui.components.parseComposeColor
import com.shemiji.emogibattery.ui.viewmodel.content.BatteryCustomizationViewModel

@Composable
fun BatteryCustomizationScreen(
    onBack: () -> Unit,
    viewModel: BatteryCustomizationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showAccessibilityDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.checkAccessibility()
    }

    var customHeight by remember { mutableStateOf(uiState.customToolbarHeight) }
    var customLeftMargin by remember { mutableStateOf(uiState.customToolbarLeftMargin) }
    var customRightMargin by remember { mutableStateOf(uiState.customToolbarRightMargin) }
    var selectedIconColor by remember { mutableStateOf(uiState.customToolbarIconColor) }
    var selectedBackgroundColor by remember { mutableStateOf(uiState.customToolbarBackgroundColor) }
    var selectedBackgroundImage by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.customToolbarHeight, uiState.customToolbarLeftMargin, uiState.customToolbarRightMargin, uiState.customToolbarIconColor, uiState.customToolbarBackgroundColor) {
        customHeight = uiState.customToolbarHeight
        customLeftMargin = uiState.customToolbarLeftMargin
        customRightMargin = uiState.customToolbarRightMargin
        selectedIconColor = uiState.customToolbarIconColor
        selectedBackgroundColor = uiState.customToolbarBackgroundColor
    }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        if (Settings.canDrawOverlays(context)) viewModel.enableToolbar()
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    if (showAccessibilityDialog) {
        AlertDialog(
            onDismissRequest = { showAccessibilityDialog = false },
            title = { Text("Highly Recommended") },
            text = { Text("To ensure the battery toolbar stays on top of other app overlays and remains persistent, please enable the Accessibility Service for Shemiji Battery in your system settings.") },
            confirmButton = {
                Button(onClick = {
                    showAccessibilityDialog = false
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }) {
                    Text("Go to Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAccessibilityDialog = false }) {
                    Text("Maybe Later")
                }
            }
        )
    }

    Scaffold(
        topBar = { FeatureTopBar("Battery & Toolbar", onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingState(Modifier.padding(innerPadding))
            uiState.errorMessage != null &&
                uiState.batteryEmojis.isEmpty() &&
                uiState.toolbarStyles.isEmpty() -> {
                ErrorState(
                    message = uiState.errorMessage.orEmpty(),
                    onRetry = viewModel::refresh,
                    modifier = Modifier.padding(innerPadding),
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    if (uiState.isRefreshing) {
                        item {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "Live preview",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                )
                                SourceBadge(uiState.sourceMode)
                            }
                            Spacer(Modifier.height(12.dp))
                            BatteryToolbarPreview(
                                emojiModel = uiState.batteryEmojis
                                    .firstOrNull { it.id == uiState.selectedBatteryId }
                                    ?.imageModel(),
                                backgroundColor = selectedBackgroundColor,
                                contentColor = selectedIconColor,
                                accentColor = selectedIconColor,
                                height = customHeight,
                                leftMargin = customLeftMargin,
                                rightMargin = customRightMargin,
                            )
                        }
                    }

                    uiState.errorMessage?.let { error ->
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = error,
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 12.sp,
                                )
                                TextButton(onClick = viewModel::refresh) { Text("Retry") }
                            }
                        }
                    }

                    item {
                        SectionTitle(
                            title = "Choose battery emoji",
                            subtitle = "This icon updates with your selected toolbar.",
                        )
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(uiState.batteryEmojis, key = { it.id }) { emoji ->
                                SelectableImageCard(
                                    title = emoji.name,
                                    imageModel = emoji.imageModel(),
                                    selected = emoji.id == uiState.selectedBatteryId,
                                    onClick = { viewModel.selectBattery(emoji.id) },
                                    modifier = Modifier.size(width = 132.dp, height = 166.dp),
                                )
                            }
                        }
                    }

                    item {
                        StatusBarCustomizationCard(
                            height = customHeight,
                            leftMargin = customLeftMargin,
                            rightMargin = customRightMargin,
                            iconColor = selectedIconColor,
                            backgroundColor = selectedBackgroundColor,
                            backgroundImageSelected = selectedBackgroundImage,
                            onHeightChange = { customHeight = it },
                            onLeftMarginChange = { customLeftMargin = it },
                            onRightMarginChange = { customRightMargin = it },
                            onIconColorSelected = { selectedIconColor = it },
                            onBackgroundColorSelected = { selectedBackgroundColor = it },
                            onBackgroundImageClick = { selectedBackgroundImage = !selectedBackgroundImage },
                        )
                    }

                    item {
                        Button(
                            onClick = {
                                if (uiState.isToolbarEnabled) {
                                    viewModel.applyCustomToolbarSettings(
                                        customHeight = customHeight,
                                        customLeftMargin = customLeftMargin,
                                        customRightMargin = customRightMargin,
                                        customIconColor = selectedIconColor,
                                        customBackgroundColor = selectedBackgroundColor,
                                    )
                                    return@Button
                                }

                                if (uiState.isAccessibilityEnabled || Settings.canDrawOverlays(context)) {
                                    viewModel.enableToolbar(
                                        customHeight = customHeight,
                                        customLeftMargin = customLeftMargin,
                                        customRightMargin = customRightMargin,
                                        customIconColor = selectedIconColor,
                                        customBackgroundColor = selectedBackgroundColor,
                                    )
                                } else {
                                    overlayPermissionLauncher.launch(
                                        Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}"),
                                        ),
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Text("Apply status bar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BatteryToolbarPreview(
    emojiModel: Any?,
    backgroundColor: Color,
    contentColor: Color,
    accentColor: Color,
    height: Float = 34f,
    leftMargin: Float = 16f,
    rightMargin: Float = 16f,
) {
    val toolbarHeight = height.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = leftMargin.dp, end = rightMargin.dp)
            .height(toolbarHeight)
            .clip(RoundedCornerShape(0.dp))
            .background(backgroundColor)
            .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(0.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "13:14",
            color = contentColor,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(end = 6.dp),
        )
        AsyncImage(
            model = emojiModel,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.weight(1f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = "◔", color = contentColor, fontSize = 12.sp)
            Text(text = "5%", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun StatusBarCustomizationCard(
    height: Float,
    leftMargin: Float,
    rightMargin: Float,
    iconColor: Color,
    backgroundColor: Color,
    backgroundImageSelected: Boolean,
    onHeightChange: (Float) -> Unit,
    onLeftMarginChange: (Float) -> Unit,
    onRightMarginChange: (Float) -> Unit,
    onIconColorSelected: (Color) -> Unit,
    onBackgroundColorSelected: (Color) -> Unit,
    onBackgroundImageClick: () -> Unit,
) {
    val iconColors = listOf(Color.White, Color.Black, Color(0xFF36D399), Color(0xFF2EC5FF), Color(0xFFFF7A59), Color(0xFF8B5CF6))
    val backgroundColors = listOf(
        Color(0xFFF4D10F),
        Color(0xFF1F2937),
        Color(0xFFFFF7ED),
        Color(0xFFD9F99D),
        Color(0xFFB8E6FF),
        Color(0xFFE9D5FF),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Text(
            text = "Status Bar Custom",
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
        )
        Spacer(Modifier.height(16.dp))

        Text("Size", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Spacer(Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Height", modifier = Modifier.weight(1f), fontSize = 13.sp)
            Text("${height.toInt()}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(value = height, onValueChange = onHeightChange, valueRange = 24f..60f)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Left margin", modifier = Modifier.weight(1f), fontSize = 13.sp)
            Text("${leftMargin.toInt()}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(value = leftMargin, onValueChange = onLeftMarginChange, valueRange = 0f..32f)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Right margin", modifier = Modifier.weight(1f), fontSize = 13.sp)
            Text("${rightMargin.toInt()}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(value = rightMargin, onValueChange = onRightMarginChange, valueRange = 0f..32f)

        Spacer(Modifier.height(18.dp))
        Text("Appearance", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Icon color", modifier = Modifier.weight(1f), fontSize = 13.sp)
            iconColors.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(color, shape = RoundedCornerShape(50))
                        .border(
                            width = if (iconColor == color) 2.dp else 1.dp,
                            color = if (iconColor == color) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(50),
                        )
                        .clickable { onIconColorSelected(color) },
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Background color", modifier = Modifier.weight(1f), fontSize = 13.sp)
            backgroundColors.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(color, shape = RoundedCornerShape(50))
                        .border(
                            width = if (backgroundColor == color) 2.dp else 1.dp,
                            color = if (backgroundColor == color) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(50),
                        )
                        .clickable { onBackgroundColorSelected(color) },
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onBackgroundImageClick)
                .background(Color(0xFFDBF3FF), shape = RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Background image", modifier = Modifier.weight(1f), fontSize = 13.sp, color = Color(0xFF093048))
            Text(if (backgroundImageSelected) "Selected" else "View More", fontSize = 12.sp, color = Color(0xFF0F5C7B), fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ToolbarStyleCard(
    title: String,
    backgroundColor: Color,
    contentColor: Color,
    accentColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(17.dp)
    Column(
        modifier = Modifier
            .size(width = 158.dp, height = 112.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                shape,
            )
            .clickable(onClick = onClick)
            .padding(9.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(backgroundColor)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("●", color = accentColor)
            Spacer(Modifier.size(6.dp))
            Text("Battery", color = contentColor, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text("78%", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 10.dp)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Spacer(Modifier.height(3.dp))
        Text(
            subtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )
    }
}
@Preview
@Composable
fun displayBatteryCustomizationScreen(){
    BatteryCustomizationScreen(onBack = {})
}
