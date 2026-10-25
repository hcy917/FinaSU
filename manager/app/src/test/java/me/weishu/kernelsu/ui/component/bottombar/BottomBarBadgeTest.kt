package me.weishu.kernelsu.ui.component.bottombar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BottomBarBadgeTest {
    @Test
    fun `module destination prioritises updatable modules`() {
        val state = NavigationBadgeState(moduleEnabledCount = 4, moduleUpdatableCount = 1)

        assertEquals(
            NavBadge(1, BadgeTone.Alert),
            badgeFor(BottomBarDestination.Module, state),
        )
    }

    @Test
    fun `module destination falls back to the enabled count`() {
        val state = NavigationBadgeState(moduleEnabledCount = 4)

        assertEquals(
            NavBadge(4, BadgeTone.Accent),
            badgeFor(BottomBarDestination.Module, state),
        )
    }

    @Test
    fun `module destination has no badge when nothing is enabled`() {
        assertNull(badgeFor(BottomBarDestination.Module, NavigationBadgeState()))
    }
}
