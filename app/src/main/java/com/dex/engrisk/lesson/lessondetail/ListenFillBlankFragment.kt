package com.dex.engrisk.lesson.lessondetail

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.dex.engrisk.R
import com.dex.engrisk.databinding.FragmentListenFillBlankBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

// Implement interface này là bước đầu tiên để sử dụng TTS.
// Nó yêu cầu chúng ta phải định nghĩa hàm onInit để biết khi nào TTS sẵn sàng.
class ListenFillBlankFragment : Fragment(), TextToSpeech.OnInitListener {

    private val TAG = "ListenFillBlankFragment"
    private lateinit var binding: FragmentListenFillBlankBinding
    private lateinit var tts: TextToSpeech
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Biến trạng thái
    private var questions: List<Map<String, String>> = emptyList()
    private var currentQuestionIndex = 0
    private var score = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentListenFillBlankBinding.inflate(inflater, container, false)
        tts = TextToSpeech(requireContext(), this)
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
        binding.btnSpeak.setOnClickListener { speakCurrentSentence() }
        binding.btnCheck.setOnClickListener { handleCheckAnswer() }
        binding.btnNext.setOnClickListener { handleNextQuestion() }
    }

    // --- HÀM CỦA OnInitListener ---
    /**
     * Hàm này sẽ được tự động gọi bởi hệ thống Android sau khi bộ máy TTS đã khởi tạo xong.
     * @param status cho biết việc khởi tạo có thành công hay không.
     */
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "The Language specified is not supported!")
                binding.btnSpeak.isEnabled = false
            } else {
                binding.btnSpeak.isEnabled = true
                Log.d(TAG, "TTS Initialized Successfully!")
                // Phát âm câu đầu tiên nếu có
                if (questions.isNotEmpty()) {
                    speakCurrentSentence()
                }
            }
        } else {
            Log.e(TAG, "TTS Initialization Failed!")
            binding.btnSpeak.isEnabled = false
        }
    }

    // --- HÀM LẤY THÔNG TIN BÀI HỌC ---
    private fun fetchLessonDetails(lessonId: String) {
        binding.progressBar.visibility = View.VISIBLE
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



    private fun speakCurrentSentence() {
        if (currentQuestionIndex < questions.size) {
            val question = questions[currentQuestionIndex]
            val sentenceToSpeak  = question["full_sentence"] ?: ""
            // Phát âm câu, QUEUE_FLUSH sẽ hủy các yêu cầu phát âm trước đó để đảm bảo chỉ có câu hiện tại được đọc.
            tts.speak(sentenceToSpeak , TextToSpeech.QUEUE_FLUSH, null, "")
        }
    }

    private fun displayCurrentQuestion() {
        if (currentQuestionIndex < questions.size) {
            val question = questions[currentQuestionIndex]
            // Lấy dữ liệu từ Map câu hỏi
            val fullSentence = question["full_sentence"] ?: ""
            val blankWord = question["blank_word"] ?: ""

            // Hiển thị câu hỏi
            val sentenceWithBlank = fullSentence.replace(blankWord, "______", ignoreCase = true)

            // Hiển thị câu đã được xử lý ra giao diện
            binding.tvSourceSentence.text = sentenceWithBlank
            binding.tvProgress.text = "Câu hỏi: ${currentQuestionIndex + 1}/${questions.size}"
            binding.etAnswer.text?.clear()
            binding.etAnswer.isEnabled = true

            // Reset trạng thái các nút và feedback
            binding.layoutFeedback.visibility = View.GONE
            binding.btnCheck.visibility = View.VISIBLE
            binding.btnNext.visibility = View.GONE
        } else {
            showCompletionDialog()
        }
    }

    // --- XỬ LÝ KHI NHẤN NÚT "KIỂM TRA" ---
    private fun handleCheckAnswer() {
        val userAnswer = binding.etAnswer.text.toString().trim()
        val question = questions[currentQuestionIndex]
        val correctAnswer = question["blank_word"] ?: ""

        binding.etAnswer.isEnabled = false
        binding.layoutFeedback.visibility = View.VISIBLE

        if (userAnswer.equals(correctAnswer, ignoreCase = true)) {
            // --- TRẢ LỜI ĐÚNG ---
            score++
            binding.tvFeedback.text = "Chính xác!"
            binding.tvFeedback.setTextColor(ContextCompat.getColor(requireContext(), R.color.correct_green))
            binding.tvCorrectAnswer.visibility = View.GONE
        } else {
            // --- TRẢ LỜI SAI ---
            binding.tvFeedback.text = "Chưa chính xác!"
            binding.tvFeedback.setTextColor(ContextCompat.getColor(requireContext(), R.color.incorrect_red))
            binding.tvCorrectAnswer.visibility = View.VISIBLE
            binding.tvCorrectAnswer.text = "Đáp án đúng: $correctAnswer"
        }

        binding.btnCheck.visibility = View.GONE
        binding.btnNext.visibility = View.VISIBLE
    }

    // --- XỬ LÝ KHI NHẤN NÚT "TIẾP TỤC" ---
    private fun handleNextQuestion() {
        currentQuestionIndex++
        displayCurrentQuestion()
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
                findNavController().popBackStack()
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

    // --- QUẢN LÝ VÒNG ĐỜI CỦA TTS ---
    override fun onDestroy() {
        // Giải phóng tài nguyên TTS khi Fragment bị hủy để tránh memory leak
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}