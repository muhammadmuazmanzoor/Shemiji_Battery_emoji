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
                                backgroundColor = uiState.toolbarStyles
                                    .firstOrNull { it.id == uiState.selectedToolbarId }
                                    ?.backgroundColor
                                    ?.let { parseComposeColor(it) }
                                    ?: Color(0xFF16182B),
                                contentColor = uiState.toolbarStyles
                                    .firstOrNull { it.id == uiState.selectedToolbarId }
                                    ?.contentColor
                                    ?.let { parseComposeColor(it) }
                                    ?: Color.White,
                                accentColor = uiState.toolbarStyles
                                    .firstOrNull { it.id == uiState.selectedToolbarId }
                                    ?.accentColor
                                    ?.let { parseComposeColor(it) }
                                    ?: Color(0xFF8B8FFF),
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
                        SectionTitle(
                            title = "Choose toolbar style",
                            subtitle = "Color values work the same for local and API content.",
                        )
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(uiState.toolbarStyles, key = { it.id }) { style ->
                                ToolbarStyleCard(
                                    title = style.name,
                                    backgroundColor = parseComposeColor(style.backgroundColor),
                                    contentColor = parseComposeColor(style.contentColor),
                                    accentColor = parseComposeColor(style.accentColor),
                                    selected = style.id == uiState.selectedToolbarId,
                                    onClick = { viewModel.selectToolbar(style.id) },
                                )
                            }
                        }
                    }

                    item {
                        FullWidthActionButton(
                            text = if (uiState.isToolbarEnabled) {
                                "Disable floating toolbar"
                            } else {
                                "Enable floating toolbar"
                            },
                            onClick = {
                                if (uiState.isToolbarEnabled) {
                                    viewModel.disableToolbar()
                                } else {
                                    if (!uiState.isAccessibilityEnabled) {
                                        showAccessibilityDialog = true
                                    }
                                    if (Settings.canDrawOverlays(context)) {
                                        viewModel.enableToolbar()
                                    } else {
                                        overlayPermissionLauncher.launch(
                                            Intent(
                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:${context.packageName}"),
                                            ),
                                        )
                                    }
                                }
                            },
                            destructive = uiState.isToolbarEnabled,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 22.dp),
                        )
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
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(1.dp, accentColor.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = emojiModel,
            contentDescription = null,
            modifier = Modifier.size(38.dp),
        )
        Spacer(Modifier.size(10.dp))
        Text(
            text = "My battery",
            color = contentColor,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        Text("78%", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 17.sp)
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
