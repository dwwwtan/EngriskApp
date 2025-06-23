package com.dex.engrisk.lesson.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.dex.engrisk.R
import com.dex.engrisk.databinding.ItemLessonBinding
import com.dex.engrisk.model.Lesson

class LessonAdapter(
    private var lessons: List<Lesson>,
    private val onItemClicked: (Lesson) -> Unit
) : RecyclerView.Adapter<LessonAdapter.LessonViewHolder>() {

    // Lớp ViewHolder chứa tham chiếu đến các view trong một item
    inner class LessonViewHolder(val binding: ItemLessonBinding) : RecyclerView.ViewHolder(binding.root)

    // Tạo ViewHolder mới
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LessonViewHolder {
        val binding = ItemLessonBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LessonViewHolder(binding)
    }

    // Gán dữ liệu từ danh sách vào các view trong ViewHolder
    override fun onBindViewHolder(holder: LessonViewHolder, position: Int) {
        val lesson = lessons[position]
        holder.binding.tvLessonTitle.text = lesson.title

        val context = holder.itemView.context

        when (lesson.type) {
            "TRANSLATE_VI_EN", "TRANSLATE_EN_VI" -> {
                holder.binding.ivLessonIcon.setImageResource(R.drawable.ic_translate_24)
                holder.binding.tvLessonType.text = if(lesson.type == "TRANSLATE_VI_EN") "Dịch Việt - Anh" else "Dịch Anh - Việt"
                holder.binding.cardLesson.setCardBackgroundColor(ContextCompat.getColor(context, R.color.lesson_type_translate))
            }
            "LISTEN_FILL_BLANK" -> {
                holder.binding.ivLessonIcon.setImageResource(R.drawable.ic_edit_note_24)
                holder.binding.tvLessonType.text = "Nghe và Điền từ"
                holder.binding.cardLesson.setCardBackgroundColor(ContextCompat.getColor(context, R.color.lesson_type_listen_fill))
            }
            "LISTEN_CHOOSE_CORRECT" -> {
                holder.binding.ivLessonIcon.setImageResource(R.drawable.ic_check_circle_24)
                holder.binding.tvLessonType.text = "Nghe và Chọn đáp án"
                holder.binding.cardLesson.setCardBackgroundColor(ContextCompat.getColor(context, R.color.lesson_type_listen_choice))
            }
            else -> {
                holder.binding.ivLessonIcon.setImageResource(R.drawable.ic_lesson_default_24)
                holder.binding.tvLessonType.text = "Bài học khác"
                holder.binding.cardLesson.setCardBackgroundColor(ContextCompat.getColor(context, R.color.lesson_type_default))
            }
        }

        // BƯỚC 2: Thiết lập sự kiện click cho view của item
        // Khi người dùng nhấn vào, gọi hàm onItemClicked và truyền vào đối tượng lesson tương ứng
        holder.itemView.setOnClickListener {
            onItemClicked(lesson)
        }
    }

    // Trả về số lượng item trong danh sách
    override fun getItemCount(): Int = lessons.size

    // Hàm để cập nhật danh sách và thông báo cho RecyclerView vẽ lại
    @SuppressLint("NotifyDataSetChanged")
    fun updateLessons(newLessons: List<Lesson>) {
        lessons = newLessons
        notifyDataSetChanged()
    }
}