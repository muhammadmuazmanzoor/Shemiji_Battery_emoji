package com.shemiji.emogibattery.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.shemiji.emogibattery.R
import com.shemiji.emogibattery.data.model.BatteryEmoji
import com.shemiji.emogibattery.data.model.ShimejiCharacter
import com.shemiji.emogibattery.data.model.WallpaperItem
import com.shemiji.emogibattery.data.model.imageModel
import com.shemiji.emogibattery.ui.theme.AppFontFamily
import com.shemiji.emogibattery.ui.theme.InterFontFamily
import com.shemiji.emogibattery.ui.theme.buttongradientEnd
import com.shemiji.emogibattery.ui.theme.buttongradientStart
import com.shemiji.emogibattery.ui.theme.neutral200
import com.shemiji.emogibattery.ui.theme.neutral700
import com.shemiji.emogibattery.ui.theme.primary600
import com.shemiji.emogibattery.ui.theme.warning
import com.shemiji.emogibattery.ui.components.SpriteSheetPose
import com.shemiji.emogibattery.ui.viewmodel.content.BatteryCustomizationViewModel
import com.shemiji.emogibattery.ui.viewmodel.content.ShimejiViewModel
import com.shemiji.emogibattery.ui.viewmodel.content.WallpapersViewModel

@Composable
fun HomeScreen(
    onBatteryCustomization: () -> Unit,
    onWallpapers: () -> Unit,
    onShimeji: () -> Unit,
    onShimejiCharacter: (String) -> Unit,
    onSettings: () -> Unit,
    showAccessibilityPermission: Boolean = false,
    onAccessibilityAgree: () -> Unit = {},
    shimejiViewModel: ShimejiViewModel = hiltViewModel(),
    batteryViewModel: BatteryCustomizationViewModel = hiltViewModel(),
    wallpapersViewModel: WallpapersViewModel = hiltViewModel(),
) {
    val shimejiUiState by shimejiViewModel.uiState.collectAsStateWithLifecycle()
    val batteryUiState by batteryViewModel.uiState.collectAsStateWithLifecycle()
    val wallpapersUiState by wallpapersViewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        Scaffold(
            topBar = {
                HomeTopBar(onSettings = onSettings)
            },
            containerColor = Color.Transparent,
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(
                    top = 6.dp,
                    bottom = 24.dp,
                ),
              verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                item { 
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        HomeHero(onEnableAnimation = onShimeji) 
                    }
                }

                // Trending Shimeji
                if (shimejiUiState.characters.isNotEmpty()) {
                    item {
                        HomeSection(
                            title = "Trending Shimeji",
                            emoji = "🔥",
                            onSeeAll = onShimeji
                        ) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp)
                            ) {
                                items(shimejiUiState.characters, key = { it.id }) { character ->
                                    TrendingShimejiItem(
                                        character = character,
                                        onClick = { 
                                            onShimejiCharacter(character.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Battery Emoji
                if (batteryUiState.batteryEmojis.isNotEmpty()) {
                    item {
                        HomeSection(
                            title = "Battery Emoji",
                            emoji = "⚡",
                            onSeeAll = onBatteryCustomization
                        ) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                               contentPadding = PaddingValues(start = 16.dp, end = 16.dp)
                            ) {
                                items(batteryUiState.batteryEmojis.take(5), key = { it.id }) { emoji ->
                                    BatteryEmojiItem(
                                        emoji = emoji,
                                        onClick = {
                                            batteryViewModel.selectBattery(emoji.id)
                                            onBatteryCustomization()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Wallpapers
                if (wallpapersUiState.wallpapers.isNotEmpty()) {
                    item {
                        HomeSection(
                            title = "Wallpapers",
                            emoji = "🖼️",
                            onSeeAll = onWallpapers
                        ) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 4.dp)
                            ) {
                                items(wallpapersUiState.wallpapers.take(5), key = { it.id }) { wallpaper ->
                                    WallpaperHomeItem(
                                        wallpaper = wallpaper,
                                        onClick = {
                                            wallpapersViewModel.selectWallpaper(wallpaper.id)
                                            onWallpapers()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAccessibilityPermission) {
            AccessibilityPermissionSheet(onAgree = onAccessibilityAgree)
        }
    }
}

@Composable
private fun HomeSection(
    title: String,
    emoji: String,
    onSeeAll: () -> Unit,
    content: @Composable () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 14.sp, fontWeight = FontWeight.Bold)
               // Spacer(Modifier.width(2.dp))
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily,
                    color = neutral700
                )
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onSeeAll)
                    .padding(0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "See All",
                    fontSize = 11.sp,
                    color = primary600,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFontFamily
                )
                Spacer(modifier = Modifier.width(0.dp))
                Icon(
                    painter = painterResource(R.drawable.right),
                    contentDescription = null,
                    tint = Color.Unspecified
                )
            }
        }
        
        Spacer(Modifier.height(6.dp))
        content()
    }
}

@Composable
private fun HomeTopBar(onSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.battery_home),
                contentDescription = null,
                modifier = Modifier.width(100.dp).height(34.dp)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ToolbarActionButton(icon = R.drawable.ic_heart, onClick = {})
            ToolbarActionButton(icon = R.drawable.ic_pro, onClick = {})
            ToolbarActionButton(icon = R.drawable.ic_setting, onClick = onSettings)
        }
    }
}

@Composable
private fun ToolbarActionButton(icon: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .buttonShadow()
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = icon),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun Modifier.buttonShadow(
    borderRadius: Dp = 8.dp,
    blurRadius: Dp = 8.dp,
    offsetY: Dp = 3.dp
) = drawBehind {
    drawIntoCanvas { canvas ->
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()
        frameworkPaint.color = android.graphics.Color.TRANSPARENT
        frameworkPaint.setShadowLayer(
            blurRadius.toPx(),
            0f,
            offsetY.toPx(),
            Color.Black.copy(alpha = 0.06f).toArgb()
        )
        canvas.drawRoundRect(
            0f,
            0f,
            size.width,
            size.height,
            borderRadius.toPx(),
            borderRadius.toPx(),
            paint
        )
    }
}


@Composable
private fun HomeHero(onEnableAnimation: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        Image(
            painter = painterResource(id = R.drawable.banner_image),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Column(verticalArrangement = Arrangement.spacedBy((-4).dp)) {
                Text(
                    text = "Cute Companions,",
                    color = neutral700,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = AppFontFamily,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeight = 14.sp
                    )
                )
                Text(
                    text = "Endless Fun!",
                    style = TextStyle(
                        brush = Brush.linearGradient(
                            colors = listOf(buttongradientStart, buttongradientEnd)
                        ),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = AppFontFamily,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeight = 20.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(35.dp).height(1.dp).background(Color(0xFFFFB8D1).copy(alpha = 0.6f)))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "★",
                    color = Color(0xFFFFB8D1),
                    fontSize = 12.sp,
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(modifier = Modifier.width(35.dp).height(1.dp).background(Color(0xFFFFB8D1).copy(alpha = 0.6f)))
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Add your favorite Shimeji\nand let them live on your screen.",
                color = Color(0xFF171F37).copy(alpha = 0.8f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = AppFontFamily,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeight = 10.sp
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(buttongradientStart, buttongradientEnd)
                        )
                    )
                    .clickable(onClick = onEnableAnimation)
                    .padding(horizontal = 12.dp, vertical = 3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Enable Animation",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = AppFontFamily
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(modifier = Modifier.width(1.dp).height(10.dp).background(Color.White.copy(alpha = 0.4f)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "→",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = AppFontFamily
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendingShimejiItem(
    character: ShimejiCharacter,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(90.dp)
            .height(127.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFEF3F7))
            .border(0.5.dp, Color(0xFFFCE5F1), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(3.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.heart),
            contentDescription = null,
            modifier = Modifier
                .size(20.dp)
                .align(Alignment.TopEnd)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            character.drawableRes?.let { drawableRes ->
                SpriteSheetPose(
                    drawableRes = drawableRes,
                    row = 0,
                    column = 0,
                    modifier = Modifier
                        .width(78.dp)
                        .height(88.dp)
                        .padding(top = 4.dp, start = 4.dp, end = 4.dp),
                )
            } ?: AsyncImage(
                model = character.imageModel(),
                contentDescription = character.name,
                modifier = Modifier.width(78.dp).height(88.dp),
            )
            Box(
                modifier = Modifier
                    .width(78.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, primary600, RoundedCornerShape(50)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = character.name,
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                    maxLines = 1,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFontFamily,
                    color = primary600,
                )
            }
        }
    }
}

@Composable
private fun BatteryEmojiItem(
    emoji: BatteryEmoji,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(90.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFEEE5FC))
            .clickable(onClick = onClick)
            .border(color = neutral200, width = 0.5.dp, shape = RoundedCornerShape(12.dp))
            .padding(top = 4.dp, start = 4.dp, end = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = emoji.imageModel(),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth()
                .height(54.dp)
        )
        if(emoji.isPremium) {
            Image(
                painter = painterResource(R.drawable.pro_ic),
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.TopEnd)
                    .buttonShadow()
            )
        }

    }
}

@Composable
private fun WallpaperHomeItem(
    wallpaper: WallpaperItem,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(96.dp)
            .height(126.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)

    ) {
        AsyncImage(
            model = wallpaper.imageModel(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        if (wallpaper.isPremium) {
            Image(
                painter = painterResource(id = R.drawable.pro_ic),
                contentDescription = null,
                modifier = Modifier
                    .padding(4.dp)
                    .size(18.dp)
                    .align(Alignment.TopEnd)
                    .buttonShadow()
            )
        }
    }
}

@Preview
@Composable
fun displayHomeScreen() {
    HomeScreen(
        onBatteryCustomization = {},
        onWallpapers = {},
        onShimeji = {},
        onShimejiCharacter = {},
        onSettings = {},
    )
}
