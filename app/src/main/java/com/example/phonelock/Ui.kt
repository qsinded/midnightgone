package com.example.phonelock

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.cos
import kotlin.math.sin

object C {
    val BG = Color.parseColor("#0B1020")
    val SURFACE = Color.parseColor("#151C33")
    val SURFACE2 = Color.parseColor("#1F2849")
    val LINE = Color.parseColor("#222C52")
    val TEXT = Color.parseColor("#F2F4FA")
    val MUTED = Color.parseColor("#9AA5C7")
    val AMBER = Color.parseColor("#FFB547")
    val ON_AMBER = Color.parseColor("#1A1405")
    val MINT = Color.parseColor("#6EE7B7")
}

const val MATCH = LinearLayout.LayoutParams.MATCH_PARENT
const val WRAP = LinearLayout.LayoutParams.WRAP_CONTENT

fun Context.dp(v: Int) = (v * resources.displayMetrics.density).toInt()

fun Context.rounded(color: Int, radiusDp: Int, stroke: Int? = null) = GradientDrawable().apply {
    setColor(color)
    cornerRadius = dp(radiusDp).toFloat()
    if (stroke != null) setStroke(1, stroke)
}

fun Context.label(s: String, sp: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
    text = s
    textSize = sp
    setTextColor(color)
    if (bold) setTypeface(typeface, Typeface.BOLD)
}

fun lp(w: Int = MATCH, h: Int = WRAP, weight: Float = 0f, top: Int = 0) =
    LinearLayout.LayoutParams(w, h, weight).apply { topMargin = top }

private fun paint(c: Int, stroke: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = c
    style = if (stroke) Paint.Style.STROKE else Paint.Style.FILL
    textAlign = Paint.Align.CENTER
}

/** 24-hour dial: the locked window is an amber arc. 1440 minutes = 360 degrees, 00:00 at the top. */
class DialView(c: Context) : View(c) {
    private var start = 0
    private var end = 0
    private val track = paint(C.SURFACE2, stroke = true)
    private val arc = paint(C.AMBER, stroke = true).apply { strokeCap = Paint.Cap.ROUND }
    private val fill = paint(C.AMBER)
    private val hole = paint(C.BG)
    private val tick = paint(Color.parseColor("#8490B8"))
    private val big = paint(C.TEXT).apply { typeface = Typeface.DEFAULT_BOLD }
    private val small = paint(C.MUTED)

    fun setWindow(s: Int, e: Int) {
        start = s
        end = e
        contentDescription = "Locked from ${Schedule.fmt(s)} to ${Schedule.fmt(e)}, ${Schedule.fmtDur(Schedule.lockedMinutes(s, e))}"
        invalidate()
    }

    override fun onDraw(cv: Canvas) {
        val d = resources.displayMetrics.density
        val cx = width / 2f
        val cy = height / 2f
        val r = minOf(width, height) / 2f - 12 * d
        track.strokeWidth = 14 * d
        arc.strokeWidth = 14 * d
        cv.drawCircle(cx, cy, r, track)

        val mins = Schedule.lockedMinutes(start, end)
        if (mins > 0) {
            cv.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), start / 4f - 90f, mins / 4f, false, arc)
            for (m in intArrayOf(start, end)) {
                val a = Math.toRadians((m / 4f - 90f).toDouble())
                val x = cx + r * cos(a).toFloat()
                val y = cy + r * sin(a).toFloat()
                cv.drawCircle(x, y, 9 * d, fill)
                cv.drawCircle(x, y, 5 * d, hole)
            }
        }

        tick.textSize = 11 * d
        for (h in intArrayOf(0, 6, 12, 18)) {
            val a = Math.toRadians((h * 15 - 90).toDouble())
            val tr = r - 26 * d
            cv.drawText(h.toString().padStart(2, '0'), cx + tr * cos(a).toFloat(), cy + tr * sin(a).toFloat() + 4 * d, tick)
        }
        big.textSize = 44 * d
        small.textSize = 12 * d
        cv.drawText(Schedule.fmtDur(mins), cx, cy + 12 * d, big)
        cv.drawText("locked daily", cx, cy + 34 * d, small)
    }
}

/** Lock-screen ring: amber arc = how much of the lock window has passed. */
class RingView(c: Context, private val until: String, initial: Float) : View(c) {
    var progress = initial
        set(v) { field = v; invalidate() }

    private val outer = paint(C.LINE, stroke = true)
    private val track = paint(C.SURFACE2, stroke = true)
    private val arc = paint(C.AMBER, stroke = true).apply { strokeCap = Paint.Cap.ROUND }
    private val cap = paint(C.MUTED).apply { letterSpacing = 0.15f }
    private val time = paint(C.AMBER).apply { typeface = Typeface.DEFAULT_BOLD }

    init { contentDescription = "Locked until $until" }

    override fun onDraw(cv: Canvas) {
        val d = resources.displayMetrics.density
        val cx = width / 2f
        val cy = height / 2f
        val half = minOf(width, height) / 2f
        outer.strokeWidth = 2 * d
        track.strokeWidth = 12 * d
        arc.strokeWidth = 12 * d
        cv.drawCircle(cx, cy, half - 2 * d, outer)
        val r = half - 26 * d
        cv.drawCircle(cx, cy, r, track)
        cv.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), -90f, progress * 360f, false, arc)
        cap.textSize = 13 * d
        time.textSize = 56 * d
        cv.drawText("LOCKED UNTIL", cx, cy - 14 * d, cap)
        cv.drawText(until, cx, cy + 40 * d, time)
    }
}
