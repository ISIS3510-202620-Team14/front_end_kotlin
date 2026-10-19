@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.enad.enadmovil.ui.theme

import com.enad.enadmovil.R
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// Lexend en títulos y Work Sans en textos, igual que Flutter. Ambas son fuentes variables.
private fun variable(res: Int, weight: Int) =
    Font(res, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

val bodyFontFamily = FontFamily(
    variable(R.font.work_sans, 400),
    variable(R.font.work_sans, 500),
    variable(R.font.work_sans, 600),
    variable(R.font.work_sans, 700),
)

val displayFontFamily = FontFamily(
    variable(R.font.lexend, 500),
    variable(R.font.lexend, 700),
)

private fun heading(size: TextUnit, weight: FontWeight = FontWeight.Bold) = TextStyle(
    fontFamily = displayFontFamily, fontSize = size, fontWeight = weight,
    lineHeight = 1.2.em, color = EnadInk,
)

private fun body(size: TextUnit, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = bodyFontFamily, fontSize = size, fontWeight = weight, lineHeight = 1.5.em,
)

// Tamaños tomados de AppTheme.theme en Flutter.
val Typography = Typography(
    displayLarge = heading(28.8.sp),
    displayMedium = heading(28.8.sp),
    displaySmall = heading(28.8.sp),
    headlineLarge = heading(25.6.sp),
    headlineMedium = heading(25.6.sp),
    headlineSmall = heading(22.sp),
    titleLarge = heading(19.2.sp, FontWeight.Medium),
    titleMedium = heading(16.sp, FontWeight.Medium),
    titleSmall = heading(14.sp, FontWeight.Medium),
    bodyLarge = body(16.sp).copy(color = EnadInk),
    bodyMedium = body(14.sp).copy(color = EnadSoftInk),
    bodySmall = body(14.sp).copy(color = EnadSoftInk),
    labelLarge = body(16.sp, FontWeight.Bold),
    labelMedium = body(14.sp, FontWeight.SemiBold),
    labelSmall = body(12.sp, FontWeight.Medium),
)
