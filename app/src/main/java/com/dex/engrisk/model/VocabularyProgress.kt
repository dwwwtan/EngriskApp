package com.dex.engrisk.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class VocabularyProgress(
    var isLearned: Boolean = false,
    // Dùng @ServerTimestamp để Firestore tự điền ngày giờ trên server,
    // và cho phép giá trị null ban đầu để không bị lỗi khi đọc.
    @ServerTimestamp
    var lastReviewed: Timestamp? = null
) {
    // Thêm constructor rỗng để Firestore không báo lỗi
    constructor() : this(false, null)
}