package com.aistudio.xide.core.designsystem.tokens

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween

object MotionTokens {
    val EasingStandard: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EasingStandardDecelerate: Easing = CubicBezierEasing(0.0f, 0.0f, 0.0f, 1.0f)
    val EasingStandardAccelerate: Easing = CubicBezierEasing(0.3f, 0.0f, 1.0f, 1.0f)

    val DurationShort1 = 50
    val DurationShort2 = 100
    val DurationShort3 = 150
    val DurationMedium1 = 200
    val DurationMedium2 = 250
    val DurationMedium3 = 300
    val DurationLong1 = 400
    val DurationLong2 = 500

    fun standardTween() = tween<Float>(durationMillis = DurationMedium2, easing = EasingStandard)
}
