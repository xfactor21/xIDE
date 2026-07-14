package com.aistudio.xide.core.designsystem

import com.aistudio.xide.core.designsystem.tokens.ColorTokens
import com.aistudio.xide.core.designsystem.tokens.TypographyTokens
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ThemeValidationTest {

    @Test
    fun testColorTokensExist() {
        assertNotNull(ColorTokens.Primary)
        assertNotNull(ColorTokens.Background)
        assertNotNull(ColorTokens.Surface)
    }

    @Test
    fun testTypographyTokensExist() {
        assertNotNull(TypographyTokens.DisplayLarge)
        assertNotNull(TypographyTokens.BodyMedium)
    }
}
