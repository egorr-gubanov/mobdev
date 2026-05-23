package lab3.egor.chat.ui.screens.messages

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import lab3.egor.chat.R
import lab3.egor.chat.data.model.Message
import lab3.egor.chat.ui.theme.AppleBlue
import lab3.egor.chat.ui.theme.AppleGray
import lab3.egor.chat.ui.theme.TextGray
import lab3.egor.chat.ui.theme.White
import lab3.egor.chat.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    channelName: String,
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    onImageClick: (String) -> Unit
) {
    val isOnline by viewModel.isOnline.collectAsState()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(channelName, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                            if (!isOnline) {
                                Text(
                                    stringResource(R.string.offline_mode),
                                    fontSize = 12.sp,
                                    color = Color.Red
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = AppleBlue)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = White)
                )
                OfflineBanner(isOnline = isOnline)
            }
        },
        containerColor = White
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            MessagesContent(
                viewModel = viewModel,
                onImageClick = onImageClick
            )
        }
    }
}

@Composable
fun OfflineBanner(isOnline: Boolean) {
    AnimatedVisibility(
        visible = !isOnline,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Surface(
            color = Color.Red.copy(alpha = 0.1f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CloudOff, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.no_connection_hint), color = Color.Red, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun MessagesContent(
    viewModel: ChatViewModel,
    onImageClick: (String) -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            if (messages.isEmpty() && !isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.no_messages),
                        color = TextGray,
                        textAlign = TextAlign.Center
                    )
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
            onSendImage = { bytes -> viewModel.sendImage(bytes) },
            isOnline = isOnline
        )
    }
}

@Composable
fun MessagesList(
    messages: List<Message>,
    onLoadMore: () -> Unit,
    onImageClick: (String) -> Unit
) {
    val listState = rememberLazyListState()
    val shouldLoadMore = remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItem >= totalItems - 5
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) onLoadMore()
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        reverseLayout = true
    ) {
        items(messages, key = { it.id.toString() }) { message ->
            MessageBubble(message, onImageClick)
        }
    }
}

@Composable
fun MessageBubble(message: Message, onImageClick: (String) -> Unit) {
    val isPending = message.id < 0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isPending) 0.6f else 1f),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = message.from,
            style = MaterialTheme.typography.labelSmall,
            color = AppleBlue,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(if (message.data.Text != null) AppleGray else Color.Transparent)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                message.data.Text?.let {
                    Text(text = it.text, fontSize = 16.sp, lineHeight = 20.sp)
                }
                message.data.Image?.let { img ->
                    AsyncImage(
                        model = "https://faerytea.name/thumb/${img.link}",
                        contentDescription = null,
                        modifier = Modifier
                            .widthIn(max = 240.dp)
                            .heightIn(max = 320.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onImageClick(img.link) },
                        contentScale = ContentScale.Crop
                    )
                }
                Row(
                    modifier = Modifier.align(Alignment.End).padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(message.time),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray,
                        fontSize = 10.sp
                    )
                    if (isPending) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(10.dp),
                            tint = TextGray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageInput(
    onSendText: (String) -> Unit,
    onSendImage: (ByteArray) -> Unit,
    isOnline: Boolean = true
) {
    var text by remember { mutableStateOf("") }
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { s -> onSendImage(s.readBytes()) }
        }
    }
    
    Column {
        HorizontalDivider(thickness = 0.5.dp, color = AppleGray)
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth()
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { launcher.launch("image/*") }, 
                modifier = Modifier.size(36.dp),
                enabled = isOnline
            ) {
                Icon(
                    Icons.Default.Add, 
                    contentDescription = null, 
                    tint = if (isOnline) AppleBlue else Color.Gray
                )
            }
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f).heightIn(min = 36.dp),
                placeholder = { Text(stringResource(R.string.message_hint), fontSize = 16.sp) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = AppleGray,
                    unfocusedContainerColor = AppleGray,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(20.dp),
                maxLines = 4
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { if (text.isNotBlank()) { onSendText(text); text = "" } },
                modifier = Modifier
                    .size(36.dp)
                    .background(if (text.isNotBlank()) AppleBlue else Color.Transparent, RoundedCornerShape(18.dp)),
                enabled = text.isNotBlank()
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    tint = if (text.isNotBlank()) White else TextGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    return try { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp)) } catch(e: Exception) { "" }
}
