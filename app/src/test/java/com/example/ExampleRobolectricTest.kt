package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AspectRatioFormat
import com.example.data.model.EmotionalPreset
import com.example.data.model.NarrativeTemplate
import com.example.data.model.PromoScene
import com.example.data.model.SceneVisualType
import com.example.data.model.TransitionStyle
import com.example.data.model.VoiceActor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("DevDirector", appName)
    }

    @Test
    fun `verify all 5 devdirector theme modes exist`() {
        val modes = com.example.ui.theme.AppThemeMode.entries
        assertEquals(5, modes.size)
        assertTrue(modes.contains(com.example.ui.theme.AppThemeMode.SYSTEM))
        assertTrue(modes.contains(com.example.ui.theme.AppThemeMode.STUDIO_LIGHT))
        assertTrue(modes.contains(com.example.ui.theme.AppThemeMode.STUDIO_DARK))
        assertTrue(modes.contains(com.example.ui.theme.AppThemeMode.NEON_DIRECTOR))
        assertTrue(modes.contains(com.example.ui.theme.AppThemeMode.CYBER_DIRECTOR))

        assertEquals(com.example.ui.theme.AppThemeMode.SYSTEM, com.example.ui.theme.AppThemeMode.fromId("system"))
        assertEquals(com.example.ui.theme.AppThemeMode.STUDIO_LIGHT, com.example.ui.theme.AppThemeMode.fromId("studio_light"))
        assertEquals(com.example.ui.theme.AppThemeMode.STUDIO_DARK, com.example.ui.theme.AppThemeMode.fromId("studio_dark"))
        assertEquals(com.example.ui.theme.AppThemeMode.NEON_DIRECTOR, com.example.ui.theme.AppThemeMode.fromId("neon_director"))
        assertEquals(com.example.ui.theme.AppThemeMode.CYBER_DIRECTOR, com.example.ui.theme.AppThemeMode.fromId("cyber_director"))
        assertEquals(com.example.ui.theme.AppThemeMode.SYSTEM, com.example.ui.theme.AppThemeMode.fromId("unknown_id"))
    }

    @Test
    fun `verify default promo scene structure`() {
        val scene = PromoScene(
            orderIndex = 0,
            title = "Stop Writing Videos by Hand",
            subtitle = "The Traditional Way is Broken",
            voiceover = "You spent months building something amazing. Now you have to spend weeks making a promo video?",
            emotionalPreset = EmotionalPreset.RAGE,
            durationSeconds = 4.0f,
            visualType = SceneVisualType.HOOK_FRUSTRATION,
            accentTag = "THE HOOK"
        )

        assertEquals("Stop Writing Videos by Hand", scene.title)
        assertEquals(EmotionalPreset.RAGE, scene.emotionalPreset)
        assertEquals(4.0f, scene.durationSeconds, 0.01f)
        assertEquals(SceneVisualType.HOOK_FRUSTRATION, scene.visualType)
    }

    @Test
    fun `verify aspect ratio formats`() {
        assertEquals(1920, AspectRatioFormat.LANDSCAPE_16_9.width)
        assertEquals(1080, AspectRatioFormat.LANDSCAPE_16_9.height)

        assertEquals(1080, AspectRatioFormat.PORTRAIT_9_16.width)
        assertEquals(1920, AspectRatioFormat.PORTRAIT_9_16.height)
    }

    @Test
    fun `verify voice actors and narrative templates`() {
        assertTrue(VoiceActor.values().isNotEmpty())
        assertTrue(NarrativeTemplate.values().isNotEmpty())
        assertTrue(TransitionStyle.values().contains(TransitionStyle.METALLIC_SWOOSH))
    }
}
