package com.shemiji.emogibattery.ui.screens

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.shemiji.emogibattery.data.model.WallpaperItem
import com.shemiji.emogibattery.data.model.imageModel
import com.shemiji.emogibattery.ui.components.ErrorState
import com.shemiji.emogibattery.ui.components.FeatureTopBar
import com.shemiji.emogibattery.ui.components.FullWidthActionButton
import com.shemiji.emogibattery.ui.components.LoadingState
import com.shemiji.emogibattery.ui.components.SourceBadge
import com.shemiji.emogibattery.ui.viewmodel.content.WallpapersViewModel

@Composable
fun WallpapersScreen(
    onBack: () -> Unit,
    viewModel: WallpapersViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val selectedWallpaper = uiState.wallpapers.firstOrNull {
        it.id == uiState.selectedWallpaperId
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = { FeatureTopBar("Wallpapers", onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingState(Modifier.padding(innerPadding))
            uiState.errorMessage != null && uiState.wallpapers.isEmpty() -> {
                ErrorState(
                    message = uiState.errorMessage.orEmpty(),
                    onRetry = viewModel::refresh,
                    modifier = Modifier.padding(innerPadding),
                )
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (uiState.isRefreshing) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text("Pick your background", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(
                                    "Tap a wallpaper to preview it",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                )
                            }
                            SourceBadge(uiState.sourceMode)
                        }
                    }

                    selectedWallpaper?.let { wallpaper ->
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(270.dp)
                                    .clip(RoundedCornerShape(24.dp)),
                            ) {
                                AsyncImage(
                                    model = wallpaper.imageModel(),
                                    contentDescription = wallpaper.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                                Text(
                                    text = "${wallpaper.name}  •  ${wallpaper.category}",
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(12.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                                        .padding(horizontal = 12.dp, vertical = 7.dp),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }

                    item(span = { GridItemSpan(maxLineSpan) }) {
                        FullWidthActionButton(
                            text = "Apply selected wallpaper",
                            onClick = viewModel::applySelectedWallpaper,
                            enabled = selectedWallpaper != null,
                            loading = uiState.isApplying,
                        )
                    }

                    uiState.errorMessage?.let { error ->
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
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

                    items(uiState.wallpapers, key = WallpaperItem::id) { wallpaper ->
                        WallpaperCard(
                            wallpaper = wallpaper,
                            selected = wallpaper.id == uiState.selectedWallpaperId,
                            onClick = { viewModel.selectWallpaper(wallpaper.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WallpaperCard(
    wallpaper: WallpaperItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(7.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(13.dp)),
        ) {
            AsyncImage(
                model = wallpaper.imageModel(),
                contentDescription = wallpaper.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(7.dp)
                        .size(24.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✓", color = MaterialTheme.colorScheme.onPrimary, fontSize = 13.sp)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = wallpaper.name,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = wallpaper.category,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
        )
    }
}
