package com.ratio.launcher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class NoteEditorActivity : AppCompatActivity() {

    private lateinit var editor: EditText
    private var noteId: Long = -1

    companion object {
        const val EXTRA_NOTE_ID = "note_id"
        const val EXTRA_NOTE_TEXT = "note_text"
        const val RESULT_DELETED = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_editor)

        editor = findViewById(R.id.noteEditorText)
        val saveBtn = findViewById<TextView>(R.id.noteSaveBtn)
        val deleteBtn = findViewById<TextView>(R.id.noteDeleteBtn)

        noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1)
        val text = intent.getStringExtra(EXTRA_NOTE_TEXT) ?: ""
        editor.setText(text)
        editor.setSelection(text.length)

        saveBtn.setOnClickListener {
            val result = Intent().apply {
                putExtra(EXTRA_NOTE_ID, noteId)
                putExtra(EXTRA_NOTE_TEXT, editor.text.toString())
            }
            setResult(RESULT_OK, result)
            finish()
        }

        deleteBtn.setOnClickListener {
            val result = Intent().apply {
                putExtra(EXTRA_NOTE_ID, noteId)
            }
            setResult(RESULT_DELETED, result)
            finish()
        }
    }
}
