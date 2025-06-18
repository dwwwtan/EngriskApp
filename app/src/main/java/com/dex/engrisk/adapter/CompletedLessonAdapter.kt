package com.dex.engrisk.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dex.engrisk.databinding.ItemCompletedLessonBinding
import com.dex.engrisk.model.CompletedLesson
import java.text.SimpleDateFormat
import java.util.Locale

// Sửa constructor để nhận vào danh sách đối tượng mới
class CompletedLessonAdapter(private var completedLessons: List<CompletedLesson>) :
    RecyclerView.Adapter<CompletedLessonAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemCompletedLessonBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCompletedLessonBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val completedLesson = completedLessons[position]

        // Bây giờ ta có thể lấy title từ lessonDetails
        holder.binding.tvLessonTitle.text = completedLesson.lessonDetails.title

        // Lấy điểm số và ngày tháng từ progressDetails
        val progress = completedLesson.progressDetails
        holder.binding.tvScore.text = "${progress.score}/${progress.totalQuestions}"
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        holder.binding.tvCompletedDate.text = "Hoàn thành: ${sdf.format(progress.completedAt.toDate())}"
    }

    override fun getItemCount(): Int = completedLessons.size

    fun updateData(newCompletedLessons: List<CompletedLesson>) {
        completedLessons = newCompletedLessons
        notifyDataSetChanged()
    }
}