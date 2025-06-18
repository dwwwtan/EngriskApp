package com.dex.engrisk // Thay bằng package name của bạn

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.dex.engrisk.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationBarView
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.dex.engrisk.model.User
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.commit
import androidx.core.content.edit
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.dex.engrisk.lesson.LessonFragment
import com.dex.engrisk.user.ProfileFragment
import com.dex.engrisk.progress.ProgressFragment
import com.dex.engrisk.viewmodel.MainViewModel
import com.dex.engrisk.vocabulary.VocabularyFragment
import com.google.android.material.snackbar.Snackbar
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
        //
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