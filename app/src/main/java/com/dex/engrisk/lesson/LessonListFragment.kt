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
import com.dex.engrisk.adapter.LessonAdapter
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

        setupRecyclerView()

        // Lấy tham số levelName được gửi đến
        val levelName = arguments?.getString("levelName")

        if (levelName != null) {
            // Nếu có levelName, thực hiện lấy dữ liệu
            fetchLessons(levelName)
        } else {
            // Xử lý lỗi nếu không có tham số được truyền
            Log.e(TAG, "Level name argument is missing!")
        }
    }

    private fun setupRecyclerView() {
        lessonAdapter = LessonAdapter(emptyList()) { clickedLesson ->
            // Dùng when để kiểm tra loại bài học
            when (clickedLesson.type) {
                // Nếu là 1 trong 2 loại này, đi đến màn hình dịch câu
                "TRANSLATE_VI_EN", "TRANSLATE_EN_VI" -> {
                    // Tạo một bundle để chứa ID của bài học được chọn
                    val bundle = bundleOf("lessonId" to clickedLesson.id)
                    // Điều hướng đến màn hình game, gửi kèm bundle chứa ID
                    findNavController().navigate(R.id.action_to_translateFragment, bundle)
                }

                // Sau này có thể thêm các case khác
                // "LISTEN_FILL_BLANK" -> { ... }

                else -> {
                    // Nếu gặp loại bài học chưa được hỗ trợ
                    Toast.makeText(
                        requireContext(),
                        "Loại bài học này chưa được hỗ trợ.",
                        Toast.LENGTH_SHORT
                    ).show()
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

        db = FirebaseFirestore.getInstance()
        db.collection("lessons")
            .whereEqualTo("level", levelName) // <-- SỬ DỤNG THAM SỐ Ở ĐÂY
            .orderBy("order", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val lessons = querySnapshot.toObjects(Lesson::class.java)
                lessonAdapter.updateLessons(lessons)
                binding.progressBar.visibility = View.GONE
                Log.d(TAG, "Fetched ${lessons.size} lessons for level: $levelName")
            }
            .addOnFailureListener { exception ->
                binding.progressBar.visibility = View.GONE
                Log.w(TAG, "Error getting documents for level $levelName: ", exception)
            }
    }
}