package com.dex.engrisk.vocabulary.flashcard

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.bumptech.glide.Glide
import com.dex.engrisk.databinding.FragmentFlashcardBinding
import com.dex.engrisk.model.Vocabulary
import com.google.firebase.firestore.FirebaseFirestore

class FlashcardFragment : Fragment() {

    private val TAG = "FlashcardFragment"
    private lateinit var binding: FragmentFlashcardBinding

    // --- BIẾN TRẠNG THÁI ---
    private var vocabularyList: List<Vocabulary> = emptyList()
    private var currentCardIndex = 0
    private var isFrontVisible = true // Biến để theo dõi mặt thẻ đang hiển thị

    // --- BIẾN CHO HIỆU ỨNG LẬT ---
    private lateinit var frontAnim: AnimatorSet
    private lateinit var backAnim: AnimatorSet


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFlashcardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Tải dữ liệu từ Firestore
        fetchVocabulary()

        // Chuẩn bị các hiệu ứng
        setupAnimations()

        // Gán sự kiện cho việc lật thẻ và các nút bấm
        setupClickListeners()
    }

    private fun fetchVocabulary() {
        // ... code hàm này giữ nguyên ...
        binding.progressBar.visibility = View.VISIBLE

        FirebaseFirestore.getInstance().collection("vocabulary")
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    vocabularyList = querySnapshot.toObjects(Vocabulary::class.java)
                    displayCurrentCard()
                } else {
                    Log.d(TAG, "No vocabulary documents found.")
                }
                binding.progressBar.visibility = View.GONE
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error fetching vocabulary", exception)
                binding.progressBar.visibility = View.GONE
            }
    }

    private fun displayCurrentCard() {
        if (vocabularyList.isNotEmpty() && currentCardIndex in vocabularyList.indices) {
            val vocabulary = vocabularyList[currentCardIndex]

            binding.tvWord.text = vocabulary.word
            binding.tvPronunciation.text = vocabulary.pronunciation
            binding.tvDefinition.text = vocabulary.definition
            binding.tvWordTypeBack.text = vocabulary.type
            binding.tvExample.text = "Example: ${vocabulary.example}"

            Glide.with(this)
                .load(vocabulary.imageUrl)
                .into(binding.ivWordImage)

            // Reset về mặt trước mỗi khi chuyển thẻ
            resetCard()
        }
    }

    // --- CÁC HÀM LOGIC MỚI ---

    private fun setupAnimations() {
        val scale = requireContext().resources.displayMetrics.density
        binding.cardFront.cameraDistance = 8000 * scale
        binding.cardBack.cameraDistance = 8000 * scale

        // Tải hiệu ứng từ animator resource (chúng ta sẽ tạo ở bước sau)
        // Nhưng để đơn giản, ta sẽ tạo bằng code
        // Tạm thời để trống, sẽ thêm logic lật thẻ ở hàm setupClickListeners
    }

    private fun setupClickListeners() {
        // Sự kiện lật thẻ khi nhấn vào
        val flipClickListener = View.OnClickListener {
            flipCard()
        }
        binding.cardFront.setOnClickListener(flipClickListener)
        binding.cardBack.setOnClickListener(flipClickListener)

        // Sự kiện cho nút "Từ tiếp"
        binding.btnNext.setOnClickListener {
            if (currentCardIndex < vocabularyList.size - 1) {
                currentCardIndex++
                displayCurrentCard()
            } else {
                Toast.makeText(requireContext(), "Bạn đã xem hết từ vựng!", Toast.LENGTH_SHORT).show()
            }
        }

        // Sự kiện cho nút "Từ trước"
        binding.btnPrev.setOnClickListener {
            if (currentCardIndex > 0) {
                currentCardIndex--
                displayCurrentCard()
            } else {
                Toast.makeText(requireContext(), "Đây là từ đầu tiên!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun flipCard() {
        if (isFrontVisible) {
            // Lật từ trước ra sau
            binding.cardFront.animate().rotationY(90f).setDuration(300).withEndAction {
                binding.cardFront.visibility = View.GONE
                binding.cardBack.visibility = View.VISIBLE
                binding.cardBack.rotationY = -90f
                binding.cardBack.animate().rotationY(0f).setDuration(300).start()
            }.start()
        } else {
            // Lật từ sau ra trước
            binding.cardBack.animate().rotationY(-90f).setDuration(300).withEndAction {
                binding.cardBack.visibility = View.GONE
                binding.cardFront.visibility = View.VISIBLE
                binding.cardFront.rotationY = 90f
                binding.cardFront.animate().rotationY(0f).setDuration(300).start()
            }.start()
        }
        isFrontVisible = !isFrontVisible
    }

    private fun resetCard() {
        // Hàm này đảm bảo mỗi khi chuyển thẻ mới, nó sẽ luôn hiển thị mặt trước
        isFrontVisible = true
        binding.cardFront.visibility = View.VISIBLE
        binding.cardBack.visibility = View.GONE
        // Reset góc xoay về 0 để tránh lỗi hiển thị sau khi lật
        binding.cardFront.rotationY = 0f
        binding.cardBack.rotationY = 0f
    }
}