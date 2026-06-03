package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkerDao {
    @Query("SELECT * FROM workers WHERE isActive = 1 ORDER BY id ASC")
    fun getAllWorkers(): Flow<List<Worker>>

    @Query("SELECT * FROM workers WHERE id = :id")
    suspend fun getWorkerById(id: Int): Worker?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorker(worker: Worker): Long

    @Update
    suspend fun updateWorker(worker: Worker)

    @Delete
    suspend fun deleteWorker(worker: Worker)
}

@Dao
interface PunchLogDao {
    @Query("SELECT * FROM punch_logs ORDER BY date DESC, id DESC")
    fun getAllLogs(): Flow<List<PunchLog>>

    @Query("SELECT * FROM punch_logs WHERE workerId = :workerId ORDER BY date DESC")
    fun getLogsByWorker(workerId: Int): Flow<List<PunchLog>>

    @Query("SELECT * FROM punch_logs WHERE workerId = :workerId AND date = :date LIMIT 1")
    suspend fun getLogForDate(workerId: Int, date: String): PunchLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: PunchLog): Long

    @Update
    suspend fun updateLog(log: PunchLog)

    @Delete
    suspend fun deleteLog(log: PunchLog)

    @Query("SELECT * FROM punch_logs WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getLogsBetweenDates(startDate: String, endDate: String): Flow<List<PunchLog>>

    @Query("SELECT * FROM punch_logs WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    suspend fun getLogsBetweenDatesDirect(startDate: String, endDate: String): List<PunchLog>
}

@Dao
interface SettingDao {
    @Query("SELECT * FROM app_settings WHERE `key` = :key")
    suspend fun getSetting(key: String): AppSetting?

    @Query("SELECT * FROM app_settings WHERE `key` = :key")
    fun getSettingFlow(key: String): Flow<AppSetting?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: AppSetting)
}

@Dao
interface PunchEventDao {
    @Query("SELECT * FROM punch_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<PunchEvent>>

    @Query("SELECT * FROM punch_events WHERE workerId = :workerId ORDER BY timestamp DESC")
    fun getEventsByWorker(workerId: Int): Flow<List<PunchEvent>>

    @Query("SELECT * FROM punch_events WHERE workerId = :workerId AND date = :date ORDER BY timestamp ASC")
    suspend fun getEventsForWorkerAndDate(workerId: Int, date: String): List<PunchEvent>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: PunchEvent): Long

    @Query("DELETE FROM punch_events")
    suspend fun deleteAllEvents()

    @Delete
    suspend fun deleteEvent(event: PunchEvent)
}
