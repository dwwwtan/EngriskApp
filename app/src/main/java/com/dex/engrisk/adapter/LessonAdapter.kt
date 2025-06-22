package com.dex.engrisk.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
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
        holder.binding.tvLessonType.text = lesson.type // Ta sẽ cải thiện cái này sau

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