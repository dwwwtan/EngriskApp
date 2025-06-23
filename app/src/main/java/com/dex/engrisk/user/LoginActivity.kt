package com.dex.engrisk.user

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dex.engrisk.MainActivity
import androidx.core.text.HtmlCompat
import com.dex.engrisk.R
import com.dex.engrisk.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private val TAG = "LoginActivity"

    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth

    /**
     * Hàm onStart() được gọi mỗi khi Activity hiển thị ra cho người dùng.
     */
    override fun onStart() {
        super.onStart()
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            // Nếu người dùng đã đăng nhập, bỏ qua màn hình này và vào thẳng app
            navigateToMain()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initFirebase()
        setupClickListeners()
    }

    // Hàm khởi tạo Firebase Auth và Firestore
    private fun initFirebase() {
        firebaseAuth = FirebaseAuth.getInstance()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener { validateInputAndLogin() }
        val registerText = getString(R.string.prompt_no_account)
        binding.tvRegisterPrompt.text = HtmlCompat.fromHtml(registerText, HtmlCompat.FROM_HTML_MODE_LEGACY)
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
            binding.progressBar.visibility = View.VISIBLE
            binding.btnLogin.isEnabled = false
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
                Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                navigateToMain()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "signInWithEmail:failure", e)
                Toast.makeText(this, "Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin.", Toast.LENGTH_LONG).show()
                binding.progressBar.visibility = View.GONE
                binding.btnLogin.isEnabled = true
            }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}