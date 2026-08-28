package com.shemiji.emogibattery.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shemiji.emogibattery.ui.theme.InterFontFamily
import com.shemiji.emogibattery.ui.theme.buttongradientEnd
import com.shemiji.emogibattery.ui.theme.buttongradientStart
import com.shemiji.emogibattery.ui.theme.primary600
import com.shemiji.emogibattery.ui.theme.primory100

private const val PermissionRequiredMessage =
    "Accessibility permission is necessary to show Shimejis"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibilityPermissionSheet(
    onAgree: () -> Unit,
) {
    val context = LocalContext.current
    var accepted by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    val showRequiredMessage = {
        Toast.makeText(context, PermissionRequiredMessage, Toast.LENGTH_SHORT).show()
    }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { target ->
            if (target == SheetValue.Hidden) {
                showRequiredMessage()
                false
            } else {
                true
            }
        },
    )

    ModalBottomSheet(
        onDismissRequest = showRequiredMessage,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 54.dp, height = 5.dp)
                    .background(primary600, RoundedCornerShape(50)),
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 24.dp,
                end = 24.dp,
                bottom = 24.dp,
            ),
        ) {
            item {
                Text(
                    text = "Accessibility Permission",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontFamily = InterFontFamily,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D1D22),
                )
                Spacer(Modifier.height(22.dp))
                Text(
                    "Shemiji Battery requires Accessibility Service permission to keep your selected Shimeji and battery customization visible over other apps.",
                    color = Color(0xFF66666F),
                    fontFamily = InterFontFamily,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    "The Accessibility Service is used to:",
                    color = Color(0xFF33333A),
                    fontFamily = InterFontFamily,
                    fontSize = 15.sp,
                )
                PermissionBullet("Display your selected Shimeji and battery overlay consistently.")
                PermissionBullet("Keep visual overlays aligned when the active app or screen changes.")
                PermissionBullet("Restore enabled customizations when the service reconnects.")
                Spacer(Modifier.height(8.dp))
                Text(
                    "We do not collect messages, passwords, notifications, or personal screen content. You can turn this permission off at any time in device settings.",
                    color = Color(0xFF66666F),
                    fontFamily = InterFontFamily,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = accepted,
                            role = Role.Checkbox,
                            onValueChange = { accepted = it },
                        )
                        .semantics(mergeDescendants = true) {},
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = accepted,
                        onCheckedChange = null,
                        modifier = Modifier.clearAndSetSemantics {},
                        colors = CheckboxDefaults.colors(
                            checkedColor = primary600,
                            uncheckedColor = primary600,
                        ),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "I have read and agree to continue",
                        color = primary600,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                }
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = onAgree,
                        enabled = accepted,
                        modifier = Modifier.weight(1f).height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primary600,
                            disabledContainerColor = Color(0xFFD7D6E0),
                        ),
                    ) {
                        Text("Agree", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = showRequiredMessage,
                        modifier = Modifier.weight(1f).height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, primary600),
                    ) {
                        Text(
                            "Close",
                            color = primary600,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionBullet(text: String) {
    Row(
        modifier = Modifier.padding(top = 10.dp, start = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text("•", color = Color(0xFF66666F), fontSize = 18.sp)
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            color = Color(0xFF66666F),
            fontFamily = InterFontFamily,
            fontSize = 15.sp,
            lineHeight = 21.sp,
        )
    }
}

@Composable
fun AccessibilityOnboardingScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val steps = listOf(
        "Open Downloaded apps or Installed services",
        "Select Shemiji Battery",
        "Turn on Use Shemiji Battery",
        "Review the system disclosure and tap Allow",
    )

    Box(Modifier.fillMaxSize().background(Color(0xFFF9F9FC))) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(72.dp)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "←",
                    modifier = Modifier
                        .size(48.dp)
                        .semantics {
                            contentDescription = "Back"
                            role = Role.Button
                        }
                        .clickable(onClick = onBack),
                    fontSize = 28.sp,
                    color = Color(0xFF1D1D22),
                )
                Text(
                    text = "How to use",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontFamily = InterFontFamily,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D1D22),
                )
                Spacer(Modifier.size(48.dp))
            }
            HorizontalDivider(color = Color(0xFFF0F0F3))
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 20.dp,
                    bottom = 116.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                itemsIndexed(steps) { index, step ->
                    PermissionStepCard(number = index + 1, text = step)
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.White)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Button(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(buttongradientStart, buttongradientEnd),
                            ),
                            RoundedCornerShape(14.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Go to Settings",
                        color = Color.White,
                        fontFamily = InterFontFamily,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionStepCard(number: Int, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(primory100, RoundedCornerShape(22.dp))
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(primary600, RoundedCornerShape(50)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                number.toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text,
            modifier = Modifier.weight(1f),
            color = Color(0xFF25252B),
            fontFamily = InterFontFamily,
            fontSize = 17.sp,
            lineHeight = 23.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
