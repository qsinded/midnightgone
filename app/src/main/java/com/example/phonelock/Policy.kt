package com.example.phonelock

/** Decides which foreground app may stay open during a lock. Pure, so it can be unit tested. */
object Policy {
    // System surfaces that must never be blocked: system UI, call screens (so emergency calls work),
    // permission prompts and system dialogs. The home screen is deliberately NOT here, so Home returns to the lock screen.
    private val SYSTEM = setOf(
        "android", "com.android.systemui", "com.android.incallui", "com.android.phone",
        "com.android.server.telecom", "com.android.emergency",
        "com.android.permissioncontroller", "com.google.android.permissioncontroller"
    )

    fun allows(pkg: String, own: String, dialer: String?, keyboards: Set<String>, chosen: Set<String>) =
        pkg == own || pkg == dialer || pkg in SYSTEM || pkg in keyboards || pkg in chosen
}
