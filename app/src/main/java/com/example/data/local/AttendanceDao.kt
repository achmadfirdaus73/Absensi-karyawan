package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceType
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records ORDER BY timestamp DESC")
    fun getAllAttendanceRecords(): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE formattedDate = :date ORDER BY timestamp DESC")
    fun getAttendanceForDate(date: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentAttendance(limit: Int = 10): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE employeeId = :empId AND formattedDate = :date AND type = :type LIMIT 1")
    suspend fun getTodayRecord(empId: Long, date: String, type: AttendanceType): AttendanceRecord?

    @Query("SELECT * FROM attendance_records WHERE isSyncedToCloud = 0")
    suspend fun getUnsyncedRecords(): List<AttendanceRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(record: AttendanceRecord): Long

    @Update
    suspend fun updateAttendance(record: AttendanceRecord)

    @Query("UPDATE attendance_records SET isSyncedToCloud = 1, syncTimestamp = :syncTime WHERE id = :id")
    suspend fun markSynced(id: Long, syncTime: Long)

    @Query("SELECT COUNT(*) FROM attendance_records WHERE formattedDate = :date AND type = 'CHECK_IN'")
    fun getTodayCheckInCount(date: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM attendance_records WHERE formattedDate = :date AND type = 'CHECK_OUT'")
    fun getTodayCheckOutCount(date: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM attendance_records WHERE formattedDate = :date AND punctualityStatus = 'LATE'")
    fun getTodayLateCount(date: String): Flow<Int>

    @Query("DELETE FROM attendance_records")
    suspend fun clearAllRecords()
}
