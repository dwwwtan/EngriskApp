package com.dex.engrisk.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dex.engrisk.model.User

class MainViewModel : ViewModel() {

    // Dùng MutableLiveData để có thể thay đổi giá trị bên trong ViewModel
    private val _user = MutableLiveData<User>()

    // Cung cấp một LiveData công khai, chỉ có thể đọc, cho các Fragment
    // Điều này đảm bảo chỉ ViewModel mới có quyền thay đổi dữ liệu
    val user: LiveData<User> get() = _user

    // Hàm để cập nhật dữ liệu người dùng từ bên ngoài (ví dụ: từ MainActivity)
    fun setUser(userData: User) {
        _user.value = userData
    }
}