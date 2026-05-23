package lab3.egor.chat.data.storage.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM channels")
    fun getChannels(): Flow<List<ChannelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Query("SELECT * FROM messages WHERE [to] = :channelName ORDER BY time DESC")
    fun getMessages(channelName: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("DELETE FROM messages WHERE [to] = :channelName")
    suspend fun clearMessages(channelName: String)

    // Pending messages
    @Query("SELECT * FROM pending_messages ORDER BY time ASC")
    fun getPendingMessagesFlow(): Flow<List<PendingMessageEntity>>

    @Query("SELECT * FROM pending_messages ORDER BY time ASC")
    suspend fun getPendingMessages(): List<PendingMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingMessage(message: PendingMessageEntity)

    @Delete
    suspend fun deletePendingMessage(message: PendingMessageEntity)
}
