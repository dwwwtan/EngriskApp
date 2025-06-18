package com.dex.engrisk.progress

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProgressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firebaseAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupRecyclerView()
        fetchUserProgress()
    }

    private fun setupRecyclerView() {
        completedLessonAdapter = CompletedLessonAdapter(emptyList())
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
                if (userProgress != null && userProgress.lessonProgress.isNotEmpty()) {
                    // Cập nhật số liệu tổng quan
                    binding.tvLessonsCompletedCount.text = userProgress.lessonProgress.size.toString()
                    // BƯỚC B: LẤY CHI TIẾT CÁC BÀI HỌC TƯƠNG ỨNG
                    fetchLessonDetails(userProgress.lessonProgress)
                } else {
                    binding.progressBar.visibility = View.GONE
                    Log.d(TAG, "No progress document found or no lessons completed.")
                }
            }
            .addOnFailureListener { exception ->
                binding.progressBar.visibility = View.GONE
                Log.e(TAG, "Error fetching user progress", exception)
            }
    }

    private fun fetchLessonDetails(progressMap: Map<String, LessonProgress>) {
        // Lấy danh sách các ID bài học từ map tiến độ
        val lessonIds = progressMap.keys.toList()

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

                // Cập nhật Adapter với dữ liệu đã được gộp
                completedLessonAdapter.updateData(completedLessons)
                binding.progressBar.visibility = View.GONE
            }
            .addOnFailureListener { exception ->
                binding.progressBar.visibility = View.GONE
                Log.e(TAG, "Error fetching lesson details", exception)
            }
    }
}