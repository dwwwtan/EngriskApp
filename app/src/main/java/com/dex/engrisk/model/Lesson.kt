package com.dex.engrisk.model

import com.google.firebase.firestore.DocumentId

data class Lesson(
    @DocumentId // Annotation này giúp tự động map ID của document vào trường này
    val id: String = "",
    val title: String = "",
    val level: String = "",
    val type: String = "",
    val order: Int = 0, // Trường này có thể dùng để sắp xếp các bài học
    var questions: List<Map<String, Any>> = emptyList()

    // Bạn có thể thêm các trường khác như 'order' nếu cần
)