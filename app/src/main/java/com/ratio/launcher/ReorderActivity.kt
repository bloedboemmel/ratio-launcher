package com.ratio.launcher

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ratio.launcher.utils.CategoryOrder
import com.ratio.launcher.utils.RootCardOrder

class ReorderActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MODE = "mode"
        const val MODE_CATEGORIES = "categories"
        const val MODE_CARDS = "cards"
    }

    private lateinit var items: MutableList<String>
    private lateinit var adapter: ReorderAdapter
    private lateinit var touchHelper: ItemTouchHelper
    private var mode = MODE_CARDS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reorder)

        mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_CARDS

        val title = findViewById<TextView>(R.id.reorderTitle)
        val list = findViewById<RecyclerView>(R.id.reorderList)
        val doneBtn = findViewById<TextView>(R.id.reorderDone)

        when (mode) {
            MODE_CARDS -> {
                title.text = "Reorder Cards"
                val order = RootCardOrder.getOrder(this)
                val displayNames = mapOf(
                    "screen_time" to "Screen Time",
                    "media" to "Now Playing",
                    "weather" to "Weather",
                    "calendar" to "Calendar",
                    "notes" to "Notes"
                )
                items = order.toMutableList()
                adapter = ReorderAdapter(items.map { displayNames[it] ?: it }.toMutableList())
            }
            MODE_CATEGORIES -> {
                title.text = "Reorder Categories"
                val currentOrder = CategoryOrder.getOrder(this)
                items = if (currentOrder != null) {
                    currentOrder.toMutableList()
                } else {
                    getAppCategories().toMutableList()
                }
                adapter = ReorderAdapter(items.toMutableList())
            }
            else -> {
                items = mutableListOf()
                adapter = ReorderAdapter(mutableListOf())
            }
        }

        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.bindingAdapterPosition
                val to = target.bindingAdapterPosition
                adapter.moveItem(from, to)
                items.add(to, items.removeAt(from))
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun isLongPressDragEnabled(): Boolean = true
        }

        touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(list)

        adapter.setDragStartListener { viewHolder ->
            touchHelper.startDrag(viewHolder)
        }

        doneBtn.setOnClickListener {
            saveOrder()
            finish()
        }
    }

    private fun saveOrder() {
        when (mode) {
            MODE_CARDS -> RootCardOrder.setOrder(this, items)
            MODE_CATEGORIES -> CategoryOrder.setOrder(this, items)
        }
    }

    private fun getAppCategories(): List<String> {
        val pm = packageManager
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        val customPrefs = getSharedPreferences("ratio_app_categories", MODE_PRIVATE)
        val categories = pm.queryIntentActivities(intent, 0)
            .map { ri ->
                val pkg = ri.activityInfo.packageName
                customPrefs.getString(pkg, null) ?: "Other"
            }
            .distinct()
            .toMutableList()

        val defaults = listOf("Communication", "Media", "Social", "Games", "Productivity",
            "Finance", "Shopping", "Travel", "Health", "System", "Other")
        defaults.forEach { if (!categories.contains(it)) categories.add(it) }

        return categories.sorted()
    }
}

class ReorderAdapter(
    private val labels: MutableList<String>
) : RecyclerView.Adapter<ReorderAdapter.ViewHolder>() {

    private var dragListener: ((RecyclerView.ViewHolder) -> Unit)? = null

    fun setDragStartListener(listener: (RecyclerView.ViewHolder) -> Unit) {
        dragListener = listener
    }

    fun moveItem(from: Int, to: Int) {
        labels.add(to, labels.removeAt(from))
        notifyItemMoved(from, to)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reorder, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(labels[position])
    }

    override fun getItemCount(): Int = labels.size

    @SuppressLint("ClickableViewAccessibility")
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val label: TextView = view.findViewById(R.id.reorderLabel)
        private val handle: TextView = view.findViewById(R.id.reorderHandle)

        fun bind(text: String) {
            label.text = text
            handle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    dragListener?.invoke(this)
                }
                false
            }
        }
    }
}
