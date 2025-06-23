package com.dex.engrisk.vocabulary.flashcard

import android.animation.Animator
import android.animation.AnimatorInflater
import android.annotation.SuppressLint
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.bumptech.glide.Glide
import com.dex.engrisk.R
import com.dex.engrisk.databinding.FragmentFlashcardBinding
import com.dex.engrisk.model.Vocabulary
import com.dex.engrisk.model.VocabularyProgress
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Locale

class FlashcardFragment : Fragment(), TextToSpeech.OnInitListener {

    private val TAG = "FlashcardFragment"
    private lateinit var binding: FragmentFlashcardBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var frontAnim: Animator
    private lateinit var backAnim: Animator
    private lateinit var tts: TextToSpeech

    // --- BIẾN TRẠNG THÁI ---
    private var vocabularyList: List<Vocabulary> = emptyList()
    private var currentCardIndex = 0
    private var isFrontVisible = true

    // --- BIẾN LOGIC VUỐT ---
    private var x1: Float = 0f
    private var x2: Float = 0f
    private val MIN_SWIPE_DISTANCE = 150 // Ngưỡng vuốt, có thể điều chỉnh

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentFlashcardBinding.inflate(inflater, container, false)
        tts = TextToSpeech(requireContext(), this)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initFirebase()
        setupAnimations()
        fetchVocabulary()
        setupClickListeners()
    }

    private fun initFirebase() {
        db = FirebaseFirestore.getInstance()
        firebaseAuth = FirebaseAuth.getInstance()
    }

    // Thiết lập các animation cho thẻ
    private fun setupAnimations() {
        val scale = requireContext().resources.displayMetrics.density
        binding.cardFront.cameraDistance = 8000 * scale
        binding.cardBack.cameraDistance = 8000 * scale
        frontAnim = AnimatorInflater.loadAnimator(requireContext(), R.animator.flip_out)
        backAnim = AnimatorInflater.loadAnimator(requireContext(), R.animator.flip_in)
    }

    // Implement hàm onInit của TTS
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "The specified language is not supported!")
                binding.btnSpeak.isEnabled = false
            } else {
                binding.btnSpeak.isEnabled = true
                Log.d(TAG, "TTS Initialized Successfully!")
                // Phát âm câu đầu tiên nếu có
                if (vocabularyList.isNotEmpty()) {
                    speakCurrentWord()
                }
            }
        } else {
            Log.e(TAG, "TTS Initialization Failed!")
            binding.btnSpeak.isEnabled = false
        }
    }

    private fun speakCurrentWord() {
        if (vocabularyList.isNotEmpty() && currentCardIndex in vocabularyList.indices) {
            val wordToSpeak = vocabularyList[currentCardIndex].word
            tts.speak(wordToSpeak, TextToSpeech.QUEUE_FLUSH, null, "")
        }
    }

    private fun fetchVocabulary() {
        binding.progressBar.visibility = View.VISIBLE
        val topicName = arguments?.getString("topicName")
        var query: Query = db.collection("vocabulary")
        // Nếu có tên chủ đề, thêm điều kiện lọc vào câu truy vấn
        if (!topicName.isNullOrEmpty()) {
            query = query.whereEqualTo("topic", topicName)
        }
        // Thực thi truy vấn
        query.get().addOnSuccessListener { querySnapshot ->
            binding.progressBar.visibility = View.GONE
            if (!querySnapshot.isEmpty) {
                vocabularyList = querySnapshot.toObjects(Vocabulary::class.java)
                displayCurrentCard()
            } else {
                Toast.makeText(requireContext(), "Chưa có từ vựng cho chủ đề này.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { exception ->
            binding.progressBar.visibility = View.GONE
            Log.e(TAG, "Error fetching vocabulary for topic: $topicName", exception)
        }
    }

    private fun displayCurrentCard() {
        if (vocabularyList.isNotEmpty() && currentCardIndex in vocabularyList.indices) {
            val vocabulary = vocabularyList[currentCardIndex]
            val progressPercentage = ((currentCardIndex + 1) * 100 / vocabularyList.size)
            binding.progressIndicator.progress = progressPercentage
            binding.tvWord.text = vocabulary.word
            binding.tvPronunciation.text = vocabulary.pronunciation
            binding.tvDefinition.text = vocabulary.definition
            binding.tvWordTypeBack.text = vocabulary.type
            binding.tvExample.text = "Example: ${vocabulary.example}"
            Glide.with(this).load(vocabulary.imageUrl).into(binding.ivWordImage)
            resetCard()
            speakCurrentWord()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupClickListeners() {
        val flipClickListener = View.OnClickListener { flipCard() }
        binding.cardFront.setOnClickListener(flipClickListener)
        binding.cardBack.setOnClickListener(flipClickListener)
        // Gán sự kiện lắng nghe thao tác vuốt cho khu vực chứa thẻ
        binding.cardContainer.setOnTouchListener { _, event ->
            handleSwipe(event)
            // Trả về false để các sự kiện click khác (như lật thẻ) vẫn hoạt động
            false
        }
        binding.btnIKnow.setOnClickListener { saveWordProgress(isLearned = true) }
        binding.btnDontKnow.setOnClickListener { saveWordProgress(isLearned = false) }
        binding.btnNext.setOnClickListener { handleNextWord() }
        binding.btnPrev.setOnClickListener { handlePrevWord() }
        binding.btnSpeak.setOnClickListener { speakCurrentWord() }
    }

    // --- HÀM XỬ LÝ VUỐT ---
    private fun handleSwipe(event: MotionEvent) {
        when (event.action) {
            // Khi người dùng bắt đầu chạm vào màn hình
            MotionEvent.ACTION_DOWN -> {
                x1 = event.x
            }
            // Khi người dùng nhấc ngón tay lên
            MotionEvent.ACTION_UP -> {
                x2 = event.x
                val deltaX = x2 - x1

                // Kiểm tra xem khoảng cách có đủ lớn để coi là một cú vuốt không
                if (deltaX < -MIN_SWIPE_DISTANCE) {
                    // Vuốt sang trái -> Qua từ tiếp theo
                    handleNextWord()
                } else if (deltaX > MIN_SWIPE_DISTANCE) {
                    // Vuốt sang phải -> Quay lại từ trước
                    handlePrevWord()
                }
            }
        }
    }

    private fun flipCard() {
        if (isFrontVisible) {
            frontAnim.setTarget(binding.cardFront)
            backAnim.setTarget(binding.cardBack)
            frontAnim.start()
            backAnim.start()
            binding.cardBack.visibility = View.VISIBLE
            binding.cardFront.visibility = View.GONE
        } else {
            frontAnim.setTarget(binding.cardBack)
            backAnim.setTarget(binding.cardFront)
            frontAnim.start()
            backAnim.start()
            binding.cardFront.visibility = View.VISIBLE
            binding.cardBack.visibility = View.GONE
        }
        isFrontVisible = !isFrontVisible
    }

    private fun handleNextWord() {
        if (currentCardIndex < vocabularyList.size - 1) {
            currentCardIndex++
            displayCurrentCard()
        } else {
            Toast.makeText(requireContext(), "Bạn đã xem hết từ vựng!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handlePrevWord() {
        if (currentCardIndex > 0) {
            currentCardIndex--
            displayCurrentCard()
        } else {
            Toast.makeText(requireContext(), "Đây là từ đầu tiên!", Toast.LENGTH_SHORT).show()
        }
    }

    // --- HÀM LƯU TIẾN ĐỘ TỪ VỰNG ---
    private fun saveWordProgress(isLearned: Boolean) {
        if (currentCardIndex !in vocabularyList.indices) return
        val uid = firebaseAuth.currentUser?.uid ?: return
        val wordId = vocabularyList[currentCardIndex].id

        setInteractionEnabled(false)

        val progressData = VocabularyProgress(isLearned = isLearned)

        val fieldToUpdate = "vocabularyProgress.$wordId"

        db.collection("userProgress").document(uid)
            .update(fieldToUpdate, progressData)
            .addOnSuccessListener {
                Log.d(TAG, "Progress for word '$wordId' saved.")
                // Tự động chuyển sang từ tiếp theo để luồng học mượt mà
                handleNextWord()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error saving word progress", e)
                Toast.makeText(requireContext(), "Lỗi khi lưu tiến độ", Toast.LENGTH_SHORT).show()
            }
            .addOnCompleteListener {
                setInteractionEnabled(true)
            }
    }

    // --- HÀM RESETCARD ---
    private fun resetCard() {
        // Hủy các animation đang chạy (nếu có) để tránh lỗi
        frontAnim.cancel()
        backAnim.cancel()

        isFrontVisible = true
        // Reset góc xoay về 0
        binding.cardFront.rotationY = 0f
        binding.cardBack.rotationY = 0f
        // Reset hiển thị về trạng thái ban đầu
        binding.cardFront.visibility = View.VISIBLE
        binding.cardBack.visibility = View.GONE
    }

    private fun setInteractionEnabled(enabled: Boolean) {
        binding.btnIKnow.isEnabled = enabled
        binding.btnDontKnow.isEnabled = enabled
        binding.btnNext.isEnabled = enabled
        binding.btnPrev.isEnabled = enabled
    }

    override fun onDestroyView() {
        // Thay vì onDestroy, dùng onDestroyView trong Fragment sẽ an toàn hơn cho các tác vụ liên quan đến View
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroyView()
    }
}