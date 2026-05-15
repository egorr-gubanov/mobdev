package lab3.egor.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import lab3.egor.chat.data.repository.ChatRepository
import lab3.egor.chat.data.storage.SettingsStorage

class LoginViewModel(
    private val repository: ChatRepository,
    private val storage: SettingsStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _savedName = MutableStateFlow("")
    val savedName: StateFlow<String> = _savedName.asStateFlow()

    init {
        checkAutoLogin()
    }

    private fun checkAutoLogin() {
        viewModelScope.launch {
            val name = storage.name.first() ?: ""
            val pass = storage.password.first() ?: ""
            val token = storage.token.first() ?: ""

            _savedName.value = name

            if (name.isNotBlank() && pass.isNotBlank()) {
                if (token.isNotBlank()) {
                    _uiState.value = LoginUiState.Success
                } else {
                    // Есть креды, но нет токена - пробуем войти автоматически
                    login(name, pass)
                }
            }
        }
    }

    fun login(name: String, pass: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            repository.login(name, pass)
                .onSuccess { 
                    _uiState.value = LoginUiState.Success 
                }
                .onFailure { 
                    _uiState.value = LoginUiState.Error 
                }
        }
    }

    fun register(name: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            repository.register(name)
                .onSuccess { pass -> login(name, pass) }
                .onFailure { _uiState.value = LoginUiState.Error }
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
        viewModelScope.launch {
            _savedName.value = storage.name.first() ?: ""
        }
    }
}

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    object Success : LoginUiState()
    object Error : LoginUiState()
}
