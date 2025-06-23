package com.dex.engrisk.vocabulary

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.dex.engrisk.R
import com.dex.engrisk.vocabulary.adapter.TopicAdapter
import com.dex.engrisk.databinding.FragmentVocabularyBinding
import com.dex.engrisk.model.Topic

class VocabularyFragment : Fragment() {

    private lateinit var binding: FragmentVocabularyBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentVocabularyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Danh sách các chủ đề
        val topics = listOf(
            Topic("Animals", R.drawable.ic_animals_24),
            Topic("Fruits", R.drawable.ic_fruits_24),
            Topic("Home", R.drawable.ic_home_24),
            Topic("Character", R.drawable.ic_character_24),
            Topic("Technology", R.drawable.ic_technology_24),
        )

        // Khởi tạo Adapter với danh sách chủ đề và hành động khi click
        val topicAdapter = TopicAdapter(topics) { selectedTopic ->
            // Khi một chủ đề được nhấn, điều hướng đến FlashcardFragment
            // và gửi kèm tên chủ đề đã chọn.
            val bundle = bundleOf("topicName" to selectedTopic.name)
            findNavController().navigate(R.id.action_vocabularyFragment_to_flashcardFragment, bundle)
        }

        // Thiết lập RecyclerView
        binding.rvTopics.apply {
            adapter = topicAdapter
            layoutManager = GridLayoutManager(requireContext(), 2)
        }
    }
}