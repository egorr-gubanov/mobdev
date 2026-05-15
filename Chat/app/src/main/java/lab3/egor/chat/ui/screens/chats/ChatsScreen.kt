package lab3.egor.chat.ui.screens.chats

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import lab3.egor.chat.R
import lab3.egor.chat.ui.theme.AppleBlue
import lab3.egor.chat.ui.theme.AppleGray
import lab3.egor.chat.ui.theme.White
import lab3.egor.chat.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(
    viewModel: ChatViewModel,
    onChannelClick: (String) -> Unit,
    onLogout: () -> Unit,
    messagesContent: @Composable (String) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val channels by viewModel.channels.collectAsState()
    val selectedChannel by viewModel.selectedChannel.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var newChannelName by remember { mutableStateOf("") }

    if (isLandscape && selectedChannel != null) {
        BackHandler { viewModel.selectChannel(null) }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.new_chat_title)) },
            text = {
                OutlinedTextField(
                    value = newChannelName,
                    onValueChange = { newChannelName = it },
                    label = { Text(stringResource(R.string.channel_name_hint)) },
                    placeholder = { Text("my_chat@channel") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newChannelName.isNotBlank()) {
                            onChannelClick(newChannelName.trim())
                            showDialog = false
                            newChannelName = ""
                        }
                    },
                    enabled = newChannelName.isNotBlank()
                ) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.channels_title), fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = { viewModel.logout(onLogout) }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = AppleBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = AppleBlue,
                contentColor = White
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) { padding ->
        if (isLandscape) {
            Row(modifier = Modifier.padding(padding).fillMaxSize()) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.loadChannels(refresh = true) }
                    ) {
                        ChannelList(
                            channels = channels,
                            selectedChannel = selectedChannel,
                            onChannelClick = { viewModel.selectChannel(it) }
                        )
                    }
                }
                VerticalDivider(thickness = 1.dp, color = AppleGray)
                Box(modifier = Modifier.weight(2f).fillMaxHeight().background(AppleGray)) {
                    if (!selectedChannel.isNullOrEmpty()) {
                        messagesContent(selectedChannel!!)
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.select_chat), color = Color.Gray)
                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.padding(padding).fillMaxSize().background(White)) {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.loadChannels(refresh = true) }
                ) {
                    ChannelList(
                        channels = channels,
                        selectedChannel = null,
                        onChannelClick = onChannelClick
                    )
                }
            }
        }
    }
}

@Composable
fun ChannelList(
    channels: List<String>,
    selectedChannel: String?,
    onChannelClick: (String) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(channels) { channel ->
            val isSelected = channel == selectedChannel
            ListItem(
                headlineContent = { 
                    Text(
                        text = channel, 
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) AppleBlue else Color.Unspecified
                    ) 
                },
                modifier = Modifier
                    .clickable { onChannelClick(channel) }
                    .background(if (isSelected) AppleBlue.copy(alpha = 0.08f) else White),
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = AppleGray)
        }
    }
}
