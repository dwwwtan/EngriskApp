package com.dex.engrisk.model

data class UserProgress(
    // Dùng MutableMap để Firestore có thể map dữ liệu vào dễ dàng
    // Key là lessonId (String), Value là đối tượng LessonProgress
    val lessonProgress: MutableMap<String, LessonProgress> = mutableMapOf()
    // Tương tự, ta sẽ thêm vocabulary_progress sau
)