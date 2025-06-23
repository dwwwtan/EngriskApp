package com.dex.engrisk.model

import androidx.annotation.DrawableRes

data class Topic(
    val name: String,
    // Annotation này để đảm bảo chúng ta truyền vào một ID của drawable
    @DrawableRes val iconResId: Int
)