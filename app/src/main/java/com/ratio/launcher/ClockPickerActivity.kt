package com.ratio.launcher

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.ratio.launcher.utils.ClockStyle
import com.ratio.launcher.views.AnalogClockView
import com.ratio.launcher.views.FlipClockView

class ClockPickerActivity : AppCompatActivity() {

    private lateinit var list: RecyclerView
    private lateinit var nameLabel: TextView
    private lateinit var selectBtn: TextView
    private val styles = ClockStyle.entries
    private var selectedIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clock_picker)

        list = findViewById(R.id.clockPickerList)
        nameLabel = findViewById(R.id.clockPickerName)
        selectBtn = findViewById(R.id.clockPickerSelect)

        selectedIndex = ClockStyle.getCurrent(this).ordinal

        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        list.layoutManager = layoutManager
        list.adapter = ClockPreviewAdapter()

        // Snap to center
        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(list)

        // Scroll to current selection
        list.scrollToPosition(selectedIndex)
        nameLabel.text = styles[selectedIndex].displayName

        // Update label on scroll
        list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val snapped = snapHelper.findSnapView(layoutManager) ?: return
                    val pos = layoutManager.getPosition(snapped)
                    selectedIndex = pos
                    nameLabel.text = styles[pos].displayName
                }
            }
        })

        selectBtn.setOnClickListener {
            ClockStyle.setCurrent(this, styles[selectedIndex])
            finish()
        }
    }

    inner class ClockPreviewAdapter : RecyclerView.Adapter<ClockPreviewAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_clock_preview, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(styles[position])
        }

        override fun getItemCount(): Int = styles.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val container: FrameLayout = view.findViewById(R.id.clockPreviewContainer)
            private val label: TextView = view.findViewById(R.id.clockPreviewLabel)

            fun bind(style: ClockStyle) {
                container.removeAllViews()
                label.text = style.displayName

                val prefs = getSharedPreferences("ratio_prefs", MODE_PRIVATE)
                val use24h = prefs.getBoolean("clock_24h", true)
                val showSeconds = prefs.getBoolean("show_seconds", false)

                when (style) {
                    ClockStyle.ANALOG -> {
                        val clock = AnalogClockView(this@ClockPickerActivity).apply {
                            this.showSeconds = showSeconds
                        }
                        val params = FrameLayout.LayoutParams(400, 400).apply {
                            gravity = Gravity.CENTER
                        }
                        container.addView(clock, params)
                    }
                    ClockStyle.FLIP -> {
                        val clock = FlipClockView(this@ClockPickerActivity).apply {
                            setUse24h(use24h)
                            setShowSeconds(showSeconds)
                        }
                        val params = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT
                        ).apply { gravity = Gravity.CENTER }
                        container.addView(clock, params)
                    }
                    else -> {
                        val textView = TextView(this@ClockPickerActivity).apply {
                            text = style.formatTime(use24h, showSeconds)
                            setTextColor(resources.getColor(R.color.ratio_white, null))
                            gravity = Gravity.CENTER
                            when (style) {
                                ClockStyle.MINIMAL -> {
                                    textSize = 42f
                                    typeface = resources.getFont(R.font.ratio_light)
                                    letterSpacing = -0.02f
                                }
                                ClockStyle.BOLD -> {
                                    textSize = 48f
                                    typeface = resources.getFont(R.font.inter_semibold)
                                    letterSpacing = -0.04f
                                }
                                ClockStyle.WORD -> {
                                    textSize = 20f
                                    typeface = resources.getFont(R.font.inter_light)
                                }
                                ClockStyle.BINARY -> {
                                    textSize = 18f
                                    typeface = android.graphics.Typeface.MONOSPACE
                                    letterSpacing = 0.1f
                                }
                                ClockStyle.BAR -> {
                                    textSize = 12f
                                    typeface = android.graphics.Typeface.MONOSPACE
                                }
                                else -> textSize = 36f
                            }
                        }
                        val params = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        ).apply { gravity = Gravity.CENTER }
                        container.addView(textView, params)
                    }
                }
            }
        }
    }
}
