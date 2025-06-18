package com.dex.engrisk.user

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.dex.engrisk.R
import com.dex.engrisk.databinding.FragmentProfileBinding
import com.dex.engrisk.viewmodel.MainViewModel
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private val TAG = "ProfileFragment"

    private lateinit var binding: FragmentProfileBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Khởi tạo các đối tượng Firebase
        firebaseAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Gán các sự kiện click cho các nút
        setupClickListeners()

        // Lắng nghe và hiển thị dữ liệu người dùng
        observeUserData()
    }

    private fun setupClickListeners() {
        binding.btnEditName.setOnClickListener { showEditNameDialog() }
        binding.btnLogout.setOnClickListener { showLogoutDialog() }
        binding.btnChangePassword.setOnClickListener { showChangePasswordDialog() }
        binding.btnDeleteAccount.setOnClickListener { showDeleteDialog() }
    }

    /**
     * Hàm này có nhiệm vụ "lắng nghe" hoặc "quan sát" (observe) dữ liệu người dùng
     * từ MainViewModel.
     *
     * Bất cứ khi nào dữ liệu người dùng trong ViewModel thay đổi (ví dụ: sau khi
     * người dùng đăng nhập hoặc sau khi họ cập nhật tên), đoạn code bên trong khối
     * lệnh này sẽ được tự động thực thi để cập nhật lại giao diện cho khớp với
     * dữ liệu mới nhất.
     */
    private fun observeUserData() {
        mainViewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                binding.tvEmail.text = user.email
                binding.tvDisplayName.text = if (user.displayName.isEmpty()) {
                    "Chưa có tên hiển thị"
                } else {
                    user.displayName
                }
            }
        }
    }

//    private fun observeUserData() {
//        // mainViewModel.user là một đối tượng LiveData. Nó giống như một "kênh" truyền dữ liệu.
//        // Lệnh .observe() là hành động đăng ký để "lắng nghe" các cập nhật từ kênh này.
//        //
//        // Tham số 'viewLifecycleOwner' là một phần rất quan trọng và thông minh của Android Jetpack.
//        // Nó đảm bảo rằng việc lắng nghe này chỉ hoạt động khi giao diện của Fragment (View)
//        // đang trong trạng thái an toàn (tức là đã được tạo ra và chưa bị hủy).
//        // Nó cũng tự động hủy đăng ký lắng nghe khi Fragment bị phá hủy, giúp ứng dụng
//        // không bị lỗi và tránh rò rỉ bộ nhớ (memory leak).
//        mainViewModel.user.observe(viewLifecycleOwner) { user ->
//            // 'user' ở đây chính là đối tượng User (model) mới nhất được gửi từ LiveData.
//            // Đoạn code trong dấu ngoặc { ... } này sẽ được gọi mỗi khi có dữ liệu mới.
//
//            // Luôn kiểm tra để chắc chắn rằng đối tượng user nhận được không phải là null.
//            if (user != null) {
//                // Lấy email từ đối tượng user và gán nó vào TextView có id là tv_email.
//                binding.tvEmail.text = user.email
//            }
//        }
//    }

    //======================================================================
    // --- SECTION: THAY ĐỔI TÊN HIỂN THỊ ---
    //======================================================================
    private fun showEditNameDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_name, null)
        val etNewName = dialogView.findViewById<TextInputEditText>(R.id.et_new_display_name)
        etNewName.setText(mainViewModel.user.value?.displayName)

        AlertDialog.Builder(requireContext())
            .setTitle("Thay đổi tên hiển thị")
            .setView(dialogView)
            .setPositiveButton("Lưu") { _, _ ->
                val newName = etNewName.text.toString().trim()
                if (newName.isNotEmpty()) {
                    updateDisplayName(newName)
                } else {
                    Toast.makeText(requireContext(), "Tên không được để trống", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun updateDisplayName(newName: String) {
        val uid = firebaseAuth.currentUser?.uid ?: return

        db.collection("users").document(uid)
            .update("displayName", newName)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Cập nhật tên thành công!", Toast.LENGTH_SHORT).show()
                val currentUser = mainViewModel.user.value
                if (currentUser != null) {
                    val updatedUser = currentUser.copy(displayName = newName)
                    mainViewModel.setUser(updatedUser)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    //======================================================================
    // --- SECTION: THAY ĐỔI MẬT KHẨU ---
    //======================================================================
    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null)
        val etCurrentPass = dialogView.findViewById<TextInputEditText>(R.id.et_current_password)
        val etNewPass = dialogView.findViewById<TextInputEditText>(R.id.et_new_password)
        val etConfirmNewPass = dialogView.findViewById<TextInputEditText>(R.id.et_confirm_new_password)

        AlertDialog.Builder(requireContext())
            .setTitle("Thay đổi mật khẩu")
            .setView(dialogView)
            .setPositiveButton("Lưu") { _, _ ->
                val currentPass = etCurrentPass.text.toString()
                val newPass = etNewPass.text.toString()
                val confirmPass = etConfirmNewPass.text.toString()
                changePassword(currentPass, newPass, confirmPass)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun changePassword(currentPass: String, newPass: String, confirmPass: String) {
        if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
            return
        }
        if (newPass.length < 6) {
            Toast.makeText(requireContext(), "Mật khẩu mới phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show()
            return
        }
        if (newPass != confirmPass) {
            Toast.makeText(requireContext(), "Mật khẩu mới không khớp", Toast.LENGTH_SHORT).show()
            return
        }

        val user = firebaseAuth.currentUser ?: return
        val credential = EmailAuthProvider.getCredential(user.email!!, currentPass)

        user.reauthenticate(credential)
            .addOnSuccessListener {
                Log.d(TAG, "User re-authenticated successfully.")
                user.updatePassword(newPass)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Lỗi khi cập nhật mật khẩu: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Mật khẩu hiện tại không đúng.", Toast.LENGTH_LONG).show()
            }
    }

    //======================================================================
    // --- SECTION: XÓA TÀI KHOẢN ---
    //======================================================================
    private fun showDeleteDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_delete_account, null)
        val etPassword = dialogView.findViewById<TextInputEditText>(R.id.et_password_confirm)

        AlertDialog.Builder(requireContext())
            .setTitle("Bạn có chắc chắn không?")
            .setMessage("Hành động này không thể hoàn tác. Toàn bộ dữ liệu của bạn sẽ bị xóa vĩnh viễn.")
            .setView(dialogView)
            .setPositiveButton("Xóa") { _, _ ->
                val password = etPassword.text.toString()
                if (password.isNotEmpty()) {
                    deleteUserAccount(password)
                } else {
                    Toast.makeText(requireContext(), "Vui lòng nhập mật khẩu", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun deleteUserAccount(password: String) {
        val user = firebaseAuth.currentUser ?: return
        val credential = EmailAuthProvider.getCredential(user.email!!, password)

        // BƯỚC A: XÁC THỰC LẠI
        user.reauthenticate(credential).addOnSuccessListener {
            Log.d(TAG, "User re-authenticated for deletion.")

            // BƯỚC B: XÓA DỮ LIỆU FIRESTORE
            db.collection("users").document(user.uid).delete()
                .addOnSuccessListener {
                    Log.d(TAG, "Firestore user data deleted.")

                    // BƯỚC C: XÓA TÀI KHOẢN AUTHENTICATION
                    user.delete().addOnSuccessListener {
                        Log.d(TAG, "User account deleted from Authentication.")
                        Toast.makeText(requireContext(), "Tài khoản đã được xóa vĩnh viễn.", Toast.LENGTH_LONG).show()

                        // Quay về màn hình Login
                        val intent = Intent(requireActivity(), LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        requireActivity().finish()
                    }.addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Lỗi khi xóa tài khoản: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }.addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Lỗi khi xóa dữ liệu: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Mật khẩu không đúng.", Toast.LENGTH_LONG).show()
        }
    }

    //======================================================================
    // --- SECTION: ĐĂNG XUẤT ---
    //======================================================================
    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Đăng xuất")
            .setMessage("Bạn có chắc chắn muốn đăng xuất không?")
            .setPositiveButton("Đăng xuất") { _, _ ->
                firebaseAuth.signOut()
                Toast.makeText(requireContext(), "Đăng xuất thành công", Toast.LENGTH_SHORT).show()
                val intent = Intent(requireActivity(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}