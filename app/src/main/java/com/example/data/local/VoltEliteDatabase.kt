package com.example.data.local

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey val id: String,
    val make: String,
    val model: String,
    val batteryCapacityKwh: Double,
    val realWorldRangeKm: Int,
    val connectorType: String,
    val currentBatteryPercent: Int,
    val isDefault: Boolean
)

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey val id: String,
    val origin: String,
    val destination: String,
    val distanceKm: Int,
    val durationFormatted: String,
    val costRupees: Int,
    val dateFormatted: String,
    val status: String,
    val stopsCount: Int
)

@Entity(tableName = "saved_locations")
data class SavedLocationEntity(
    @PrimaryKey val id: String,
    val label: String,
    val address: String,
    val iconName: String
)

@Entity(tableName = "charging_sessions")
data class ChargingSessionEntity(
    @PrimaryKey val id: String,
    val stationId: String,
    val stationName: String,
    val networkName: String,
    val connectorType: String,
    val powerKw: Int,
    val energyAddedKwh: Double,
    val totalCostRupees: Double,
    val status: String,
    val evseId: String = "EVSE-08",
    val txnId: String = "TXN-839201",
    val paymentMethod: String = "UPI (Google Pay)",
    val durationMin: Int = 32,
    val tariffPerKwh: Double = 18.5,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val message: String,
    val timeAgo: String,
    val isRead: Boolean,
    val type: String
)

@Entity(tableName = "station_bookmarks")
data class StationBookmarkEntity(
    @PrimaryKey val stationId: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles ORDER BY isDefault DESC")
    fun getAllVehicles(): Flow<List<VehicleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: VehicleEntity)

    @Query("UPDATE vehicles SET isDefault = 0")
    suspend fun clearDefault()

    @Query("UPDATE vehicles SET isDefault = 1 WHERE id = :id")
    suspend fun setDefault(id: String)

    @Query("UPDATE vehicles SET currentBatteryPercent = :percent WHERE id = :id")
    suspend fun updateBattery(id: String, percent: Int)
}

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY id DESC")
    fun getAllTrips(): Flow<List<TripEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripEntity)
}

@Dao
interface SavedLocationDao {
    @Query("SELECT * FROM saved_locations")
    fun getAllSavedLocations(): Flow<List<SavedLocationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(loc: SavedLocationEntity)

    @Query("DELETE FROM saved_locations WHERE id = :id")
    suspend fun deleteLocation(id: String)
}

@Dao
interface ChargingSessionDao {
    @Query("SELECT * FROM charging_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<ChargingSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChargingSessionEntity)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)
}

@Dao
interface StationBookmarkDao {
    @Query("SELECT stationId FROM station_bookmarks")
    fun getBookmarkedStationIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addBookmark(bookmark: StationBookmarkEntity)

    @Query("DELETE FROM station_bookmarks WHERE stationId = :stationId")
    suspend fun removeBookmark(stationId: String)
}

@Database(
    entities = [
        VehicleEntity::class,
        TripEntity::class,
        SavedLocationEntity::class,
        ChargingSessionEntity::class,
        NotificationEntity::class,
        StationBookmarkEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class VoltEliteDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun tripDao(): TripDao
    abstract fun savedLocationDao(): SavedLocationDao
    abstract fun chargingSessionDao(): ChargingSessionDao
    abstract fun notificationDao(): NotificationDao
    abstract fun stationBookmarkDao(): StationBookmarkDao

    companion object {
        @Volatile
        private var INSTANCE: VoltEliteDatabase? = null

        fun getInstance(context: Context): VoltEliteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VoltEliteDatabase::class.java,
                    "voltelite_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
