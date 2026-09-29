package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AdminNotification
import com.example.data.model.AttendanceRecord
import com.example.data.model.Employee
import com.example.data.model.KioskSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Employee::class, AttendanceRecord::class, KioskSettings::class, AdminNotification::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun employeeDao(): EmployeeDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_presensi_kiosk.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                val database = getDatabase(context)
                                database.attendanceDao().saveKioskSettings(KioskSettings())
                                // Seed default demo employees
                                database.employeeDao().insertEmployee(
                                    Employee(
                                        name = "Budi Santoso",
                                        nik = "EMP-001",
                                        division = "Teknologi Informasi",
                                        role = "Manager",
                                        shiftType = "MANUAL_CUSTOM",
                                        customStartTime = "09:00",
                                        customEndTime = "18:00",
                                        pin = "1111"
                                    )
                                )
                                database.employeeDao().insertEmployee(
                                    Employee(
                                        name = "Siti Rahmawati",
                                        nik = "EMP-002",
                                        division = "Operasional",
                                        role = "Staff",
                                        shiftType = "AUTO_DETECT",
                                        pin = "2222"
                                    )
                                )
                                database.employeeDao().insertEmployee(
                                    Employee(
                                        name = "Ahmad Hidayat",
                                        nik = "EMP-003",
                                        division = "Keamanan",
                                        role = "Security",
                                        shiftType = "SHIFT_3",
                                        pin = "3333"
                                    )
                                )
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
