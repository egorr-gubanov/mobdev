package lab3.egor.chat

import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.flow.collectLatest
import lab3.egor.chat.data.network.NetworkMonitor
import lab3.egor.chat.data.repository.ChatRepository
import lab3.egor.chat.data.storage.SettingsStorage
import lab3.egor.chat.data.storage.db.AppDatabase
import lab3.egor.chat.ui.navigation.Screen
import lab3.egor.chat.ui.screens.chats.ChatsScreen
import lab3.egor.chat.ui.screens.image.FullImageScreen
import lab3.egor.chat.ui.screens.login.LoginScreen
import lab3.egor.chat.ui.screens.messages.MessageInput
import lab3.egor.chat.ui.screens.messages.MessagesList
import lab3.egor.chat.ui.screens.messages.MessagesScreen
import lab3.egor.chat.ui.theme.ChatTheme
import lab3.egor.chat.viewmodel.ChatViewModel
import lab3.egor.chat.viewmodel.LoginViewModel
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val storage = SettingsStorage(applicationContext)
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ChatRepository(storage, database.chatDao())
        val networkMonitor = NetworkMonitor(applicationContext)

        setContent {
            ChatTheme {
                val navController = rememberNavController()
                val context = LocalContext.current
                val configuration = LocalConfiguration.current
                val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

                val loginViewModel: LoginViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T = LoginViewModel(repository, storage) as T
                })

                val chatViewModel: ChatViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T = ChatViewModel(repository, networkMonitor) as T
                })

                LaunchedEffect(Unit) {
                    repository.initToken()
                    repository.authErrors.collectLatest {
                        chatViewModel.clearData()
                        loginViewModel.resetState()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    chatViewModel.errorEvents.collectLatest { errorRes ->
                        Toast.makeText(context, errorRes, Toast.LENGTH_SHORT).show()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Login.route,
                        modifier = Modifier.fillMaxSize().systemBarsPadding()
                    ) {
                        composable(Screen.Login.route) {
                            LoginScreen(loginViewModel) {
                                navController.navigate(Screen.Chats.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            }
                        }

                        composable(Screen.Chats.route) {
                            ChatsScreen(
                                viewModel = chatViewModel,
                                onChannelClick = { channel ->
                                    if (isLandscape) {
                                        chatViewModel.selectChannel(channel)
                                    } else {
                                        val encoded = URLEncoder.encode(channel, StandardCharsets.UTF_8.toString())
                                        navController.navigate(Screen.Messages.createRoute(encoded))
                                    }
                                },
                                onLogout = {
                                    chatViewModel.logout {
                                        loginViewModel.resetState()
                                        navController.navigate(Screen.Login.route) {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                },
                                messagesContent = { channel ->
                                    MessagesView(channel, chatViewModel, onImageClick = { link ->
                                        val encoded = URLEncoder.encode(link, StandardCharsets.UTF_8.toString())
                                        navController.navigate(Screen.FullImage.createRoute(encoded))
                                    })
                                }
                            )
                        }

                        composable(
                            route = Screen.Messages.route,
                            arguments = listOf(navArgument("channel") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val encodedChannel = backStackEntry.arguments?.getString("channel") ?: ""
                            val channel = URLDecoder.decode(encodedChannel, StandardCharsets.UTF_8.toString())
                            
                            LaunchedEffect(channel) {
                                chatViewModel.selectChannel(channel)
                            }

                            if (isLandscape) {
                                ChatsScreen(
                                    viewModel = chatViewModel,
                                    onChannelClick = { chatViewModel.selectChannel(it) },
                                    onLogout = {
                                        chatViewModel.logout {
                                            loginViewModel.resetState()
                                            navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                                        }
                                    },
                                    messagesContent = { ch ->
                                        MessagesView(ch, chatViewModel, onImageClick = { link ->
                                            val encoded = URLEncoder.encode(link, StandardCharsets.UTF_8.toString())
                                            navController.navigate(Screen.FullImage.createRoute(encoded))
                                        })
                                    }
                                )
                            } else {
                                MessagesScreen(
                                    channelName = channel,
                                    viewModel = chatViewModel,
                                    onBack = { 
                                        chatViewModel.selectChannel(null)
                                        navController.popBackStack() 
                                    },
                                    onImageClick = { link ->
                                        val encoded = URLEncoder.encode(link, StandardCharsets.UTF_8.toString())
                                        navController.navigate(Screen.FullImage.createRoute(encoded))
                                    }
                                )
                            }
                        }

                        composable(
                            route = Screen.FullImage.route,
                            arguments = listOf(navArgument("link") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val link = backStackEntry.arguments?.getString("link") ?: ""
                            FullImageScreen(link = URLDecoder.decode(link, StandardCharsets.UTF_8.toString())) {
                                navController.popBackStack()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessagesView(
    channelName: String,
    viewModel: ChatViewModel,
    onImageClick: (String) -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    LaunchedEffect(channelName) {
        viewModel.loadMessages(channelName)
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            if (messages.isEmpty() && !isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(R.string.no_messages), color = Color.Gray)
                }
            } else {
                MessagesList(
                    messages = messages,
                    onLoadMore = { viewModel.loadMoreMessages() },
                    onImageClick = onImageClick
                )
            }
        }
        MessageInput(
            onSendText = { text -> viewModel.sendMessage(text) },
            onSendImage = { bytes -> viewModel.sendImage(bytes) }
        )
    }
}
