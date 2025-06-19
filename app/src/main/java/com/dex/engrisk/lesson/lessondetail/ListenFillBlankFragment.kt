package com.dex.engrisk.lesson.lessondetail

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.dex.engrisk.R
import com.dex.engrisk.databinding.FragmentListenFillBlankBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

// Thêm TextToSpeech.OnInitListener để lắng nghe khi TTS sẵn sàng
class ListenFillBlankFragment : Fragment(), TextToSpeech.OnInitListener {

    private val TAG = "ListenFillBlankFragment"
    private lateinit var binding: FragmentListenFillBlankBinding
    private lateinit var tts: TextToSpeech
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Biến trạng thái game
    private var questions: List<Map<String, String>> = emptyList()
    private var currentQuestionIndex = 0
    private var score = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentListenFillBlankBinding.inflate(inflater, container, false)
        // Khởi tạo TTS
        tts = TextToSpeech(requireContext(), this)
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

        binding.btnListen.setOnClickListener {
            speakCurrentSentence()
        }

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

    // --- HÀM CỦA OnInitListener ---
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Đặt ngôn ngữ là Tiếng Anh (Mỹ)
            val result = tts.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "The Language specified is not supported!")
            }
        } else {
            Log.e(TAG, "TTS Initialization Failed!")
        }
    }

    private fun speakCurrentSentence() {
        if (currentQuestionIndex < questions.size) {
            val sourceSentence = questions[currentQuestionIndex]["full_sentence"] ?: ""
            // Phát âm câu
            tts.speak(sourceSentence, TextToSpeech.QUEUE_FLUSH, null, "")
        }
    }

    private fun displayCurrentQuestion() {
        if (currentQuestionIndex < questions.size) {

            // Lấy dữ liệu từ Map câu hỏi
            val sourceSentence = questions[currentQuestionIndex]["full_sentence"] ?: ""
            val blankWord = questions[currentQuestionIndex]["blank_word"] ?: ""

            // --- ĐÂY LÀ DÒNG QUAN TRỌNG NHẤT ---
            // Nó lấy câu đầy đủ, tìm từ cần điền (không phân biệt chữ hoa-thường)
            // và thay thế nó bằng "______".
            val sentenceWithBlank = sourceSentence.replace(blankWord, "______", ignoreCase = true)
            // Hiển thị câu đã được xử lý ra giao diện
            binding.tvSourceSentence.text = sentenceWithBlank

            binding.tvProgress.text = "Câu hỏi: ${currentQuestionIndex + 1}/${questions.size}"
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
        val correctAnswer = questions[currentQuestionIndex]["blank_word"] ?: ""

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

    // --- QUẢN LÝ VÒNG ĐỜI CỦA TTS (Rất quan trọng) ---
    override fun onDestroy() {
        // Giải phóng tài nguyên TTS khi Fragment bị hủy để tránh memory leak
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}