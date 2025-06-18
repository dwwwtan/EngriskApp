package com.dex.engrisk.lesson

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.navigation.fragment.findNavController
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.dex.engrisk.viewmodel.MainViewModel

import com.dex.engrisk.R
import com.dex.engrisk.databinding.FragmentLessonBinding

class LessonFragment : Fragment() {
    // BƯỚC 1: Khởi tạo binding cho Fragment
    private lateinit var binding: FragmentLessonBinding

    // Lấy thể hiện của Shared ViewModel từ MainActivity
    private val mainViewModel: MainViewModel by activityViewModels()

    companion object {
        const val INTENT_EXTRA_SCREEN_DATA = "screen_data"
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    // BƯỚC 2: Khởi tạo giao diện cho Fragment
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // BƯỚC 3: Gán binding cho Fragment
        binding = FragmentLessonBinding.inflate(inflater, container, false)
        return binding.root // Trả về view gốc của layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Lắng nghe (Observe) dữ liệu người dùng từ ViewModel
        mainViewModel.user.observe(viewLifecycleOwner, Observer { user ->
            // Mỗi khi dữ liệu user trong ViewModel thay đổi, đoạn code này sẽ chạy
            // user ở đây là đối tượng User đã được lấy từ Firestore
            if (user != null) {
                val welcomeName = if (user.displayName.isEmpty()) user.email else user.displayName
                // Giả sử bạn có một TextView trong fragment_lesson.xml với id là tv_user_welcome
                // binding.tvUserWelcome.text = "Xin chào, $welcomeName"
//                Toast.makeText(requireContext(), "Chào mừng, $welcomeName", Toast.LENGTH_SHORT).show()
            }
        })

        // setOnClickListener cho các nút trong Fragment
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

    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
    }


}
