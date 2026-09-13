package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Brand / Primary: Refined Muted Blue (Slate Blue / Financial Executive)
val MutedBluePrimary = Color(0xFF2C5E8A)
val MutedBlueLight = Color(0xFF4378A6)
val MutedBlueDark = Color(0xFF1B4266)
val MutedBlueSecondary = Color(0xFF1E6B8A)
val MutedBlueContainer = Color(0xFFEEF4F8)
val MutedBlueBorder = Color(0xFFCFDFEC)

// Aliases for compatibility: All previous green elements now render as Muted Blue
val EmeraldPrimary = MutedBluePrimary
val EmeraldLight = MutedBlueLight
val EmeraldDark = MutedBlueDark
val EmeraldContainer = MutedBlueContainer
val EmeraldBorder = MutedBlueBorder

// Secondary: Refined Steel / Financial Cyan-Blue
val CyanSecondary = Color(0xFF1E6B8A)
val CyanLight = Color(0xFF2988AF)
val CyanDark = Color(0xFF16546D)
val CyanContainer = Color(0xFFEEF6F9)
val CyanBorder = Color(0xFFD2E6EE)

// Tertiary / Warning: Restrained Warm Amber
val AmberTertiary = Color(0xFFB46514)
val AmberLight = Color(0xFFCE781D)
val AmberContainer = Color(0xFFFEF6E9)
val AmberBorder = Color(0xFFFCE1BD)
val AmberWarning = AmberTertiary
val AmberWarningContainer = AmberContainer

// Muted Blue Status Colors (Replacing previous status green with muted blue)
val RestrainedBlue = MutedBluePrimary
val RestrainedBlueContainer = MutedBlueContainer
val RestrainedBlueBorder = MutedBlueBorder

// Backward-compatibility aliases (redirected to muted blue)
val RestrainedGreen = RestrainedBlue
val RestrainedGreenContainer = RestrainedBlueContainer
val RestrainedGreenBorder = RestrainedBlueBorder

// Dedicated PnL Profit Colors (Used EXCLUSIVELY when PnL is profit)
val ProfitGreen = Color(0xFF1E824C)
val ProfitGreenLight = Color(0xFF27A862)
val ProfitGreenDark = Color(0xFF145A32)
val ProfitGreenContainer = Color(0xFFEBF7F0)
val ProfitGreenBorder = Color(0xFFBCE6CF)

val RestrainedRed = Color(0xFFA83E3E)
val RestrainedRedContainer = Color(0xFFFDF1F1)
val RestrainedRedBorder = Color(0xFFF4D4D4)
val RedCritical = RestrainedRed
val RedCriticalContainer = RestrainedRedContainer

val BlueInfo = CyanSecondary
val BlueInfoContainer = CyanContainer

// Warm Paper-Texture Surfaces & Canvas (Non-fatiguing, gentle paper tone)
val PaperBackground = Color(0xFFF7F5F0)        // Subtle warm paper-texture canvas, never harsh #FFFFFF
val PaperBackgroundTint = Color(0xFFF3EFE8)    // Subtle contrast tint for texture gradient
val PaperCard = Color(0xFFFCFBF9)              // Soft warm off-white card surface
val PaperCardElevated = Color(0xFFF1EDE4)      // Gentle inset or elevated surface
val PaperBorder = Color(0xFFE5E1D7)            // Soft warm tactile border
val PaperBorderSubtle = Color(0xFFECE8DF)      // Delicate internal divider

// High Legibility Warm Charcoal Text (Avoids harsh pure black)
val TextPrimary = Color(0xFF1F242B)            // Deep warm charcoal
val TextSecondary = Color(0xFF5A6270)          // Balanced mid charcoal-slate
val TextMuted = Color(0xFF8B929E)              // Delicate supporting slate

// Aliases for compatibility
val BackgroundLight = PaperBackground
val SurfaceLight = PaperCard
val SurfaceElevatedLight = PaperCardElevated
val SurfaceBorderLight = PaperBorder
val SurfaceBorderSubtle = PaperBorderSubtle

val SlateBackground = PaperBackground
val SlateCard = PaperCard
val SlateCardElevated = PaperCardElevated
val SlateBorder = PaperBorder

// Dark Palette Retained for Fallback
val BackgroundDark = Color(0xFF14171C)
val SurfaceDark = Color(0xFF1C2026)
val SurfaceElevatedDark = Color(0xFF262B33)
val SurfaceBorderDark = Color(0xFF383F4A)



