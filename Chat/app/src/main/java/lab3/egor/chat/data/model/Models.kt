package lab3.egor.chat.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val name: String,
    val pwd: String
)

@Serializable
data class Message(
    val id: Long,        // На сервере это число
    val from: String,
    val to: String? = null,
    val data: MessageData,
    val time: Long       // На сервере это число (timestamp)
)

@Serializable
data class MessageData(
    val Text: TextContent? = null,
    val Image: ImageContent? = null
)

@Serializable
data class TextContent(
    val text: String
)

@Serializable
data class ImageContent(
    val link: String
)

@Serializable
data class SendMessageRequest(
    val from: String,
    val to: String,
    val data: MessageData
)
