package com.dex.engrisk.user

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.dex.engrisk.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private val TAG = "RegisterActivity"
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initFirebase()
        setupClickListeners()
    }

    // Khởi tạo Firebase
    private fun initFirebase() {
        firebaseAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
    }

    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener { validateInputAndRegister() }
        binding.tvLoginPrompt.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    /**
     * Hàm chính để điều phối việc kiểm tra dữ liệu và thực hiện đăng ký.
     */
    private fun validateInputAndRegister() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        if (isInputValid(email, password, confirmPassword)) {
            binding.progressBar.visibility = View.VISIBLE
            binding.btnRegister.isEnabled = false
            registerUser(email, password)
        }
    }

    /**
     * Kiểm tra tất cả các điều kiện của email và mật khẩu.
     * @return Trả về true nếu tất cả dữ liệu hợp lệ, ngược lại trả về false.
     */
    private fun isInputValid(email: String, password: String, confirmPassword: String): Boolean {
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
        if (password.length < 6) {
            binding.etPassword.error = "Mật khẩu phải có ít nhất 6 ký tự"
            binding.etPassword.requestFocus()
            return false
        }
        if (password != confirmPassword) {
            binding.etConfirmPassword.error = "Mật khẩu xác nhận không khớp"
            binding.etConfirmPassword.requestFocus()
            return false
        }
        return true
    }

    /**
     * Gọi Firebase Authentication để tạo người dùng mới bằng email và mật khẩu.
     */
    private fun registerUser(email: String, password: String) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                // Đăng ký Authentication thành công
                Log.d(TAG, "createUserWithEmail:success")
                val firebaseUser = authResult.user
                // uid không thể null ở đây nếu task thành công
                saveUser(firebaseUser!!.uid, email)
            }
            .addOnFailureListener { e ->
                // Đăng ký Authentication thất bại
                Log.w(TAG, "createUserWithEmail:failure", e)
                Toast.makeText(this, "Đăng ký thất bại: ${e.message}", Toast.LENGTH_LONG).show()
                binding.progressBar.visibility = View.GONE
                binding.btnRegister.isEnabled = true
            }
    }

    /**
     * Sau khi tạo tài khoản thành công, lưu thông tin hồ sơ người dùng vào Firestore.
     */
    private fun saveUser(uid: String, email: String) {
        // Tạo một Map để chứa dữ liệu, đúng theo cấu trúc của dự án Engrisk
        val userData = hashMapOf(
            "uid" to uid,
            "email" to email,
            "displayName" to "",
            "imageUrl" to "",
            "role" to "user",
            "registrationDate" to com.google.firebase.Timestamp.now(),
            "currentLevel" to "Beginner"
        )

        db.collection("users").document(uid)
            .set(userData)
            .addOnSuccessListener {
                // Lưu hồ sơ vào Firestore thành công
                Log.d(TAG, "Hồ sơ người dùng đã được tạo trong Firestore cho UID: $uid")
                Toast.makeText(this, "Đăng ký và tạo hồ sơ thành công!", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
                // Chuyển hướng đến màn hình đăng nhập
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Lỗi khi tạo hồ sơ người dùng", e)
                Toast.makeText(this, "Lỗi khi lưu dữ liệu hồ sơ: ${e.message}", Toast.LENGTH_LONG).show()
                binding.progressBar.visibility = View.GONE
                binding.btnRegister.isEnabled = true
            }
    }
}