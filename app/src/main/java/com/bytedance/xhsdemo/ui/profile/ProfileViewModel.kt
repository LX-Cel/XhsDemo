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

data class ProfileUiState(
    val profile: UserProfileEntity? = null,
    val isLoading: Boolean = false
)

class ProfileViewModel(private val repository: ProfileRepository) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    fun load() {
        if (_state.value.isLoading) return
        viewModelScope.launch {
            _state.emit(ProfileUiState(isLoading = true))
            val data = repository.loadProfile()
            _state.emit(ProfileUiState(profile = data, isLoading = false))
        }
    }

    fun updateAvatar(uri: String) {
        viewModelScope.launch {
            repository.updateAvatar(uri)
            load() // Reload to update UI
        }
    }

    fun updateName(name: String) {
        viewModelScope.launch {
            repository.updateName(name)
            load() // Reload to update UI
        }
    }

    fun showToast(message: String) {
        viewModelScope.launch { _events.emit(message) }
    }
}

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
