package com.dex.engrisk.model

// Lớp này chứa thông tin đầy đủ của một bài học đã được hoàn thành
data class CompletedLesson(
    val lessonDetails: Lesson, // Chứa title, type, level...
    val progressDetails: LessonProgress // Chứa score, completedAt...
)