package com.dex.engrisk.lesson.lessondetail

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

    // --- BIẾN TRẠNG THÁI ---
    private var questions: List<Map<String, String>> = emptyList()
    private var currentQuestionIndex = 0
    private var score = 0
    private var lessonType: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentTranslateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initFirebase()
        loadLesson()
        setupClickListeners()
    }

    // --- KHỞI TẠO FIREBASE ---
    private fun initFirebase() {
        firebaseAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
    }

    // --- NẠP BÀI HỌC ---
    private fun loadLesson() {
        // --- LẤY ID BÀI HỌC TỪ THAM SỐ ---
        val lessonId = arguments?.getString("lessonId") ?: ""

        if (lessonId.isNotEmpty()) {
            fetchLessonDetails(lessonId)
        } else {
            Log.e(TAG, "Lesson ID is missing. Cannot fetch lesson details.")
            Toast.makeText(requireContext(), "Lỗi: Không tìm thấy bài học.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupClickListeners() {
        binding.btnCheck.setOnClickListener { handleCheckAnswer() }
        binding.btnNext.setOnClickListener { handleNextQuestion() }
    }

    // --- HÀM LẤY THÔNG TIN BÀI HỌC ---
    private fun fetchLessonDetails(lessonId: String) {
        binding.progressBar.visibility = View.VISIBLE
        db.collection("lessons").document(lessonId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Lấy và lưu lại loại bài học
                    this.lessonType = document.getString("type") ?: ""

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

    // --- HÀM HIỂN THỊ CÂU HỎI HIỆN TẠI ---
    private fun displayCurrentQuestion() {
        if (currentQuestionIndex < questions.size) {
            val question = questions[currentQuestionIndex]
            val sourceSentence = when (lessonType) {
                "TRANSLATE_VI_EN" -> question["vi_sentence"] ?: ""
                "TRANSLATE_EN_VI" -> question["en_sentence"] ?: ""
                else -> ""
            }

            binding.tvSourceSentenceLabel.text = when (lessonType) {
                "TRANSLATE_VI_EN" -> "Dịch sang tiếng Anh"
                "TRANSLATE_EN_VI" -> "Dịch sang tiếng Việt"
                else -> "Dịch câu sau:"
            }

            val progressPercentage = ((currentQuestionIndex + 1) * 100 / questions.size)
            binding.progressIndicator.progress = progressPercentage
            binding.tvSourceSentence.text = sourceSentence
            binding.etAnswer.text?.clear()
            binding.etAnswer.isEnabled = true
            binding.btnCheck.isEnabled = true
            binding.btnCheck.visibility = View.VISIBLE
            hideFeedbackPanel()
        } else {
            showCompletionDialog()
        }
    }

    // --- XỬ LÝ KHI NHẤN NÚT "KIỂM TRA" ---
    private fun handleCheckAnswer() {
        val userAnswer = binding.etAnswer.text.toString().trim()
        val question = questions[currentQuestionIndex]

        // LessonType
        val correctAnswer: String = when (lessonType) {
            "TRANSLATE_VI_EN" -> question["en_sentence"] ?: ""
            "TRANSLATE_EN_VI" -> question["vi_sentence"] ?: ""
            else -> ""
        }

        binding.etAnswer.isEnabled = false
        binding.btnCheck.isEnabled = false

        val isCorrect = userAnswer.equals(correctAnswer, ignoreCase = true)
        if (isCorrect) {
            score++
        }
        showFeedbackPanel(isCorrect, correctAnswer)
    }

    private fun showFeedbackPanel(isCorrect: Boolean, correctAnswer: String) {
        if (isCorrect) {
            binding.feedbackPanel.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.correct_green_bg)
            binding.tvFeedback.text = "Chính xác!"
            binding.tvFeedback.setTextColor(ContextCompat.getColor(requireContext(), R.color.correct_green))
            binding.tvCorrectAnswer.visibility = View.GONE
        } else {
            binding.feedbackPanel.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.incorrect_red_bg)
            binding.tvFeedback.text = "Chưa chính xác!"
            binding.tvFeedback.setTextColor(ContextCompat.getColor(requireContext(), R.color.incorrect_red))
            binding.tvCorrectAnswer.visibility = View.VISIBLE
            binding.tvCorrectAnswer.text = "Đáp án đúng: $correctAnswer"
        }

        // Chuẩn bị cho animation
        binding.feedbackPanel.visibility = View.VISIBLE
        binding.feedbackPanel.translationY = binding.feedbackPanel.height.toFloat() // Đưa panel xuống dưới

        // Bắt đầu animation trượt lên
        binding.feedbackPanel.animate().translationY(0f).setDuration(300).start()
        binding.btnCheck.visibility = View.GONE
    }

    private fun hideFeedbackPanel() {
        binding.feedbackPanel.visibility = View.GONE
        // Đặt lại vị trí ban đầu cho lần hiển thị sau
        binding.feedbackPanel.translationY = binding.feedbackPanel.height.toFloat()
    }

    // --- XỬ LÝ KHI NHẤN NÚT "TIẾP TỤC" ---
    private fun handleNextQuestion() {
        currentQuestionIndex++
        displayCurrentQuestion() // Hiển thị câu hỏi tiếp theo hoặc màn hình kết quả
    }

    // --- HIỂN THỊ HỘP THOẠI KHI HOÀN THÀNH ---
    private fun showCompletionDialog() {
        val lessonId = arguments?.getString("lessonId") ?: ""

        // --- LƯU ĐIỂM VÀO FIRESTORE ---
        if (lessonId.isNotEmpty()) {
            saveLessonProgress(lessonId, score, questions.size)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Hoàn thành bài học!")
            .setMessage("Chúc mừng bạn đã hoàn thành bài học với số điểm: $score/${questions.size}")
            .setPositiveButton("Tuyệt vời!") { dialog, _ ->
                // Nhấn nút -> quay lại màn hình danh sách bài học
                findNavController().popBackStack() // Quay lại màn hình trước đó
                dialog.dismiss()
            }
            .setCancelable(false).show()
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

        // Kỹ thuật "dot notation" này rất hiệu quả để cập nhật một trường
        // bên trong một đối tượng (map) trên Firestore mà không cần ghi đè cả đối tượng.
        // ví dụ: lessonProgress.lesson_abc
        val fieldToUpdate = "lessonProgress.$lessonId"

        db.collection("userProgress").document(uid)
            .update(fieldToUpdate, progressData)
            .addOnSuccessListener {
                Log.d(TAG, "Lesson progress saved successfully for lesson: $lessonId")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error saving lesson progress", e)
            }
    }
}

