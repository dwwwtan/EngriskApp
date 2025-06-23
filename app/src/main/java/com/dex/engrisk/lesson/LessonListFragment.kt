package com.dex.engrisk.lesson

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.dex.engrisk.R
import com.dex.engrisk.lesson.adapter.LessonAdapter
import com.dex.engrisk.databinding.FragmentLessonListBinding
import com.dex.engrisk.model.Lesson
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class LessonListFragment : Fragment() {

    private val TAG = "LessonListFragment"
    private lateinit var binding: FragmentLessonListBinding
    private lateinit var lessonAdapter: LessonAdapter
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLessonListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initFirebase()
        setupRecyclerView()

        // Lấy tham số levelName được gửi đến
        val levelName = arguments?.getString("levelName")

        if (!levelName.isNullOrEmpty()) {
            // Cập nhật tiêu đề màn hình
            binding.tvLevelTitle.text = "$levelName"
            fetchLessons(levelName)
        } else {
            Log.e(TAG, "Level name argument is missing!")
        }
    }

    private fun initFirebase() {
        db = FirebaseFirestore.getInstance()
    }

    private fun setupRecyclerView() {
        lessonAdapter = LessonAdapter(emptyList()) { clickedLesson ->
            // Tạo một bundle để chứa ID của bài học được chọn
            val bundle = bundleOf("lessonId" to clickedLesson.id)
            when (clickedLesson.type) {
                "TRANSLATE_VI_EN", "TRANSLATE_EN_VI" -> { findNavController().navigate(R.id.action_to_translateFragment, bundle) }
                "LISTEN_FILL_BLANK" -> { findNavController().navigate(R.id.action_to_listenFillBlankFragment, bundle) }
                "LISTEN_CHOOSE_CORRECT" -> { findNavController().navigate(R.id.action_to_listenChooseCorrectFragment, bundle) }
                else -> {
                    Toast.makeText(requireContext(), "Loại bài học này chưa được hỗ trợ.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.rvLessons.apply {
            adapter = lessonAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun fetchLessons(levelName: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmptyState.visibility = View.GONE

        db.collection("lessons")
            .whereEqualTo("level", levelName)
            .orderBy("order", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                binding.progressBar.visibility = View.GONE
                val lessons = querySnapshot.toObjects(Lesson::class.java)
                if (lessons.isEmpty()) {
                    binding.rvLessons.visibility = View.GONE
                    binding.tvEmptyState.visibility = View.VISIBLE
                } else {
                    binding.rvLessons.visibility = View.VISIBLE
                    binding.tvEmptyState.visibility = View.GONE
                    lessonAdapter.updateLessons(lessons)
                }
                Log.d(TAG, "Fetched ${lessons.size} lessons for level: $levelName")
            }
            .addOnFailureListener { exception ->
                binding.progressBar.visibility = View.GONE
                binding.tvEmptyState.visibility = View.VISIBLE // Hiển thị lỗi nếu không tải được
                binding.tvEmptyState.text = "Lỗi khi tải bài học."
                Log.w(TAG, "Error getting documents for level $levelName: ", exception)
            }
    }
}