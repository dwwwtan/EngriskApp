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

        optionButtons = listOf(binding.btnOption1, binding.btnOption2, binding.btnOption3, binding.btnOption4)

        firebaseAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Logic fetch data, gán sự kiện click tương tự các fragment game khác
        // Bạn hãy copy và chỉnh sửa các hàm fetchLessonDetails, showCompletionDialog, saveLessonProgress

        binding.btnListen.setOnClickListener { speakCurrentSentence() }
        binding.btnNext.setOnClickListener { handleNextQuestion() }

        val lessonId = arguments?.getString("lessonId")
        if (lessonId != null) {
            fetchLessonDetails(lessonId)
        } else {
            Log.e(TAG, "Lesson ID is null!")
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

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "The Language specified is not supported!")
            }
        } else {
            Log.e(TAG, "TTS Initialization Failed!")
        }
    }

    private fun displayCurrentQuestion() {
        if (currentQuestionIndex < questions.size) {
            val question = questions[currentQuestionIndex]
            val correctAnswer = question["correct_answer"] as? String ?: ""
            val options = question["options"] as? List<String> ?: listOf()

            val allChoices = (options + correctAnswer).shuffled()

            // Gán các lựa chọn và SỰ KIỆN CLICK lên các nút
            optionButtons.forEachIndexed { index, button ->
                if (index < allChoices.size) {
                    button.visibility = View.VISIBLE
                    button.text = allChoices[index]

                    // Reset lại trạng thái của nút
                    button.isEnabled = true
                    button.strokeColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.default_stroke_color))

                    // QUAN TRỌNG: Gán sự kiện click cho từng nút lựa chọn ở đây
                    button.setOnClickListener {
                        // Khi người dùng nhấn vào nút này, ta gọi hàm handleAnswer
                        // và truyền vào chính cái nút đã được nhấn.
                        handleAnswer(it as MaterialButton, correctAnswer)
                    }
                } else {
                    button.visibility = View.GONE
                }
            }

            // Reset các view khác
            binding.layoutFeedback.visibility = View.GONE
            binding.btnCheck.visibility = View.VISIBLE
            binding.btnNext.visibility = View.GONE
            // ...
        } else {
             showCompletionDialog()
        }
    }

    /**
     * Hàm này được gọi ngay khi người dùng nhấn vào MỘT trong bốn nút đáp án.
     * @param clickedButton Chính là cái nút mà người dùng đã nhấn.
     * @param correctAnswer Đáp án đúng của câu hỏi hiện tại.
     */
    private fun handleAnswer(clickedButton: MaterialButton, correctAnswer: String) {
        // 1. Vô hiệu hóa tất cả các nút để người dùng không chọn lại được nữa.
        optionButtons.forEach { it.isEnabled = false }

        // 2. Kiểm tra xem text trên nút được nhấn có trùng với đáp án đúng không.
        val isCorrect = clickedButton.text.toString() == correctAnswer

        if (isCorrect) {
            score++
            // Phản hồi cho người dùng biết họ đã chọn đúng (ví dụ: đổi màu nút thành xanh)
             clickedButton.strokeColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.correct_green))
        } else {
            // Phản hồi cho người dùng biết họ đã chọn sai (ví dụ: đổi màu nút thành đỏ)
             clickedButton.strokeColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.incorrect_red))
            // Đồng thời, tìm và làm nổi bật đáp án đúng
            optionButtons.find { it.text.toString() == correctAnswer }?.apply {
                // set stroke color to green
                strokeColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.correct_green))
            }
        }

        // 3. Hiển thị nút "Tiếp tục" để người dùng qua câu mới.
        binding.btnNext.visibility = View.VISIBLE
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
            "completedAt" to Timestamp.now()
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

    override fun onDestroy() {
        // Giải phóng tài nguyên TTS khi Fragment bị hủy để tránh memory leak
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}