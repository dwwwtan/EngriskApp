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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initFirebase()
        setupClickListeners()
        observeUserData()
    }

    private fun initFirebase() {
        firebaseAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
    }

    private fun setupClickListeners() {
        binding.btnEditName.setOnClickListener { showEditNameDialog() }
        binding.btnLogout.setOnClickListener { showLogoutDialog() }
        binding.btnChangePassword.setOnClickListener { showChangePasswordDialog() }
        binding.btnDeleteAccount.setOnClickListener { showDeleteDialog() }
    }

    /**
     * Hàm này có nhiệm vụ "lắng nghe" hoặc "quan sát" (observe) dữ liệu người dùng từ MainViewModel.
     * giúp giao diện luôn hiển thị đúng thông tin user mới nhất mỗi khi dữ liệu user thay đổi trong ViewModel.
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
            .setNegativeButton("Hủy", null).show()
    }

    private fun updateDisplayName(newName: String) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        showLoading(true)

        db.collection("users").document(uid).update("displayName", newName)
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
            .addOnCompleteListener {
                showLoading(false)
            }
    }

    //======================================================================
    // --- SECTION: THAY ĐỔI MẬT KHẨU ---
    //======================================================================
    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null)
        val etCurrentPass = dialogView.findViewById<TextInputEditText>(R.id.et_current_password)
        val etNewPass = dialogView.findViewById<TextInputEditText>(R.id.et_new_password)
        val etConfirmNewPass =
            dialogView.findViewById<TextInputEditText>(R.id.et_confirm_new_password)

        AlertDialog.Builder(requireContext()).setTitle("Thay đổi mật khẩu").setView(dialogView)
            .setPositiveButton("Lưu") { _, _ ->
                val currentPass = etCurrentPass.text.toString()
                val newPass = etNewPass.text.toString()
                val confirmPass = etConfirmNewPass.text.toString()
                changePassword(currentPass, newPass, confirmPass)
            }
            .setNegativeButton("Hủy", null).show()
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
        showLoading(true)

        // Đây là bước xác thực lại danh tính người dùng.
        // Firebase yêu cầu điều này cho các hành động nhạy cảm để đảm bảo an toàn.
        val credential = EmailAuthProvider.getCredential(user.email!!, currentPass)
        user.reauthenticate(credential)
            .addOnSuccessListener {
                Log.d(TAG, "User re-authenticated successfully.")
                // Sau khi xác thực lại thành công, tiến hành đổi mật khẩu.
                user.updatePassword(newPass)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(requireContext(), "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "Lỗi khi cập nhật mật khẩu: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                        showLoading(false)
                    }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Mật khẩu hiện tại không đúng.", Toast.LENGTH_LONG).show()
                showLoading(false)
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
            .setNegativeButton("Hủy", null).show()
    }

    private fun deleteUserAccount(password: String) {
        val user = firebaseAuth.currentUser ?: return
        showLoading(true)

        // BƯỚC A: XÁC THỰC LẠI
        user.reauthenticate(EmailAuthProvider.getCredential(user.email!!, password))
            .addOnCompleteListener { reauthTask ->
                if (reauthTask.isSuccessful) {
                    Log.d(TAG, "User re-authenticated for deletion.")
                    // BƯỚC B: XÓA DỮ LIỆU FIRESTORE
                    db.collection("users").document(user.uid).delete()
                        .addOnCompleteListener { firestoreTask ->
                            if (firestoreTask.isSuccessful) {
                                Log.d(TAG, "Firestore user data deleted.")
                                // BƯỚC C: XÓA TÀI KHOẢN AUTH
                                user.delete().addOnCompleteListener { authDeleteTask ->
                                    showLoading(false) // Ẩn loading
                                    if (authDeleteTask.isSuccessful) {
                                        Toast.makeText(requireContext(), "Tài khoản đã được xóa.", Toast.LENGTH_LONG).show()
                                        navigateToLogin()
                                    } else {
                                        Toast.makeText(requireContext(), "Lỗi khi xóa tài khoản: ${authDeleteTask.exception?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } else {
                                showLoading(false)
                                Toast.makeText(requireContext(), "Lỗi khi xóa dữ liệu: ${firestoreTask.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                } else {
                    showLoading(false)
                    Toast.makeText(requireContext(), "Mật khẩu không đúng.", Toast.LENGTH_LONG).show()
                }
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
                navigateToLogin()
            }
            .setNegativeButton("Hủy", null).show()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnEditName.isEnabled = !isLoading
        binding.btnChangePassword.isEnabled = !isLoading
        binding.btnDeleteAccount.isEnabled = !isLoading
        binding.btnLogout.isEnabled = !isLoading
    }

    private fun navigateToLogin() {
        val intent = Intent(requireActivity(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
}