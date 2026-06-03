package com.ratio.launcher.models

data class Note(
    val id: Long,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
