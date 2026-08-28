package com.shemiji.emogibattery.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shemiji.emogibattery.data.model.ShimejiCharacter
import com.shemiji.emogibattery.data.model.ShimejiPose
import com.shemiji.emogibattery.data.model.ShimejiPoses
import com.shemiji.emogibattery.system.AccessibilityPermission
import com.shemiji.emogibattery.ui.components.ErrorState
import com.shemiji.emogibattery.ui.components.FeatureTopBar
import com.shemiji.emogibattery.ui.components.LoadingState
import com.shemiji.emogibattery.ui.components.SpriteSheetPose
import com.shemiji.emogibattery.ui.theme.InterFontFamily
import com.shemiji.emogibattery.ui.theme.buttongradientEnd
import com.shemiji.emogibattery.ui.theme.buttongradientStart
import com.shemiji.emogibattery.ui.theme.neutral500
import com.shemiji.emogibattery.ui.theme.neutral700
import com.shemiji.emogibattery.ui.theme.primary200
import com.shemiji.emogibattery.ui.theme.primary600
import com.shemiji.emogibattery.ui.theme.primory100
import com.shemiji.emogibattery.ui.viewmodel.content.ShimejiViewModel

@Composable
fun ShimejiScreen(
    onBack: () -> Unit,
    onCharacterClick: (String) -> Unit,
    viewModel: ShimejiViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { FeatureTopBar("All Shimeji", onBack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingState(Modifier.padding(innerPadding))
            uiState.errorMessage != null && uiState.characters.isEmpty() -> ErrorState(
                message = uiState.errorMessage.orEmpty(),
                onRetry = viewModel::refresh,
                modifier = Modifier.padding(innerPadding),
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (uiState.isRefreshing) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                }
                items(uiState.characters, key = { it.id }) { character ->
                    CharacterCard(character, onClick = { onCharacterClick(character.id) })
                }
            }
        }
    }
}

@Composable
fun ShimejiDetailScreen(
    characterId: String,
    onBack: () -> Unit,
    viewModel: ShimejiViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val character = uiState.characters.firstOrNull { it.id == characterId }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedPose by remember(characterId) { mutableStateOf(ShimejiPoses.first()) }
    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        if (Settings.canDrawOverlays(context)) viewModel.enableShimeji(characterId)
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = { FeatureTopBar(character?.name ?: "Shimeji Details", onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (character != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Button(
                        onClick = {
                            when {
                                !AccessibilityPermission.isEnabled(context) -> {
                                    Toast.makeText(
                                        context,
                                        "Accessibility permission is necessary to show Shimejis",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                    AccessibilityPermission.openSettings(context)
                                }
                                Settings.canDrawOverlays(context) -> viewModel.enableShimeji(characterId)
                                else -> overlayPermissionLauncher.launch(
                                    Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}"),
                                    ),
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(buttongradientStart, buttongradientEnd),
                                    ),
                                    RoundedCornerShape(16.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "Enable Shimeji",
                                color = Color.White,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingState(Modifier.padding(innerPadding))
            character == null -> ErrorState(
                message = uiState.errorMessage ?: "This Shimeji character is unavailable",
                onRetry = viewModel::refresh,
                modifier = Modifier.padding(innerPadding),
            )
            else -> Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            ) {
                SelectedPoseHero(character, selectedPose)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Choose a pose",
                        color = neutral700,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                    Text(
                        "${ShimejiPoses.size} poses",
                        color = neutral500,
                        fontFamily = InterFontFamily,
                        fontSize = 12.sp,
                    )
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(ShimejiPoses, key = { it.id }) { pose ->
                        PoseCard(
                            drawableRes = character.drawableRes,
                            pose = pose,
                            selected = pose.id == selectedPose.id,
                            onClick = { selectedPose = pose },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CharacterCard(character: ShimejiCharacter, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(196.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(primory100)
            .border(1.dp, primary200, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        character.drawableRes?.let {
            SpriteSheetPose(
                drawableRes = it,
                row = 0,
                column = 0,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            character.name,
            color = neutral700,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun SelectedPoseHero(character: ShimejiCharacter, pose: ShimejiPose) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(230.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(primory100, Color.White)))
            .border(1.dp, primary200, RoundedCornerShape(24.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        character.drawableRes?.let {
            SpriteSheetPose(
                drawableRes = it,
                row = pose.row,
                column = pose.column,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        }
        Text(
            pose.name,
            color = primary600,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )
    }
}

@Composable
private fun PoseCard(
    drawableRes: Int?,
    pose: ShimejiPose,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Color(0xFFF0E9FF) else primory100)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) primary600 else primary200,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        drawableRes?.let {
            SpriteSheetPose(
                drawableRes = it,
                row = pose.row,
                column = pose.column,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        }
        Text(
            pose.name,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = if (selected) primary600 else neutral700,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            maxLines = 1,
        )
    }
}
