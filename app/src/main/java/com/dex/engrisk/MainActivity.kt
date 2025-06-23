package com.dex.engrisk

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.dex.engrisk.databinding.ActivityMainBinding
import com.dex.engrisk.model.User
import com.dex.engrisk.viewmodel.MainViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var navHostFragment: NavHostFragment
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Khởi tạo ViewModel một cách an toàn, gắn liền với vòng đời của MainActivity
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initFirebase()
        setupUI()

        // Tải dữ liệu người dùng nếu chưa có trong ViewModel
        if (mainViewModel.user.value == null) {
            fetchUserProfile()
        }
    }

    private fun initFirebase() {
        firebaseAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
    }

    private fun setupUI() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Thiết lập Toolbar làm Action Bar chính của Activity
        setSupportActionBar(binding.toolbar)
        // Bỏ đi tiêu đề mặc định của hệ thống
        supportActionBar?.setDisplayShowTitleEnabled(false)
        setUpNavigation()
    }

    private fun setUpNavigation() {
        // Setup NavController với NavigationHostFragment
        navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        navController = navHostFragment.navController

        // AppBarConfiguration định nghĩa các màn hình "cấp cao nhất".
        // Khi ở các màn hình này, Toolbar sẽ không hiển thị nút Back.
        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_lesson, R.id.nav_vocabulary, R.id.nav_progress, R.id.nav_profile)
        )

        // Liên kết BottomNavigationView với NavController
        binding.bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.nav_lesson -> {
                    binding.appbarLayout.visibility = View.VISIBLE
                    supportActionBar?.setDisplayShowTitleEnabled(false)
                }
                else -> {
                    binding.appbarLayout.visibility = View.GONE
                }
            }
        }
    }

    // Override hàm này để xử lý sự kiện khi người dùng nhấn nút Back trên Toolbar
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    /**
     * Hàm này sẽ lấy thông tin người dùng từ Firestore và đặt vào ViewModel.
     */
    private fun fetchUserProfile() {
        val uid = firebaseAuth.currentUser?.uid ?: return

        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val user = document.toObject(User::class.java)?.copy(uid = document.id)
                    if (user != null) {
                        mainViewModel.setUser(user)
                        Log.d(TAG, "User profile loaded into ViewModel: ${user.displayName}")
                    }
                } else {
                    Log.w(TAG, "User data not found in Firestore for UID: $uid")
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error getting user document", e)
            }
    }
}