package com.dex.engrisk.user

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.dex.engrisk.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private val TAG = "RegisterActivity"

    /**
     * lateinit cho phép chúng ta khởi tạo chúng trong onCreate thay vì ngay lúc khai báo.
     */
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Gắn layout vào Activity bằng ViewBinding
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Khởi tạo các đối tượng Firebase
        firebaseAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Thiết lập sự kiện click cho nút "Đăng ký"
        binding.btnRegister.setOnClickListener {
            validateInputAndRegister()
        }

        // Back to Login Activity
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

        // Sử dụng một hàm riêng để kiểm tra, nếu hợp lệ thì mới tiếp tục
        if (isInputValid(email, password)) {
            // Vô hiệu hóa nút để tránh người dùng nhấn nhiều lần
            binding.btnRegister.isEnabled = false
            // Bắt đầu quá trình đăng ký với Firebase
            registerUser(email, password)
        }
    }

    /**
     * Kiểm tra tất cả các điều kiện của email và mật khẩu.
     * @return Trả về true nếu tất cả dữ liệu hợp lệ, ngược lại trả về false.
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
        if (password.length < 6) {
            binding.etPassword.error = "Mật khẩu phải có ít nhất 6 ký tự"
            binding.etPassword.requestFocus()
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
                // Bật lại nút đăng ký để người dùng thử lại
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
            "displayName" to "", // Để trống, cho người dùng cập nhật sau
            "image" to "",
            "role" to "user", // Mặc định là user
            "registrationDate" to com.google.firebase.Timestamp.now(), // Lưu thời gian đăng ký
            "currentLevel" to "Beginner" // Mặc định level ban đầu
        )

        db.collection("users").document(uid)
            .set(userData)
            .addOnSuccessListener {
                // Lưu hồ sơ vào Firestore thành công
                Log.d(TAG, "Hồ sơ người dùng đã được tạo trong Firestore cho UID: $uid")
                Toast.makeText(this, "Đăng ký và tạo hồ sơ thành công!", Toast.LENGTH_SHORT).show()

                // Chuyển hướng đến màn hình đăng nhập
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                // Lưu hồ sơ vào Firestore thất bại
                Log.w(TAG, "Lỗi khi tạo hồ sơ người dùng", e)
                Toast.makeText(this, "Lỗi khi lưu dữ liệu hồ sơ: ${e.message}", Toast.LENGTH_LONG).show()
                // Bật lại nút đăng ký
                binding.btnRegister.isEnabled = true
            }
    }
}