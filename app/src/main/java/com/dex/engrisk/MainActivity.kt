package com.dex.engrisk

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.dex.engrisk.databinding.ActivityMainBinding
import com.dex.engrisk.model.User
import com.dex.engrisk.viewmodel.MainViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var navHostFragment: NavHostFragment
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    companion object {
        const val TAG = "MainActivity"
    }

    // Khởi tạo ViewModel một cách an toàn, gắn liền với vòng đời của MainActivity
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setUpNavigation()

        // Kiểm tra xem ViewModel đã có dữ liệu chưa. Nếu chưa, tải nó từ Firestore.
        if (mainViewModel.user.value == null) {
            fetchUserProfile()
        }
    }

    private fun setUpNavigation() {
        // Setup NavController với NavigationHostFragment
        navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        navController = navHostFragment.navController

        // Liên kết BottomNavigationView với NavController
        binding.bottomNav.setupWithNavController(navController)
    }

    /**
     * Hàm này sẽ lấy thông tin người dùng từ Firestore và đặt vào ViewModel.
     */
    private fun fetchUserProfile() {
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            return
        }
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Tạo đối tượng User từ dữ liệu Firestore
                    val user = User(
                        uid = document.getString("uid") ?: "",
                        email = document.getString("email") ?: "",
                        displayName = document.getString("displayName") ?: "",
                        currentLevel = document.getString("currentLevel") ?: "Beginner"
                    )
                    // Đặt dữ liệu vào Shared ViewModel
                    mainViewModel.setUser(user)
                    Log.d(TAG, "User profile loaded into ViewModel: ${user.displayName}")
                } else {
                    Log.w(TAG, "User data not found in Firestore for UID: $uid")
                    Toast.makeText(this, "Lỗi: Không tìm thấy dữ liệu người dùng.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error getting user document", e)
                Toast.makeText(this, "Lỗi khi lấy dữ liệu người dùng.", Toast.LENGTH_LONG).show()
            }
    }
}