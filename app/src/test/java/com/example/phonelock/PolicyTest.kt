package com.example.phonelock

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PolicyTest {
    private fun allows(pkg: String, dialer: String? = "com.dialer") =
        Policy.allows(pkg, "com.example.phonelock", dialer, setOf("com.keyboard"), setOf("com.chat"))

    @Test fun allowsOwnDialerKeyboardSystemAndChosen() {
        for (p in listOf("com.example.phonelock", "com.dialer", "com.keyboard", "com.android.systemui", "com.android.incallui", "com.chat"))
            assertTrue(p, allows(p))
    }

    @Test fun blocksEverythingElse() {
        // Settings must stay blocked (it can switch the blocker off); the launcher too, so Home returns to the lock screen.
        for (p in listOf("com.instagram.android", "com.android.settings", "com.google.android.apps.nexuslauncher"))
            assertFalse(p, allows(p))
    }

    @Test fun noDefaultDialerAllowsNothingExtra() {
        assertFalse(allows("com.dialer", dialer = null))
    }
}
