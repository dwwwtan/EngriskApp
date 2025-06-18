package com.dex.engrisk.user

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dex.engrisk.MainActivity
import com.dex.engrisk.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private val TAG = "LoginActivity"

    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    /**
     * Hàm onStart() được gọi mỗi khi Activity hiển thị ra cho người dùng.
     * Đây là nơi lý tưởng để kiểm tra xem người dùng đã đăng nhập từ phiên trước chưa.
     */
    override fun onStart() {
        super.onStart()
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            // Nếu người dùng đã đăng nhập, bỏ qua màn hình này và vào thẳng app
            Toast.makeText(this, "Đang tự động đăng nhập...", Toast.LENGTH_SHORT).show()
            fetchUserDataAndNavigate(currentUser.uid)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Khởi tạo Firebase
        firebaseAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Thiết lập sự kiện click cho nút "Đăng nhập"
        binding.btnLogin.setOnClickListener {
            validateInputAndLogin()
        }

        // Thiết lập sự kiện click cho text "Đăng ký ngay"
        binding.tvRegisterPrompt.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    /**
     * Kiểm tra dữ liệu nhập vào và bắt đầu quá trình đăng nhập.
     */
    private fun validateInputAndLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (isInputValid(email, password)) {
            binding.btnLogin.isEnabled = false // Vô hiệu hóa nút
            loginUser(email, password)
        }
    }

    /**
     * Hàm kiểm tra email và mật khẩu có hợp lệ không.
     */
    private fun isInputValid(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.etEmail.error = "Vui lòng nhập email"
            binding.etEmail.requestFocus()
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Email không hợp lệ"
            binding.etEmail.requestFocus()
            return false
        }
        if (password.isEmpty()) {
            binding.etPassword.error = "Vui lòng nhập mật khẩu"
            binding.etPassword.requestFocus()
            return false
        }
        return true
    }

    /**
     * Gọi Firebase Authentication để đăng nhập bằng email và mật khẩu.
     */
    private fun loginUser(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                Log.d(TAG, "signInWithEmail:success")
                val uid = authResult.user!!.uid
                // Đăng nhập Auth thành công, giờ lấy dữ liệu từ Firestore
                fetchUserDataAndNavigate(uid)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "signInWithEmail:failure", e)
                Toast.makeText(this, "Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin.", Toast.LENGTH_LONG).show()
                binding.btnLogin.isEnabled = true // Bật lại nút
            }
    }

    /**
     * Hàm này nhận UID, truy vấn Firestore để lấy hồ sơ người dùng,
     * sau đó điều hướng đến MainActivity.
     */
    private fun fetchUserDataAndNavigate(uid: String) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    // Lấy dữ liệu thành công
                    val displayName = documentSnapshot.getString("displayName") ?: "Người dùng mới"
                    val currentLevel = documentSnapshot.getString("currentLevel") ?: "Beginner"

                    // Chuyển sang MainActivity và gửi kèm dữ liệu
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("USER_DISPLAY_NAME", displayName)
                    intent.putExtra("USER_CURRENT_LEVEL", currentLevel)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()

                } else {
                    // Lạ: Có tài khoản Auth nhưng không có dữ liệu trong Firestore
                    Log.w(TAG, "User data not found in Firestore for UID: $uid")
                    Toast.makeText(this, "Lỗi: Không tìm thấy dữ liệu người dùng.", Toast.LENGTH_LONG).show()
                    binding.btnLogin.isEnabled = true
                    firebaseAuth.signOut() // Đăng xuất để tránh lỗi
                }
            }
            .addOnFailureListener { e ->
                // Lỗi khi truy vấn Firestore
                Log.w(TAG, "Error getting user document: ", e)
                Toast.makeText(this, "Lỗi khi lấy dữ liệu người dùng.", Toast.LENGTH_LONG).show()
                binding.btnLogin.isEnabled = true
                firebaseAuth.signOut() // Đăng xuất để tránh lỗi
            }
    }
}