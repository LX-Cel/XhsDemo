package com.bytedance.xhsdemo.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bytedance.xhsdemo.data.ProfileRepository
import com.bytedance.xhsdemo.data.local.UserProfileEntity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.update

// 个人资料 UI 状态：记录当前资料数据以及加载中状态
data class ProfileUiState(
    val profile: UserProfileEntity? = null,
    val isLoading: Boolean = false
)

// 个人主页 ViewModel：维护个人资料数据，封装更新逻辑
class ProfileViewModel(private val repository: ProfileRepository) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    // 加载当前个人资料
    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val data = repository.loadProfile()
            _state.update { it.copy(profile = data, isLoading = false) }
        }
    }

    // 更新头像后重新加载资料以刷新 UI
    fun updateAvatar(uri: String) {
        viewModelScope.launch {
            repository.updateAvatar(uri)
            load() // Reload to update UI
        }
    }

    // 更新昵称后重新加载资料以刷新 UI
    fun updateName(name: String) {
        viewModelScope.launch {
            repository.updateName(name)
            load() // Reload to update UI
        }
    }

    // 发送一次性 Toast 事件
    fun showToast(message: String) {
        viewModelScope.launch { _events.emit(message) }
    }
}

// 个人资料 ViewModel 工厂：用于注入 ProfileRepository
class ProfileViewModelFactory(private val repository: ProfileRepository) :
    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            return ProfileViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
