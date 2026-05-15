package lab3.egor.chat.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import lab3.egor.chat.R
import lab3.egor.chat.data.model.Message
import lab3.egor.chat.data.repository.ChatRepository

class ChatViewModel(private val repository: ChatRepository) : ViewModel() {

    private val _channels = MutableStateFlow<List<String>>(emptyList())
    val channels: StateFlow<List<String>> = _channels.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _selectedChannel = MutableStateFlow<String?>(null)
    val selectedChannel: StateFlow<String?> = _selectedChannel.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorEvents = MutableSharedFlow<Int>()
    val errorEvents = _errorEvents.asSharedFlow()

    private var isLastPage = false

    init {
        loadChannels()
    }

    fun loadChannels(refresh: Boolean = false) {
        if (!refresh && _channels.value.isNotEmpty()) return
        viewModelScope.launch {
            if (refresh) _isRefreshing.value = true else _isLoading.value = true
            repository.getChannels()
                .onSuccess { _channels.value = it }
                .onFailure { _errorEvents.emit(R.string.error_load_channels) }
            _isRefreshing.value = false
            _isLoading.value = false
        }
    }

    fun selectChannel(channel: String?) {
        if (_selectedChannel.value == channel && _messages.value.isNotEmpty()) return
        _selectedChannel.value = channel
        _messages.value = emptyList()
        isLastPage = false
        if (!channel.isNullOrEmpty()) {
            loadMessages(channel)
        }
    }

    fun loadMessages(channel: String) {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            repository.getMessages(channel)
                .onSuccess { 
                    _messages.value = it
                    isLastPage = it.size < 20
                }
                .onFailure { _errorEvents.emit(R.string.error_load_messages) }
            _isLoading.value = false
        }
    }

    fun loadMoreMessages() {
        val channel = _selectedChannel.value ?: return
        if (_isLoading.value || isLastPage) return
        val lastId = _messages.value.lastOrNull()?.id?.toString() ?: return

        viewModelScope.launch {
            _isLoading.value = true
            repository.getMessages(channel, lastId)
                .onSuccess { newMessages ->
                    if (newMessages.isEmpty()) {
                        isLastPage = true
                    } else {
                        _messages.value = _messages.value + newMessages
                        if (newMessages.size < 20) isLastPage = true
                    }
                }
            _isLoading.value = false
        }
    }

    fun sendMessage(text: String) {
        val channel = _selectedChannel.value ?: return
        val trimmedText = text.trim()
        if (trimmedText.isEmpty()) return

        viewModelScope.launch {
            val from = repository.getCurrentUser()
            repository.sendMessage(channel, trimmedText, from)
                .onSuccess {
                    repository.getMessages(channel).onSuccess { _messages.value = it }
                }
                .onFailure { _errorEvents.emit(R.string.error_send_message) }
        }
    }

    fun sendImage(bytes: ByteArray) {
        val channel = _selectedChannel.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val from = repository.getCurrentUser()
            repository.sendImage(channel, from, bytes)
                .onSuccess {
                    repository.getMessages(channel).onSuccess { _messages.value = it }
                }
                .onFailure { _errorEvents.emit(R.string.error_send_message) }
            _isLoading.value = false
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.logout()
            clearData()
            onComplete()
        }
    }

    fun clearData() {
        _channels.value = emptyList()
        _messages.value = emptyList()
        _selectedChannel.value = null
        isLastPage = false
    }
}
