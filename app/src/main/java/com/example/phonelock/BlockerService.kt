package com.example.phonelock

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.telecom.TelecomManager
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager

class BlockerService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        val prefs = Prefs(this)
        if (!prefs.locked()) return

        val dialer = getSystemService(TelecomManager::class.java).defaultDialerPackage
        // Keyboards raise their own window events; blocking them would lock you out while typing in an allowed app.
        val keyboards = getSystemService(InputMethodManager::class.java).enabledInputMethodList.map { it.packageName }.toSet()
        if (Policy.allows(pkg, packageName, dialer, keyboards, prefs.allowed)) return

        startActivity(Intent(this, LockActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun onInterrupt() {}
}
