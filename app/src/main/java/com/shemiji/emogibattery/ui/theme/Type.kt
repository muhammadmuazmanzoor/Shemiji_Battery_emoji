package com.shemiji.emogibattery.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.shemiji.emogibattery.R

// Banner / Accent Font Family (Baloo 2)
val AppFontFamily = FontFamily(
    Font(R.font.regular, FontWeight.Normal),
    Font(R.font.medium, FontWeight.Medium),
    Font(R.font.semibold, FontWeight.SemiBold),
    Font(R.font.bold, FontWeight.Bold),
    Font(R.font.extrabold, FontWeight.ExtraBold)
)

// UI / Content Font Family (Inter)
val InterFontFamily = FontFamily(
    Font(R.font.inter_18light, FontWeight.Light),
    Font(R.font.inter_18medium, FontWeight.Medium),
    Font(R.font.inter_18bold, FontWeight.Bold),
    Font(R.font.inter_18extrabold, FontWeight.ExtraBold)
)

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    labelSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    )
)
