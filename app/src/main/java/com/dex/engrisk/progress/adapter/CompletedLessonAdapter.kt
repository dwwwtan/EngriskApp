package com.dex.engrisk.progress.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dex.engrisk.databinding.ItemCompletedLessonBinding
import com.dex.engrisk.model.CompletedLesson
import java.text.SimpleDateFormat
import java.util.Locale
import com.dex.engrisk.R

class CompletedLessonAdapter(
    private var completedLessons: List<CompletedLesson>,
    private val onItemClicked: (CompletedLesson) -> Unit
) : RecyclerView.Adapter<CompletedLessonAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemCompletedLessonBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCompletedLessonBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val completedLesson = completedLessons[position]
        val lesson = completedLesson.lessonDetails
        val progress = completedLesson.progressDetails

        holder.binding.tvLessonTitle.text = completedLesson.lessonDetails.title
        holder.binding.tvScore.text = "${progress.score}/${progress.totalQuestions}"
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        holder.binding.tvCompletedDate.text = "Hoàn thành: ${sdf.format(progress.completedAt.toDate())}"

        when (lesson.type) {
            "TRANSLATE_VI_EN", "TRANSLATE_EN_VI" -> {
                holder.binding.ivLessonIcon.setImageResource(R.drawable.ic_translate_24)
            }
            "LISTEN_FILL_BLANK" -> {
                holder.binding.ivLessonIcon.setImageResource(R.drawable.ic_edit_note_24)
            }
            "LISTEN_CHOOSE_CORRECT" -> {
                holder.binding.ivLessonIcon.setImageResource(R.drawable.ic_check_circle_24)
            }
            else -> {
                holder.binding.ivLessonIcon.setImageResource(R.drawable.ic_lesson_default_24)
            }
        }

        holder.itemView.setOnClickListener {
            // Khi được nhấn, gọi hàm callback và truyền vào đối tượng của item đó
            onItemClicked(completedLesson)
        }
    }

    override fun getItemCount(): Int = completedLessons.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newCompletedLessons: List<CompletedLesson>) {
        completedLessons = newCompletedLessons
        notifyDataSetChanged()
    }
}