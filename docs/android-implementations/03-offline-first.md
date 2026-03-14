# Android Driver App — Offline-First Strategy

> **Goal:** Never lose a sensor ping even in areas with no connectivity. Store locally in Room DB, drain to backend when connectivity is restored.

---

## Room DB Setup (SQLCipher encrypted)

```kotlin
// app/src/main/kotlin/com/solodev/fleet/driver/local/SensorPingEntity.kt
@Entity(tableName = "sensor_pings")
data class SensorPingEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val vehicleId:    String,
    val latitude:     Double,
    val longitude:    Double,
    val accuracy:     Float,
    val speed:        Float,
    val heading:      Float,
    val accelX:       Float?,
    val accelY:       Float?,
    val accelZ:       Float?,
    val gyroX:        Float?,
    val gyroY:        Float?,
    val gyroZ:        Float?,
    val batteryLevel: Int?,
    val timestamp:    String,           // ISO-8601
    val routeId:      String?,
    val isSynced:     Boolean = false,
    val createdAt:    Long = System.currentTimeMillis(),
)
```

```kotlin
// app/src/main/kotlin/com/solodev/fleet/driver/local/SensorPingDao.kt
@Dao
interface SensorPingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pings: List<SensorPingEntity>)

    @Query("SELECT * FROM sensor_pings WHERE isSynced = 0 ORDER BY createdAt ASC LIMIT 100")
    suspend fun getPendingPings(): List<SensorPingEntity>

    @Query("UPDATE sensor_pings SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    // Enforce max 1000 pings — evict oldest when over limit
    @Query("""
        DELETE FROM sensor_pings
        WHERE id IN (
            SELECT id FROM sensor_pings ORDER BY createdAt ASC
            LIMIT MAX(0, (SELECT COUNT(*) FROM sensor_pings) - 1000)
        )
    """)
    suspend fun evictOverflow()

    // 7-day TTL cleanup
    @Query("DELETE FROM sensor_pings WHERE createdAt < :cutoffMs AND isSynced = 1")
    suspend fun deleteExpired(cutoffMs: Long)
}
```

```kotlin
// app/src/main/kotlin/com/solodev/fleet/driver/local/FleetLocalDatabase.kt
@Database(entities = [SensorPingEntity::class], version = 1)
abstract class FleetLocalDatabase : RoomDatabase() {
    abstract fun sensorPingDao(): SensorPingDao

    companion object {
        fun create(context: Context): FleetLocalDatabase {
            val passphrase = SQLiteDatabase.getBytes("fleet-secure-key".toCharArray())
            return Room.databaseBuilder(context, FleetLocalDatabase::class.java, "fleet.db")
                .openHelperFactory(SupportFactory(passphrase))  // SQLCipher
                .build()
        }
    }
}
```

---

## WorkManager — Background Sync

```kotlin
// app/src/main/kotlin/com/solodev/fleet/driver/worker/CoordinateRetryWorker.kt
@HiltWorker
class CoordinateRetryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val dao: SensorPingDao,
    private val apiClient: FleetApiClient,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val pending = dao.getPendingPings()
        if (pending.isEmpty()) return Result.success()

        return try {
            val pings = pending.map { it.toSensorPing() }
            val response = apiClient.sendCoordinates(pings)

            when (response.status.value) {
                202  -> {
                    dao.markSynced(pending.map { it.id })
                    Result.success()
                }
                503  -> Result.retry()   // reception disabled — retry later
                429  -> Result.retry()   // rate limited
                else -> Result.retry()
            }
        } catch (e: Exception) {
            if (runAttemptCount < 4) Result.retry() else Result.failure()
        }
    }
}
```

### WorkManager Scheduling

```kotlin
// Exponential backoff: 1 min → 5 min → 15 min → 60 min
fun schedulePeriodicSync(context: Context) {
    val syncWork = PeriodicWorkRequestBuilder<CoordinateRetryWorker>(15, TimeUnit.MINUTES)
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "coordinate_sync",
        ExistingPeriodicWorkPolicy.KEEP,
        syncWork,
    )
}
```

---

## NetworkMonitor — Drain on Connectivity Restore

```kotlin
// app/src/main/kotlin/com/solodev/fleet/driver/network/NetworkMonitor.kt
@Singleton
class NetworkMonitor @Inject constructor(
    private val context: Context,
    private val coordinateRepository: CoordinateRepository,
) {
    private val connectivityManager =
        context.getSystemService(ConnectivityManager::class.java)

    fun startObserving(scope: CoroutineScope) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                scope.launch {
                    coordinateRepository.drainLocalDatabase()
                }
            }
        }
        connectivityManager.registerDefaultNetworkCallback(callback)
    }
}
```

---

## StorageManager — Capacity Limits

```kotlin
// app/src/main/kotlin/com/solodev/fleet/driver/local/StorageManager.kt
class StorageManager @Inject constructor(private val dao: SensorPingDao) {

    /** Call periodically (e.g., every hour) to enforce limits. */
    suspend fun enforce() {
        dao.evictOverflow()                                        // max 1000 pings
        dao.deleteExpired(System.currentTimeMillis() - SEVEN_DAYS) // 7d TTL
    }

    companion object {
        private const val SEVEN_DAYS = 7L * 24 * 60 * 60 * 1000
    }
}
```

---

## Implementation Checklist

- [ ] Add Room + SQLCipher dependencies to `app/build.gradle.kts`
- [ ] Create `SensorPingEntity`, `SensorPingDao`, `FleetLocalDatabase`
- [ ] Implement `CoordinateRetryWorker` with exponential backoff
- [ ] Register `schedulePeriodicSync()` in `onStartCommand` of `SensorTrackingService`
- [ ] Implement `NetworkMonitor` and start observing in `Application.onCreate()`
- [ ] Schedule `StorageManager.enforce()` via hourly `PeriodicWorkRequest`

### Dependencies

```kotlin
// app/build.gradle.kts
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")
implementation("net.zetetic:sqlcipher-android:4.5.4")
implementation("androidx.sqlite:sqlite-ktx:2.4.0")
implementation("androidx.work:work-runtime-ktx:2.9.0")
implementation("androidx.hilt:hilt-work:1.2.0")
ksp("androidx.hilt:hilt-compiler:1.2.0")
```
