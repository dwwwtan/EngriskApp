package com.dex.engrisk.progress

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.dex.engrisk.R
import com.dex.engrisk.progress.adapter.CompletedLessonAdapter
import com.dex.engrisk.databinding.FragmentProgressBinding
import com.dex.engrisk.model.*
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
        setupClickListeners()
    }

    private fun initFirebase() {
        firebaseAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
    }

    private fun setupClickListeners() {
        binding.cardWordsSummary.setOnClickListener {
            findNavController().navigate(R.id.action_progressFragment_to_learnedWordsFragment)
        }
    }

    private fun setupRecyclerView() {
        // Khởi tạo adapter và truyền vào hàm xử lý sự kiện click
        completedLessonAdapter = CompletedLessonAdapter(emptyList()) { clickedLesson ->
            val lessonDetails = clickedLesson.lessonDetails
            val bundle = bundleOf("lessonId" to lessonDetails.id)
            when (lessonDetails.type) {
                "TRANSLATE_VI_EN", "TRANSLATE_EN_VI" -> findNavController().navigate(R.id.action_progressFragment_to_translateFragment, bundle)
                "LISTEN_FILL_BLANK" -> findNavController().navigate(R.id.action_progressFragment_to_listenFillBlankFragment, bundle)
                "LISTEN_CHOOSE_CORRECT" -> findNavController().navigate(R.id.action_progressFragment_to_listenChooseCorrectFragment, bundle)
            }
        }
        binding.rvCompletedLessons.apply {
            adapter = completedLessonAdapter
            layoutManager = LinearLayoutManager(requireContext())
            isNestedScrollingEnabled = false
        }
    }

    private fun fetchUserProgress() {
        binding.progressBar.visibility = View.VISIBLE
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("userProgress").document(uid).get()
            .addOnCompleteListener { task ->
                binding.progressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    val userProgress = task.result.toObject(UserProgress::class.java)
                    // Cập nhật số liệu thống kê
                    binding.tvLessonsCompletedCount.text = (userProgress?.lessonProgress?.size ?: 0).toString()
                    binding.tvWordsLearnedCount.text = (userProgress?.vocabularyProgress?.filterValues { it.isLearned }?.size ?: 0).toString()

                    // Xử lý danh sách bài học
                    userProgress?.lessonProgress?.let {
                        if (it.isNotEmpty()) fetchLessonDetails(it)
                    }
                } else {
                    Log.e(TAG, "Error fetching user progress", task.exception)
                }
            }
    }

    private fun fetchLessonDetails(progressMap: Map<String, LessonProgress>) {
        val lessonIds = progressMap.keys.toList()
        if (lessonIds.isEmpty()) {
            binding.rvCompletedLessons.visibility = View.GONE // Ẩn nếu không có gì
            return
        }

        db.collection("lessons").whereIn(FieldPath.documentId(), lessonIds).get()
            .addOnSuccessListener { lessonSnapshots ->
                val lessonDetailsMap = lessonSnapshots.toObjects(Lesson::class.java).associateBy { it.id }
                val completedLessons = progressMap.mapNotNull { (lessonId, progress) ->
                    lessonDetailsMap[lessonId]?.let { lesson ->
                        CompletedLesson(lessonDetails = lesson, progressDetails = progress)
                    }
                }
                binding.rvCompletedLessons.visibility = View.VISIBLE
                completedLessonAdapter.updateData(completedLessons)
            }
    }
}