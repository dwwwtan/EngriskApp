package com.dex.engrisk.lesson.lessondetail

import android.content.res.ColorStateList
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dex.engrisk.R
import com.dex.engrisk.databinding.FragmentListenChooseCorrectBinding
import com.google.android.material.button.MaterialButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class ListenChooseCorrectFragment : Fragment(), TextToSpeech.OnInitListener {

    private val TAG = "ListenChooseCorrect"
    private lateinit var binding: FragmentListenChooseCorrectBinding
    private lateinit var tts: TextToSpeech
    private lateinit var optionButtons: List<MaterialButton>
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var questions: List<Map<String, Any>> = emptyList()
    private var currentQuestionIndex = 0
    private var score = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentListenChooseCorrectBinding.inflate(inflater, container, false)
        tts = TextToSpeech(requireContext(), this)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initFirebase()
        optionButtons = listOf(binding.btnOption1, binding.btnOption2, binding.btnOption3, binding.btnOption4)
        setupClickListeners()
        loadLesson()
    }

    private fun initFirebase() {
        firebaseAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
    }

    private fun loadLesson() {
        val lessonId = arguments?.getString("lessonId")
        if (!lessonId.isNullOrEmpty()) {
            fetchLessonDetails(lessonId)
        } else {
            Log.e(TAG, "Lesson ID is null or empty!")
        }
    }

    private fun setupClickListeners() {
        binding.btnSpeak.setOnClickListener { speakCurrentSentence() }
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
                    val fetchedQuestions = document.get("questions") as? List<Map<String, Any>>
                    if (!fetchedQuestions.isNullOrEmpty()) {
                        this.questions = fetchedQuestions
                        displayCurrentQuestion()
                    } else {
                        Log.e(TAG, "Questions array is null or not in the correct format.")
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
            val correctAnswer = question["correct_answer"] as? String ?: ""
            val options = question["options"] as? List<String> ?: listOf()

            // Xáo trộn các lựa chọn để vị trí đáp án đúng luôn ngẫu nhiên
            val allChoices = (options + correctAnswer).shuffled()

            // Gán các lựa chọn và SỰ KIỆN CLICK lên các nút
            optionButtons.forEachIndexed { index, button ->
                if (index < allChoices.size) {
                    button.visibility = View.VISIBLE
                    button.text = allChoices[index]
                    button.isEnabled = true

                    // Reset màu của nút về trạng thái mặc định
                    button.strokeColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.default_stroke_color))
                    button.setOnClickListener { handleAnswer(button, correctAnswer) }
                } else {
                    button.visibility = View.GONE
                }
            }
            val progressPercentage = ((currentQuestionIndex + 1) * 100 / questions.size)
            binding.progressIndicator.progress = progressPercentage
            binding.btnNext.visibility = View.GONE
            hideFeedbackPanel()
            speakCurrentSentence()
        } else {
             showCompletionDialog()
        }
    }

    // --- HÀM XỬ LÝ KHI NGƯỜI DÙNG NHẤN VÀO MỘT NÚT TRẢ LỜI ---
    private fun handleAnswer(clickedButton: MaterialButton, correctAnswer: String) {
        optionButtons.forEach { it.isEnabled = false }

        val isCorrect = clickedButton.text.toString() == correctAnswer
        if (isCorrect) {
            score++
            clickedButton.strokeColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.correct_green))
        } else {
            clickedButton.strokeColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.incorrect_red))
            optionButtons.find { it.text.toString() == correctAnswer }?.apply {
                strokeColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.correct_green))
            }
        }
        showFeedbackPanel(isCorrect, correctAnswer)

    }

    private fun handleNextQuestion() {
        currentQuestionIndex++
        displayCurrentQuestion()
    }

    private fun speakCurrentSentence() {
        if (currentQuestionIndex < questions.size) {
            val sentence = questions[currentQuestionIndex]["en_sentence"] as? String ?: ""
            tts.speak(sentence, TextToSpeech.QUEUE_FLUSH, null, "")
        }
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
        binding.feedbackPanel.visibility = View.VISIBLE
        binding.feedbackPanel.translationY = binding.feedbackPanel.height.toFloat()
        binding.feedbackPanel.animate().translationY(0f).setDuration(300).start()
        binding.btnNext.visibility = View.VISIBLE
    }

    private fun hideFeedbackPanel() {
        binding.feedbackPanel.visibility = View.GONE
        binding.feedbackPanel.translationY = binding.feedbackPanel.height.toFloat()
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
            "completedAt" to Timestamp.now()
        )

        // Sử dụng "dot notation" để cập nhật một trường bên trong map 'lessonProgress'
        // Ví dụ: lessonProgress.lesson_abc
        val fieldToUpdate = "lessonProgress.$lessonId"

        db.collection("userProgress").document(uid)
            .update(fieldToUpdate, progressData)
            .addOnSuccessListener { Log.d(TAG, "Lesson progress saved successfully for lesson: $lessonId") }
            .addOnFailureListener { e -> Log.w(TAG, "Error saving lesson progress", e) }
    }

    override fun onDestroyView() {
        // Giải phóng tài nguyên TTS khi Fragment bị hủy để tránh memory leak
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroyView()
    }
}