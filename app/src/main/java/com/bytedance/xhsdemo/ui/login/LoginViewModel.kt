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

data class LoginUiState(
    val phone: String = "",
    val password: String = "",
    val agreementChecked: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val loginSuccess: Boolean = false,
    val error: String? = null
)

class LoginViewModel(private val repository: LoginRepository) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultUser()
        }
    }

    fun onPhoneChanged(phone: String) {
        _state.update { it.copy(phone = phone, error = null) }
    }

    fun onPasswordChanged(password: String) {
        _state.update { it.copy(password = password, error = null) }
    }

    fun onAgreementChecked(checked: Boolean) {
        _state.update { it.copy(agreementChecked = checked, error = null) }
    }

    fun togglePasswordVisibility() {
        _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

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

    private fun emitToast(msg: String) {
        viewModelScope.launch { _events.emit(msg) }
    }
}

class LoginViewModelFactory(private val repository: LoginRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
