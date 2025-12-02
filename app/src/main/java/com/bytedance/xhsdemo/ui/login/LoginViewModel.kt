package com.bytedance.xhsdemo.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bytedance.xhsdemo.data.LoginRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 登录页 UI 状态：记录输入内容、加载状态以及错误信息
data class LoginUiState(
    val phone: String = "",
    val password: String = "",
    val agreementChecked: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val loginSuccess: Boolean = false,
    val error: String? = null
)

// 登录业务 ViewModel：处理输入校验、调用仓库登录以及 UI 状态更新
class LoginViewModel(private val repository: LoginRepository) : ViewModel() {

    // 登录页面的 UI 状态
    private val _state = MutableStateFlow(LoginUiState())
    val state = _state.asStateFlow()

    // 用于向 Activity 发送一次性消息（如 Toast 提示）
    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    init {
        // 确保本地存在一个默认账号，方便本地开发体验
        viewModelScope.launch {
            repository.ensureDefaultUser()
        }
    }

    // 手机号输入变化
    fun onPhoneChanged(phone: String) {
        _state.update { it.copy(phone = phone, error = null) }
    }

    // 密码输入变化
    fun onPasswordChanged(password: String) {
        _state.update { it.copy(password = password, error = null) }
    }

    // 勾选协议状态变化
    fun onAgreementChecked(checked: Boolean) {
        _state.update { it.copy(agreementChecked = checked, error = null) }
    }

    // 切换密码可见性
    fun togglePasswordVisibility() {
        _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    // 提交登录：包含前端输入校验和调用仓库登录
    fun submitLogin() {
        val current = _state.value
        if (current.phone.isBlank() || current.password.isBlank()) {
            emitToast("请输入手机号和密码")
            return
        }
        if (!current.agreementChecked) {
            emitToast("请先勾选并同意协议")
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            repository.login(current.phone, current.password)
                .onSuccess {
                    _state.update { it.copy(isLoading = false, loginSuccess = true) }
                    emitToast("登录成功")
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                    emitToast(error.message ?: "登录失败")
                }
        }
    }

    // 发送 Toast 事件，供页面订阅后展示
    private fun emitToast(msg: String) {
        viewModelScope.launch { _events.emit(msg) }
    }
}

// 自定义 ViewModel 工厂：用于向 LoginViewModel 注入 LoginRepository
class LoginViewModelFactory(private val repository: LoginRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
