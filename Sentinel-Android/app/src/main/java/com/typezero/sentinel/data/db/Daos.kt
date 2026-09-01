package com.typezero.sentinel.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface KnownDeviceDao {
    @Query("SELECT * FROM known_devices WHERE networkKey = :networkKey ORDER BY online DESC, displayName ASC")
    fun observeForNetwork(networkKey: String): Flow<List<KnownDevice>>

    @Query("SELECT * FROM known_devices WHERE networkKey = :networkKey")
    suspend fun forNetwork(networkKey: String): List<KnownDevice>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(device: KnownDevice): Long

    @Update
    suspend fun update(device: KnownDevice)

    @Query("UPDATE known_devices SET customName = :name WHERE id = :id")
    suspend fun setCustomName(id: Long, name: String?)

    @Query("UPDATE known_devices SET userType = :type WHERE id = :id")
    suspend fun setUserType(id: Long, type: String?)

    @Query("UPDATE known_devices SET room = :room WHERE id = :id")
    suspend fun setRoom(id: Long, room: String?)

    @Query("DELETE FROM known_devices WHERE networkKey = :networkKey")
    suspend fun clearNetwork(networkKey: String)
}

@Dao
interface WatchedTargetDao {
    @Query("SELECT * FROM watched_targets ORDER BY label ASC")
    fun observeAll(): Flow<List<WatchedTarget>>

    @Query("SELECT * FROM watched_targets")
    suspend fun all(): List<WatchedTarget>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(target: WatchedTarget): Long

    @Update
    suspend fun update(target: WatchedTarget)

    @Delete
    suspend fun delete(target: WatchedTarget)
}

@Dao
interface NetworkEventDao {
    @Query("SELECT * FROM network_events ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 200): Flow<List<NetworkEvent>>

    @Insert
    suspend fun insertAll(events: List<NetworkEvent>)

    @Query("DELETE FROM network_events")
    suspend fun clear()
}

@Dao
interface NetworkStateDao {
    @Query("SELECT * FROM network_state WHERE networkKey = :networkKey LIMIT 1")
    suspend fun get(networkKey: String): NetworkState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: NetworkState)
}
