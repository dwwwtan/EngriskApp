package com.dex.engrisk.lesson

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dex.engrisk.R
import com.dex.engrisk.databinding.FragmentLessonBinding

class LessonFragment : Fragment() {

    companion object {
        //
    }

    // BƯỚC 1: Khởi tạo binding cho Fragment
    private lateinit var binding: FragmentLessonBinding

    // BƯỚC 2: Khởi tạo giao diện cho Fragment
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // BƯỚC 3: Gán binding cho Fragment
        binding = FragmentLessonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // setOnClickListener cho các nút trong LessonFragment
        binding.btnBeginnerStart.setOnClickListener {
            val bundle = bundleOf("levelName" to "Beginner")
            findNavController().navigate(R.id.action_to_lessonListFragment, bundle)
        }

        binding.btnIntermediateStart.setOnClickListener {
            val bundle = bundleOf("levelName" to "Intermediate")
            findNavController().navigate(R.id.action_to_lessonListFragment, bundle)
        }

        binding.btnAdvancedStart.setOnClickListener {
            val bundle = bundleOf("levelName" to "Advanced")
            findNavController().navigate(R.id.action_to_lessonListFragment, bundle)
        }
    }
}
