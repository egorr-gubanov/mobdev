package lab3.egor.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import lab3.egor.chat.R
import lab3.egor.chat.data.model.Message
import lab3.egor.chat.data.network.NetworkMonitor
import lab3.egor.chat.data.repository.ChatRepository

class ChatViewModel(
    private val repository: ChatRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _selectedChannel = MutableStateFlow<String?>(null)
    val selectedChannel: StateFlow<String?> = _selectedChannel.asStateFlow()

    val channels: StateFlow<List<String>> = repository.getChannelsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<Message>> = _selectedChannel
        .flatMapLatest { channel ->
            if (channel == null) flowOf(emptyList())
            else repository.getMessagesFlow(channel)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorEvents = MutableSharedFlow<Int>()
    val errorEvents = _errorEvents.asSharedFlow()

    val isOnline = networkMonitor.isOnline.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private var isLastPage = false
    private val loadedChannels = mutableSetOf<String>()

    init {
        loadChannels()
        observeNetwork()
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            networkMonitor.isOnline
                .drop(1) // Пропускаем начальное значение, чтобы не триггерить обновление при старте
                .collect { online ->
                    if (online) {
                        repository.retryPendingMessages()
                        // Обновляем данные только при восстановлении соединения
                        loadChannels(refresh = true)
                        _selectedChannel.value?.let { loadMessages(it, force = true) }
                    }
                }
        }
    }

    fun loadChannels(refresh: Boolean = false) {
        // Если не принудительное обновление и данные уже есть — в сеть не идем
        if (!refresh && channels.value.isNotEmpty()) return

        viewModelScope.launch {
            if (refresh) _isRefreshing.value = true else _isLoading.value = true
            repository.syncChannels()
                .onFailure { 
                    if (refresh) _errorEvents.emit(R.string.error_load_channels) 
                }
            _isRefreshing.value = false
            _isLoading.value = false
        }
    }

    fun selectChannel(channel: String?) {
        if (_selectedChannel.value == channel) return
        _selectedChannel.value = channel
        isLastPage = false
        if (!channel.isNullOrEmpty()) {
            loadMessages(channel)
        }
    }

    fun loadMessages(channel: String, force: Boolean = false) {
        // Если данные для канала уже загружены и мы не форсируем обновление — в сеть не идем
        if (!force && loadedChannels.contains(channel) && messages.value.isNotEmpty()) return
        
        viewModelScope.launch {
            _isLoading.value = true
            repository.syncMessages(channel)
                .onSuccess { loadedChannels.add(channel) }
                .onFailure { 
                    if (isOnline.value) _errorEvents.emit(R.string.error_load_messages)
                }
            _isLoading.value = false
        }
    }

    fun loadMoreMessages() {
        val channel = _selectedChannel.value ?: return
        if (_isLoading.value || isLastPage || !isOnline.value) return
        
        val currentMessages = messages.value
        val lastRealMessage = currentMessages.lastOrNull { it.id > 0 }
        val lastId = lastRealMessage?.id?.toString() ?: return

        viewModelScope.launch {
            _isLoading.value = true
            repository.syncMessages(channel, lastId)
                .onSuccess { 
                    // Room flow обновит UI
                }
                .onFailure { _errorEvents.emit(R.string.error_load_messages) }
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
        }
    }

    fun sendImage(bytes: ByteArray) {
        val channel = _selectedChannel.value ?: return
        if (!isOnline.value) return

        viewModelScope.launch {
            _isLoading.value = true
            val from = repository.getCurrentUser()
            repository.sendImage(channel, from, bytes)
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
        _selectedChannel.value = null
        isLastPage = false
        loadedChannels.clear()
    }
}
