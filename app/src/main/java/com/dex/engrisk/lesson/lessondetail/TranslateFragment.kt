package com.dex.engrisk.lesson.lessondetail

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dex.engrisk.R
import com.dex.engrisk.databinding.FragmentTranslateBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TranslateFragment : Fragment() {

    private val TAG = "TranslateFragment"
    private lateinit var binding: FragmentTranslateBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Biến trạng thái
    private var questions: List<Map<String, String>> = emptyList()
    private var currentQuestionIndex = 0
    private var score = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTranslateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val lessonId = arguments?.getString("lessonId")
        if (lessonId != null) {
            fetchLessonDetails(lessonId)
        } else {
            Log.e(TAG, "Lesson ID is null!")
        }

        // --- LOGIC MỚI: GÁN SỰ KIỆN CHO CÁC NÚT ---
        binding.btnCheck.setOnClickListener {
            handleCheckAnswer()
        }

        binding.btnNext.setOnClickListener {
            handleNextQuestion()
        }
    }

    private fun fetchLessonDetails(lessonId: String) {
        binding.progressBar.visibility = View.VISIBLE
        // ... code hàm này giữ nguyên như trước ...
        val db = FirebaseFirestore.getInstance()
        db.collection("lessons").document(lessonId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val fetchedQuestions = document.get("questions") as? List<Map<String, String>>
                    if (!fetchedQuestions.isNullOrEmpty()) {
                        this.questions = fetchedQuestions
                        displayCurrentQuestion()
                    } else {
                        Log.e(TAG, "Questions array is null or empty.")
                    }
                } else {
                    Log.e(TAG, "No such document with ID: $lessonId")
                }
                binding.progressBar.visibility = View.GONE
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error fetching lesson details", exception)
                binding.progressBar.visibility = View.GONE
            }
    }

    private fun displayCurrentQuestion() {
        if (currentQuestionIndex < questions.size) {
            val question = questions[currentQuestionIndex]
            val sourceSentence = question["vi_sentence"] ?: "" // Mặc định là dịch Việt - Anh

            binding.tvProgress.text = "Câu hỏi: ${currentQuestionIndex + 1}/${questions.size}"
            binding.tvSourceSentence.text = sourceSentence
            binding.etAnswer.text?.clear()
            binding.etAnswer.isEnabled = true // Đảm bảo ô nhập được bật

            // Reset trạng thái các nút và feedback
            binding.layoutFeedback.visibility = View.GONE
            binding.btnCheck.visibility = View.VISIBLE
            binding.btnNext.visibility = View.GONE
        } else {
            // --- LOGIC MỚI: HOÀN THÀNH BÀI HỌC ---
            showCompletionDialog()
        }
    }

    // --- HÀM MỚI: XỬ LÝ KHI NHẤN NÚT "KIỂM TRA" ---
    private fun handleCheckAnswer() {
        val userAnswer = binding.etAnswer.text.toString().trim()
        val correctAnswer = questions[currentQuestionIndex]["en_sentence"] ?: ""

        binding.etAnswer.isEnabled = false // Vô hiệu hóa ô nhập sau khi kiểm tra
        binding.layoutFeedback.visibility = View.VISIBLE

        if (userAnswer.equals(correctAnswer, ignoreCase = true)) {
            // --- TRẢ LỜI ĐÚNG ---
            score++
            binding.tvFeedback.text = "Chính xác!"
            binding.tvFeedback.setTextColor(ContextCompat.getColor(requireContext(), R.color.correct_green))
            binding.tvCorrectAnswer.visibility = View.GONE // Ẩn đáp án đúng vì đã trả lời đúng
        } else {
            // --- TRẢ LỜI SAI ---
            binding.tvFeedback.text = "Chưa chính xác!"
            binding.tvFeedback.setTextColor(ContextCompat.getColor(requireContext(), R.color.incorrect_red))
            binding.tvCorrectAnswer.visibility = View.VISIBLE
            binding.tvCorrectAnswer.text = "Đáp án đúng: $correctAnswer"
        }

        // Chuyển đổi nút
        binding.btnCheck.visibility = View.GONE
        binding.btnNext.visibility = View.VISIBLE
    }

    // --- HÀM MỚI: XỬ LÝ KHI NHẤN NÚT "TIẾP TỤC" ---
    private fun handleNextQuestion() {
        currentQuestionIndex++
        displayCurrentQuestion() // Hiển thị câu hỏi tiếp theo hoặc màn hình kết quả
    }

    // --- HÀM MỚI: HIỂN THỊ HỘP THOẠI KHI HOÀN THÀNH ---
    private fun showCompletionDialog() {
        // Lấy lessonId mà chúng ta đã nhận được lúc đầu
        val lessonId = arguments?.getString("lessonId") ?: ""

        // --- BƯỚC MỚI: LƯU ĐIỂM VÀO FIRESTORE ---
        if (lessonId.isNotEmpty()) {
            saveLessonProgress(lessonId, score, questions.size)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Hoàn thành bài học!")
            .setMessage("Chúc mừng bạn đã hoàn thành bài học với số điểm: $score/${questions.size}")
            .setPositiveButton("Tuyệt vời!") { dialog, _ ->
                // Nhấn nút -> quay lại màn hình danh sách bài học
                findNavController().popBackStack()
                dialog.dismiss()
            }
            .setCancelable(false) // Không cho phép đóng dialog bằng cách nhấn ra ngoài
            .show()
    }

    // --- HÀM LƯU TIẾN ĐỘ ---
    private fun saveLessonProgress(lessonId: String, userScore: Int, totalQuestions: Int) {
        val uid = firebaseAuth.currentUser?.uid ?: return

        // Tạo một đối tượng Map để chứa thông tin về lần làm bài này
        val progressData = mapOf(
            "score" to userScore,
            "totalQuestions" to totalQuestions,
            "completedAt" to com.google.firebase.Timestamp.now()
        )

        // Sử dụng "dot notation" để cập nhật một trường bên trong map 'lessonProgress'
        // Ví dụ: lessonProgress.lesson_abc
        val fieldToUpdate = "lessonProgress.$lessonId"

        db.collection("userProgress").document(uid)
            .update(fieldToUpdate, progressData)
            .addOnSuccessListener {
                Log.d(TAG, "Lesson progress saved successfully for lesson: $lessonId")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error saving lesson progress", e)
                // Lưu ý: Kể cả khi lưu lỗi, ta vẫn không cần báo cho người dùng
                // vì đây là một tác vụ nền, không ảnh hưởng đến trải nghiệm của họ.
            }
    }
}