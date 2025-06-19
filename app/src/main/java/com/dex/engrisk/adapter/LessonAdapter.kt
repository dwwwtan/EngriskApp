package com.dex.engrisk.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dex.engrisk.databinding.LessonItemBinding
import com.dex.engrisk.model.Lesson

// BƯỚC 1: Sửa đổi constructor để nhận vào một hàm (lambda)
// Hàm này không có giá trị trả về (Unit) và nhận vào một đối tượng Lesson
class LessonAdapter(
    private var lessons: List<Lesson>,
    private val onItemClicked: (Lesson) -> Unit
) :
    RecyclerView.Adapter<LessonAdapter.LessonViewHolder>() {

    // Lớp ViewHolder chứa tham chiếu đến các view trong một item
    inner class LessonViewHolder(val binding: LessonItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    // Tạo ViewHolder mới
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LessonViewHolder {
        val binding = LessonItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LessonViewHolder(binding)
    }

    // Gán dữ liệu từ danh sách vào các view trong ViewHolder
    override fun onBindViewHolder(holder: LessonViewHolder, position: Int) {
        val lesson = lessons[position]
        holder.binding.tvLessonTitle.text = lesson.title
        holder.binding.tvLessonType.text = lesson.type // Ta sẽ cải thiện cái này sau

        // BƯỚC 2: Thiết lập sự kiện click cho view của item
        // Khi người dùng nhấn vào, gọi hàm onItemClicked và truyền vào đối tượng lesson tương ứng
        holder.itemView.setOnClickListener {
            onItemClicked(lesson)
        }
    }

    // Trả về số lượng item trong danh sách
    override fun getItemCount(): Int {
        return lessons.size
    }

    // Hàm để cập nhật danh sách và thông báo cho RecyclerView vẽ lại
    fun updateLessons(newLessons: List<Lesson>) {
        lessons = newLessons
        notifyDataSetChanged()
    }
}