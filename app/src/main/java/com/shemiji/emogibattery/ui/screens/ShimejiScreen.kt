package com.shemiji.emogibattery.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.shemiji.emogibattery.ui.viewmodel.content.ShimejiViewModel

@Composable
fun ShimejiScreen(
    onBack: () -> Unit,
    viewModel: ShimejiViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showAccessibilityDialog by remember { mutableStateOf(false) }

    val selectedCharacter = uiState.characters.firstOrNull {
        it.id == uiState.selectedCharacterId
    }
    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        if (Settings.canDrawOverlays(context)) viewModel.enableShimeji()
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
            text = { Text("To ensure your Shimeji friend stays on top of other app overlays and remains persistent, please enable the Accessibility Service for Shemiji Battery in your system settings.") },
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
        topBar = { FeatureTopBar("Shimeji Characters", onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingState(Modifier.padding(innerPadding))
            uiState.errorMessage != null && uiState.characters.isEmpty() -> {
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text("Character preview", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(
                                    "The overlay can be dragged anywhere",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                )
                            }
                            SourceBadge(uiState.sourceMode)
                        }
                    }

                    item {
                        ShimejiPreview(
                            characterName = selectedCharacter?.name.orEmpty(),
                            imageModel = selectedCharacter?.imageModel(),
                        )
                    }

                    uiState.errorMessage?.let { error ->
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    error,
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 12.sp,
                                )
                                TextButton(onClick = viewModel::refresh) { Text("Retry") }
                            }
                        }
                    }

                    item {
                        Column(modifier = Modifier.padding(start = 16.dp, top = 22.dp, bottom = 10.dp)) {
                            Text("Choose your friend", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text(
                                "Local vectors and remote GIF/image URLs use the same model.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                            )
                        }
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(uiState.characters, key = { it.id }) { character ->
                                SelectableImageCard(
                                    title = character.name,
                                    imageModel = character.imageModel(),
                                    selected = character.id == uiState.selectedCharacterId,
                                    onClick = { viewModel.selectCharacter(character.id) },
                                    modifier = Modifier.size(width = 142.dp, height = 172.dp),
                                    imageBackground = Color(0xFFF3F0FF),
                                )
                            }
                        }
                    }

                    item {
                        FullWidthActionButton(
                            text = if (uiState.isEnabled) "Stop Shimeji" else "Show over other apps",
                            onClick = {
                                if (uiState.isEnabled) {
                                    viewModel.disableShimeji()
                                } else {
                                    if (!uiState.isAccessibilityEnabled) {
                                        showAccessibilityDialog = true
                                    }
                                    if (Settings.canDrawOverlays(context)) {
                                        viewModel.enableShimeji()
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
                            destructive = uiState.isEnabled,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 22.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShimejiPreview(characterName: String, imageModel: Any?) {
    val transition = rememberInfiniteTransition(label = "shimeji-preview")
    val floatY by transition.animateFloat(
        initialValue = -8f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimeji-bob",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(250.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFECE9FF), Color(0xFFF7F6FF)),
                ),
            ),
    ) {
        Text(
            text = characterName.ifBlank { "Choose a character" },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            color = Color(0xFF4A4667),
            fontWeight = FontWeight.SemiBold,
        )
        AsyncImage(
            model = imageModel,
            contentDescription = characterName,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(170.dp)
                .graphicsLayer { translationY = floatY },
        )
        Spacer(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp)
                .size(width = 92.dp, height = 13.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF77728F).copy(alpha = 0.15f)),
        )
    }
}
