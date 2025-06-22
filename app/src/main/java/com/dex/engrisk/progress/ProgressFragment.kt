package com.dex.engrisk.progress

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.dex.engrisk.R
import com.dex.engrisk.adapter.CompletedLessonAdapter
import com.dex.engrisk.databinding.FragmentProgressBinding
import com.dex.engrisk.model.CompletedLesson
import com.dex.engrisk.model.Lesson
import com.dex.engrisk.model.LessonProgress
import com.dex.engrisk.model.UserProgress
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore

class ProgressFragment : Fragment() {

    private val TAG = "ProgressFragment"
    private lateinit var binding: FragmentProgressBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var completedLessonAdapter: CompletedLessonAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentProgressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initFirebase()
        setupRecyclerView()
        fetchUserProgress()
    }

    private fun initFirebase() {
        firebaseAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
    }

    private fun setupRecyclerView() {
        // Bước 1: Khởi tạo adapter và truyền vào hàm xử lý sự kiện click
        completedLessonAdapter = CompletedLessonAdapter(emptyList()) { clickedLesson ->
            // Khi một item được nhấn, đoạn code này sẽ chạy.
            // 'clickedLesson' là đối tượng chứa cả thông tin bài học và tiến độ.

            val lessonDetails = clickedLesson.lessonDetails
            val bundle = bundleOf("lessonId" to lessonDetails.id)

            when (lessonDetails.type) {
                "TRANSLATE_VI_EN", "TRANSLATE_EN_VI" -> {
                    findNavController().navigate(R.id.action_progressFragment_to_translateFragment, bundle)
                }
                "LISTEN_FILL_BLANK" -> {
                    findNavController().navigate(R.id.action_progressFragment_to_listenFillBlankFragment, bundle)
                }
                "LISTEN_CHOOSE_CORRECT" -> {
                    findNavController().navigate(R.id.action_progressFragment_to_listenChooseCorrectFragment, bundle)
                }
                else -> {
                    Toast.makeText(requireContext(), "Không thể mở lại loại bài học này.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        binding.rvCompletedLessons.apply {
            adapter = completedLessonAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun fetchUserProgress() {
        binding.progressBar.visibility = View.VISIBLE
        val uid = firebaseAuth.currentUser?.uid ?: return

        // BƯỚC A: LẤY DỮ LIỆU TIẾN ĐỘ
        db.collection("userProgress").document(uid).get()
            .addOnSuccessListener { document ->
                val userProgress = document.toObject(UserProgress::class.java)
                val progressMap = userProgress?.lessonProgress

                if (userProgress != null && !progressMap.isNullOrEmpty()) {
                    // Cập nhật số liệu tổng quan
                    binding.tvLessonsCompletedCount.text = progressMap.size.toString()
                    // BƯỚC B: LẤY CHI TIẾT CÁC BÀI HỌC TƯƠNG ỨNG
                    fetchLessonDetails(progressMap)
                } else {
                    Log.d(TAG, "No progress found for user $uid")
                    binding.tvLessonsCompletedCount.text = "0"
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error fetching user progress", exception)
            }
    }

    private fun fetchLessonDetails(progressMap: Map<String, LessonProgress>) {
        // Lấy danh sách các ID bài học từ map tiến độ
        val lessonIds = progressMap.keys.toList()
        if (lessonIds.isEmpty()) {
            updateCompletedLessonsList(emptyList())
            return
        }

        // Dùng truy vấn "in" để lấy tất cả document có ID nằm trong danh sách lessonIds
        db.collection("lessons").whereIn(FieldPath.documentId(), lessonIds).get()
            .addOnSuccessListener { lessonSnapshots ->
                val lessonDetailsMap = lessonSnapshots.toObjects(Lesson::class.java).associateBy { it.id }
                // BƯỚC C: GỘP HAI NGUỒN DỮ LIỆU
                val completedLessons = mutableListOf<CompletedLesson>()
                for ((lessonId, progress) in progressMap) {
                    lessonDetailsMap[lessonId]?.let { lesson ->
                        completedLessons.add(CompletedLesson(lessonDetails = lesson, progressDetails = progress))
                    }
                }
                updateCompletedLessonsList(completedLessons)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error fetching lesson details", exception)
                updateCompletedLessonsList(emptyList())
            }
    }

    private fun updateCompletedLessonsList(lessons: List<CompletedLesson>) {
        binding.progressBar.visibility = View.GONE
        Log.d(TAG, "updateCompletedLessonsList được gọi với danh sách có ${lessons.size} phần tử.")

        if (lessons.isEmpty()) {
            binding.rvCompletedLessons.visibility = View.GONE
            binding.tvHistoryLabel.visibility = View.GONE
            binding.tvLessonsCompletedCount.text = "0"
            completedLessonAdapter.updateData(emptyList())
        } else {
            binding.rvCompletedLessons.visibility = View.VISIBLE
            binding.tvHistoryLabel.visibility = View.VISIBLE
            completedLessonAdapter.updateData(lessons)
        }
    }
}