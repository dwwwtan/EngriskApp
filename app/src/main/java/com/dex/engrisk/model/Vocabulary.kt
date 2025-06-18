package com.dex.engrisk.model

import com.google.firebase.firestore.DocumentId

data class Vocabulary(
    @DocumentId
    val id: String = "",
    val word: String = "",
    val type: String = "",
    val pronunciation: String = "",
    val definition: String = "",
    val example: String = "",
    val imageUrl: String = ""
)