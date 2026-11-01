package com.example.phonelock

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telecom.TelecomManager
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView

class LockActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var ring: RingView
    private val handler = Handler(Looper.getMainLooper())

    // Closes the screen when the lock window ends, and keeps the ring current while it is open.
    private val tick = object : Runnable {
        override fun run() {
            if (!prefs.locked()) { finish(); return }
            ring.progress = Schedule.progress(Schedule.nowMinutes(), prefs.start, prefs.end)
            handler.postDelayed(this, 15_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = C.BG
        window.navigationBarColor = C.BG
        prefs = Prefs(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(C.BG)
            setPadding(dp(22), dp(56), dp(22), dp(28))
        }
        ring = RingView(this, Schedule.fmt(prefs.end), Schedule.progress(Schedule.nowMinutes(), prefs.start, prefs.end))
        root.addView(ring, LinearLayout.LayoutParams(dp(260), dp(260)))
        root.addView(label("Take a break.", 26f, C.TEXT, true).apply { gravity = Gravity.CENTER }, lp(top = dp(22)))
        root.addView(label("Emergency apps are below.", 16f, C.MUTED).apply { gravity = Gravity.CENTER }, lp(top = dp(6)))
        root.addView(View(this), lp(MATCH, 0, 1f)) // pushes the buttons to the bottom when there is room

        // Emergency path: dialer first (amber), then the user's chosen apps. An app uninstalled since it was
        // picked is skipped rather than crashing the lock screen.
        val dialer = getSystemService(TelecomManager::class.java).defaultDialerPackage
        val emergency = listOfNotNull(dialer) + prefs.allowed
        for ((i, pkg) in emergency.withIndex()) {
            val launch = packageManager.getLaunchIntentForPackage(pkg) ?: continue
            val primary = i == 0 && pkg == dialer
            val (name, icon) = try {
                Pair(
                    if (primary) "Phone" else packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString(),
                    packageManager.getApplicationIcon(pkg)
                )
            } catch (e: PackageManager.NameNotFoundException) {
                continue
            }
            val btn = label(name, if (primary) 18f else 16f, if (primary) C.ON_AMBER else C.TEXT, primary).apply {
                gravity = Gravity.CENTER
                minHeight = dp(if (primary) 64 else 60)
                background = if (primary) rounded(C.AMBER, 20) else rounded(C.SURFACE, 18, C.LINE)
                if (!primary) {
                    icon.setBounds(0, 0, dp(28), dp(28))
                    setCompoundDrawablesRelative(icon, null, null, null)
                    compoundDrawablePadding = dp(10)
                }
                setOnClickListener { startActivity(launch) }
            }
            root.addView(btn, lp(top = dp(10)))
        }
        setContentView(ScrollView(this).apply { isFillViewport = true; addView(root) })
    }

    override fun onResume() {
        super.onResume()
        tick.run() // also closes at once if the lock ended while the screen was in the background
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(tick)
    }

    // Back would just return to the blocked app, which relocks; only let it through once the lock is over.
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (!prefs.locked()) finish()
    }
}
