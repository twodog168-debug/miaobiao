package com.example.stopwatch

import android.app.Activity
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue

class MainActivity : Activity() {
    private val bg = Color.rgb(247, 247, 247)
    private val blue = Color.rgb(38, 137, 232)
    private val dark = Color.rgb(48, 48, 48)

    // ✅ 枚举类放在这里
    enum class Icon { PLAY, PAUSE, LAP, RESET }

    private lateinit var timeText: TextView
    private lateinit var lapContainer: LinearLayout
    private lateinit var leftButton: IconButton
    private lateinit var rightButton: IconButton

    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var elapsedBeforeStart = 0L
    private var startRealtime = 0L
    private var lastLapElapsed = 0L
    private var lapNumber = 0
    private val laps = mutableListOf<Pair<Int, Long>>()

    private val ticker = object : Runnable {
        override fun run() {
            if (running) {
                updateTime()
                handler.postDelayed(this, 30L)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        buildUi()
        renderButtons()
        updateTime()
    }

    private fun buildUi() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(dp(22), dp(18), dp(22), dp(12))
        }

        val header = TextView(this).apply {
            text = "秒表"
            setTextColor(Color.BLACK)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f)
            gravity = Gravity.CENTER_VERTICAL
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        }
        page.addView(header, LinearLayout.LayoutParams(-1, dp(64)))

        // ✅ 使用 FrameLayout 让时间绝对居中，不受下方计次列表影响
        val centerArea = FrameLayout(this)
        val centerParams = LinearLayout.LayoutParams(-1, 0, 1f)

        timeText = TextView(this).apply {
            text = "00:00:00.00"
            setTextColor(dark)
            gravity = Gravity.CENTER
            includeFontPadding = false
            textSize = 55f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            maxLines = 1
            setSingleLine(true)
            
            // ✅ 动态自适应字号：防止粗体太宽超出屏幕
            post {
                val availableWidth = width - paddingLeft - paddingRight
                if (availableWidth > 0) {
                    var currentSize = 55f
                    var textWidth = paint.measureText(text.toString())
                    while (textWidth > availableWidth && currentSize > 12f) {
                        currentSize -= 1f
                        textSize = currentSize
                        textWidth = paint.measureText(text.toString())
                    }
                }
            }
        }
        
        // ✅ 将时间绝对固定在正中间
        centerArea.addView(timeText, FrameLayout.LayoutParams(-1, -2).apply {
            gravity = Gravity.CENTER
        })

        val scroll = ScrollView(this).apply {
            isFillViewport = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        lapContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(10), dp(8), dp(10))
        }
        scroll.addView(lapContainer)
        
        // ✅ 计次列表放在下半区（限高，防止圈数多时遮挡时间）
        centerArea.addView(scroll, FrameLayout.LayoutParams(-1, dp(220)).apply {
            gravity = Gravity.BOTTOM
        })

        page.addView(centerArea, centerParams)

        val controls = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.HORIZONTAL
        }
        leftButton = IconButton(this)
        rightButton = IconButton(this)
        leftButton.setOnClickListener { onLeftAction() }
        rightButton.setOnClickListener { onRightAction() }

        controls.addView(leftButton, LinearLayout.LayoutParams(dp(92), dp(92)).apply {
            rightMargin = dp(48)
        })
        controls.addView(rightButton, LinearLayout.LayoutParams(dp(92), dp(92)))
        page.addView(controls, LinearLayout.LayoutParams(-1, dp(132)))

        setContentView(page)
    }

    private fun onLeftAction() {
        if (running) {
            val now = currentElapsed()
            lapNumber++
            laps.add(0, lapNumber to (now - lastLapElapsed))
            lastLapElapsed = now
            renderLaps()
        } else if (elapsedBeforeStart > 0L) {
            resetStopwatch()
        }
    }

    private fun onRightAction() {
        if (running) {
            elapsedBeforeStart = currentElapsed()
            running = false
            handler.removeCallbacks(ticker)
        } else {
            startRealtime = SystemClock.elapsedRealtime() - elapsedBeforeStart
            running = true
            handler.removeCallbacks(ticker)
            handler.post(ticker)
        }
        renderButtons()
        updateTime()
    }

    private fun resetStopwatch() {
        running = false
        handler.removeCallbacks(ticker)
        elapsedBeforeStart = 0L
        startRealtime = 0L
        lastLapElapsed = 0L
        lapNumber = 0
        laps.clear()
        renderLaps()
        renderButtons()
        updateTime()
    }

    private fun currentElapsed(): Long =
        if (running) SystemClock.elapsedRealtime() - startRealtime else elapsedBeforeStart

    private fun updateTime() {
        val ms = currentElapsed().coerceAtLeast(0L)
        val hours = ms / 3_600_000L
        val minutes = (ms / 60_000L) % 60
        val seconds = (ms / 1_000L) % 60
        val centiseconds = (ms % 1_000L) / 10L
        timeText.text = String.format("%02d:%02d:%02d.%02d", hours, minutes, seconds, centiseconds)
    }

    private fun renderButtons() {
        if (running) {
            leftButton.icon = Icon.LAP
            leftButton.contentDescription = "计次"
            rightButton.icon = Icon.PAUSE
            rightButton.contentDescription = "暂停"
            leftButton.visibility = View.VISIBLE
        } else if (elapsedBeforeStart > 0L) {
            leftButton.icon = Icon.RESET
            leftButton.contentDescription = "重置"
            rightButton.icon = Icon.PLAY
            rightButton.contentDescription = "继续"
            leftButton.visibility = View.VISIBLE
        } else {
            leftButton.visibility = View.GONE
            rightButton.icon = Icon.PLAY
            rightButton.contentDescription = "开始"
        }
        leftButton.invalidate()
        rightButton.invalidate()
    }

    private fun renderLaps() {
        lapContainer.removeAllViews()
        if (laps.isEmpty()) return
        val best = laps.minByOrNull { it.second }?.first
        val worst = laps.maxByOrNull { it.second }?.first
        laps.forEach { (number, duration) ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(8), dp(10), dp(8), dp(10))
            }
            val label = TextView(this).apply {
                text = "第 $number 次"
                textSize = 16f
                setTextColor(when (number) {
                    best -> Color.rgb(30, 150, 90)
                    worst -> Color.rgb(220, 80, 70)
                    else -> dark
                })
            }
            val value = TextView(this).apply {
                text = formatDuration(duration)
                textSize = 16f
                setTextColor(dark)
                typeface = Typeface.MONOSPACE
                gravity = Gravity.RIGHT
            }
            row.addView(label, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(value, LinearLayout.LayoutParams(-2, -2))
            lapContainer.addView(row)
            val line = View(this).apply { setBackgroundColor(0x1F000000) }
            lapContainer.addView(line, LinearLayout.LayoutParams(-1, dp(1)))
        }
    }

    private fun formatDuration(ms: Long): String {
        val m = (ms / 60_000L)
        val s = (ms / 1_000L) % 60
        val cs = (ms % 1_000L) / 10L
        return String.format("%02d:%02d.%02d", m, s, cs)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

    private inner class IconButton(context: android.content.Context) : View(context) {
        var icon: Icon = Icon.PLAY

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = blue
            style = Paint.Style.FILL
        }
        private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height / 2f
            val radius = minOf(width, height) * 0.47f
            shadowPaint.setShadowLayer(dp(7).toFloat(), 0f, dp(5).toFloat(), 0x18000000)
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            canvas.drawCircle(cx, cy, radius, shadowPaint)
            shadowPaint.clearShadowLayer()

            val scale = radius / 46f
            canvas.save()
            canvas.translate(cx, cy)
            canvas.scale(scale, scale)
            when (icon) {
                Icon.PLAY -> {
                    val p = Path().apply {
                        moveTo(-10f, -17f); lineTo(18f, 0f); lineTo(-10f, 17f); close()
                    }
                    canvas.drawPath(p, paint)
                }
                Icon.PAUSE -> {
                    val r = RectF(-14f, -17f, -5f, 17f)
                    canvas.drawRoundRect(r, 2f, 2f, paint)
                    val r2 = RectF(5f, -17f, 14f, 17f)
                    canvas.drawRoundRect(r2, 2f, 2f, paint)
                }
                Icon.LAP -> {
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 5f
                    canvas.drawLine(-8f, -19f, -8f, 18f, paint)
                    paint.style = Paint.Style.FILL
                    val p = Path().apply {
                        moveTo(-5f, -17f); lineTo(17f, 0f); lineTo(-5f, 0f); close()
                    }
                    canvas.drawPath(p, paint)
                }
                Icon.RESET -> {
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 5f
                    val oval = RectF(-14f, -14f, 14f, 14f)
                    canvas.drawArc(oval, -55f, 285f, false, paint)
                    paint.style = Paint.Style.FILL
                    val p = Path().apply {
                        moveTo(8f, -20f); lineTo(19f, -17f); lineTo(11f, -8f); close()
                    }
                    canvas.drawPath(p, paint)
                }
            }
            paint.style = Paint.Style.FILL
            canvas.restore()
        }
    }
}
