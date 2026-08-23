package com.shemiji.emogibattery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shemiji.emogibattery.BuildConfig
import com.shemiji.emogibattery.data.model.ContentSourceMode
import com.shemiji.emogibattery.ui.components.FeatureTopBar
import com.shemiji.emogibattery.ui.components.SourceBadge
import com.shemiji.emogibattery.ui.theme.AppTheme
import com.shemiji.emogibattery.ui.viewmodel.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeViewModel: ThemeViewModel,
    onBack: () -> Unit,
) {
    val theme by themeViewModel.theme.collectAsStateWithLifecycle()
    val sourceMode = if (BuildConfig.USE_REMOTE_DATA) {
        ContentSourceMode.REMOTE_API
    } else {
        ContentSourceMode.LOCAL_DRAWABLES
    }

    Scaffold(
        topBar = { FeatureTopBar("Settings", onBack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            Text("App theme", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppTheme.entries.forEach { option ->
                    FilterChip(
                        selected = theme == option,
                        onClick = { themeViewModel.setTheme(option) },
                        label = {
                            Text(option.name.lowercase().replaceFirstChar(Char::uppercase))
                        },
                    )
                }
            }

            Spacer(Modifier.height(26.dp))
            Text("Content source", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp),
            ) {
                SourceBadge(sourceMode)
                Spacer(Modifier.height(10.dp))
                Text(
                    text = if (sourceMode == ContentSourceMode.REMOTE_API) {
                        "All battery emojis, toolbar styles, wallpapers, and Shimeji characters are loaded through Retrofit."
                    } else {
                        "All screens are using the bundled drawable demo content, so the complete flow can be tested without the backend."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Change USE_REMOTE_DATA in app/build.gradle.kts to switch every feature together.",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                )
            }
        }
    }
}
