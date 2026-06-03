package com.ratio.launcher.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ratio.launcher.R
import com.ratio.launcher.models.Note

class NotesAdapter(
    private val notes: MutableList<Note>,
    private val onDelete: (Note) -> Unit,
    private val onTap: ((Note) -> Unit)? = null
) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(notes[position])
    }

    override fun getItemCount(): Int = notes.size

    inner class NoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val text: TextView = view.findViewById(R.id.noteText)
        private val delete: ImageView = view.findViewById(R.id.noteDelete)

        fun bind(note: Note) {
            text.text = note.text
            delete.setOnClickListener { onDelete(note) }
            itemView.setOnClickListener { onTap?.invoke(note) }
        }
    }
}
