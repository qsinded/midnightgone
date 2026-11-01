package com.example.phonelock

import android.content.Context
import java.util.Calendar

object Schedule {
    /** All times are minutes since midnight. Handles windows that cross midnight (22:00-06:00). */
    fun isLocked(now: Int, start: Int, end: Int): Boolean = when {
        start == end -> false
        start < end -> now in start until end
        else -> now >= start || now < end
    }

    fun nowMinutes(): Int = Calendar.getInstance().let { it[Calendar.HOUR_OF_DAY] * 60 + it[Calendar.MINUTE] }

    fun lockedMinutes(start: Int, end: Int) = (end - start + 1440) % 1440

    fun fmtDur(min: Int) = when {
        min == 0 -> "Off"
        min % 60 == 0 -> "${min / 60}h"
        else -> "${min / 60}h ${min % 60}m"
    }

    /** Fraction of the lock window already elapsed; 0 when not locked. */
    fun progress(now: Int, start: Int, end: Int): Float =
        if (!isLocked(now, start, end)) 0f
        else ((now - start + 1440) % 1440).toFloat() / lockedMinutes(start, end)

    fun fmt(min: Int) = "${(min / 60).toString().padStart(2, '0')}:${(min % 60).toString().padStart(2, '0')}"
}

class Prefs(c: Context) {
    private val p = c.getSharedPreferences("phonelock", Context.MODE_PRIVATE)

    var start: Int
        get() = p.getInt("start", 22 * 60)
        set(v) = p.edit().putInt("start", v).apply()

    var end: Int
        get() = p.getInt("end", 6 * 60)
        set(v) = p.edit().putInt("end", v).apply()

    // Copy: the set returned by getStringSet must not be mutated.
    var allowed: Set<String>
        get() = HashSet(p.getStringSet("allowed", emptySet())!!)
        set(v) = p.edit().putStringSet("allowed", v).apply()

    fun locked() = Schedule.isLocked(Schedule.nowMinutes(), start, end)
}
