package lab3.egor.chat.data.storage.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val name: String
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: Long,
    val from: String,
    val to: String,
    val text: String?,
    val imageLink: String?,
    val time: Long
)

@Entity(tableName = "pending_messages")
data class PendingMessageEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val channel: String,
    val from: String,
    val text: String,
    val time: Long
)
