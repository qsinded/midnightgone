package com.example.phonelock

import android.app.Activity
import android.app.TimePickerDialog
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ResolveInfo
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.telecom.TelecomManager
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var banner: TextView
    private lateinit var guide: TextView
    private lateinit var battery: TextView
    private lateinit var list: LinearLayout
    private lateinit var count: TextView
    private lateinit var dial: DialView
    private lateinit var apps: List<ResolveInfo>
    private var query = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = C.BG
        window.navigationBarColor = C.BG
        prefs = Prefs(this)
        apps = packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
        ).filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(C.BG)
            setPadding(dp(18), dp(26), dp(18), dp(18))
        }
        root.addView(label("PhoneLock", 21f, C.TEXT, true).apply { setPadding(dp(4), 0, 0, dp(12)) })

        // Status doubles as the "enable" action, so the first-run step is the first thing you see.
        banner = label("", 15f, C.MINT).apply {
            gravity = Gravity.CENTER_VERTICAL
            minHeight = dp(48)
            setPadding(dp(16), 0, dp(16), 0)
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        root.addView(banner)

        // First-run help, shown only while the blocker is off. Kept to two lines so the app list isn't squeezed out.
        guide = label(
            "Tap above, then switch on “PhoneLock blocker”. Greyed out? Settings › Apps › PhoneLock › ⋮ › Allow restricted settings.",
            13f, C.MUTED
        ).apply {
            background = rounded(C.SURFACE, 16)
            setPadding(dp(16), dp(10), dp(16), dp(10))
            visibility = View.GONE
        }
        root.addView(guide, lp(top = dp(8)))

        // Some phones kill background services to save battery, which would silently end the lock.
        battery = label("Keep the lock reliable: set PhoneLock to “Don’t optimize” in battery settings ›", 13f, C.AMBER).apply {
            background = rounded(C.SURFACE, 16)
            setPadding(dp(16), dp(10), dp(16), dp(10))
            visibility = View.GONE
            setOnClickListener { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
        }
        root.addView(battery, lp(top = dp(8)))

        val dialCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = rounded(C.SURFACE, 24)
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }
        dial = DialView(this).apply { setWindow(prefs.start, prefs.end) }
        dialCard.addView(dial, LinearLayout.LayoutParams(dp(196), dp(196)))
        val tiles = LinearLayout(this)
        tiles.addView(timeTile("Starts", { prefs.start }, { prefs.start = it }), lp(0, WRAP, 1f))
        tiles.addView(timeTile("Ends", { prefs.end }, { prefs.end = it }), lp(0, WRAP, 1f).apply { leftMargin = dp(10) })
        dialCard.addView(tiles, lp(top = dp(10)))
        root.addView(dialCard, lp(top = dp(12)))

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(C.SURFACE, 24)
            setPadding(dp(16), dp(16), dp(16), 0)
        }
        val head = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        head.addView(label("Emergency apps", 16f, C.TEXT, true), lp(0, WRAP, 1f))
        count = label("", 13f, C.MUTED)
        head.addView(count)
        card.addView(head)
        card.addView(EditText(this).apply {
            hint = "Search apps"
            setHintTextColor(C.MUTED)
            setTextColor(C.TEXT)
            textSize = 15f
            isSingleLine = true
            background = rounded(C.BG, 14)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) { query = s.toString(); render() }
            })
        }, lp(top = dp(10)))
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        card.addView(ScrollView(this).apply { addView(list) }, lp(MATCH, 0, 1f, top = dp(6)))
        root.addView(card, lp(MATCH, 0, 1f, top = dp(12)))

        setContentView(root)
        render()
    }

    override fun onResume() {
        super.onResume()
        val cn = ComponentName(this, BlockerService::class.java)
        val on = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?.let { it.contains(cn.flattenToString()) || it.contains(cn.flattenToShortString()) } == true
        banner.text = if (on) "Blocker is active" else "Blocker off – tap to enable"
        banner.setTextColor(if (on) C.MINT else C.ON_AMBER)
        banner.setTypeface(null, if (on) Typeface.NORMAL else Typeface.BOLD)
        banner.background = rounded(if (on) Color.parseColor("#10261F") else C.AMBER, 16)
        guide.visibility = if (on) View.GONE else View.VISIBLE
        val exempt = getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)
        battery.visibility = if (on && !exempt) View.VISIBLE else View.GONE
    }

    // Edits are refused during a lock; otherwise anyone could open this app and shorten the lock or add apps.
    // ponytail: edits are still open before a lock starts, so a determined user can move the window ahead of time.
    private fun lockedNotice(): Boolean {
        if (!prefs.locked()) return false
        Toast.makeText(this, "Locked until ${Schedule.fmt(prefs.end)}. Settings can't be changed now.", Toast.LENGTH_SHORT).show()
        return true
    }

    private fun timeTile(title: String, get: () -> Int, set: (Int) -> Unit): View {
        val time = label(Schedule.fmt(get()), 24f, C.TEXT, true)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(label(title, 12f, C.MUTED))
            addView(time)
        }
        return LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(64)
            background = rounded(C.SURFACE2, 16)
            setPadding(dp(14), 0, dp(14), 0)
            addView(col, lp(0, WRAP, 1f))
            addView(label("›", 24f, C.MUTED)) // chevron = tappable
            setOnClickListener {
                if (lockedNotice()) return@setOnClickListener
                TimePickerDialog(this@MainActivity, android.R.style.Theme_DeviceDefault_Dialog_Alert, { _, h, m ->
                    // Re-check: the lock may have started while the dialog was open.
                    if (!prefs.locked()) {
                        set(h * 60 + m)
                        time.text = Schedule.fmt(get())
                        dial.setWindow(prefs.start, prefs.end)
                    }
                }, get() / 60, get() % 60, true).show()
            }
        }
    }

    private fun row(icon: Drawable?, name: String, sub: String?, trailing: View) = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(60)
        addView(ImageView(context).apply { setImageDrawable(icon) }, LinearLayout.LayoutParams(dp(40), dp(40)))
        val names = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(label(name, 16f, C.TEXT))
            if (sub != null) addView(label(sub, 12f, C.MUTED))
        }
        addView(names, lp(0, WRAP, 1f).apply { leftMargin = dp(12) })
        addView(trailing)
    }

    private fun updateCount() { count.text = "${prefs.allowed.size} of $MAX_EMERGENCY" }

    // Chosen apps first, then alphabetical (the sort is stable), filtered by the search box.
    private fun render() {
        list.removeAllViews()
        val chosen = prefs.allowed
        updateCount()

        val dialer = getSystemService(TelecomManager::class.java).defaultDialerPackage
        val dialerIcon = dialer?.let { runCatching { packageManager.getApplicationIcon(it) }.getOrNull() }
        if (dialerIcon != null) list.addView(row(dialerIcon, "Phone", "Always allowed", label("✓", 20f, C.MINT, true)))

        apps.filter { it.loadLabel(packageManager).toString().contains(query, ignoreCase = true) }
            .sortedByDescending { it.activityInfo.packageName in chosen }
            .forEach { app ->
                val pkg = app.activityInfo.packageName
                val box = CheckBox(this).apply {
                    buttonTintList = ColorStateList(
                        arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                        intArrayOf(C.AMBER, Color.parseColor("#6A77A3"))
                    )
                    isChecked = pkg in chosen
                    setOnCheckedChangeListener { b, on ->
                        if (lockedNotice()) { list.post { render() }; return@setOnCheckedChangeListener } // redraw puts the checkbox back
                        val cur = prefs.allowed
                        if (on && pkg !in cur && cur.size >= MAX_EMERGENCY) {
                            b.isChecked = false
                            Toast.makeText(this@MainActivity, "Up to $MAX_EMERGENCY emergency apps", Toast.LENGTH_SHORT).show()
                            return@setOnCheckedChangeListener
                        }
                        prefs.allowed = if (on) cur + pkg else cur - pkg
                        updateCount()
                    }
                }
                list.addView(row(app.loadIcon(packageManager), app.loadLabel(packageManager).toString(), null, box)
                    .apply { setOnClickListener { box.toggle() } })
            }
    }

    companion object { const val MAX_EMERGENCY = 8 }
}
