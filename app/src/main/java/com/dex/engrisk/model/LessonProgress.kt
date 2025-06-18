package com.dex.engrisk.model

import com.google.firebase.Timestamp

data class LessonProgress(
    val score: Int = 0,
    val totalQuestions: Int = 0,
    val completedAt: Timestamp = Timestamp.now()
)