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
    entities = [
        Employee::class,
        AttendanceRecord::class,
        AdminNotification::class,
        KioskSettings::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun employeeDao(): EmployeeDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun adminNotificationDao(): AdminNotificationDao
    abstract fun kioskSettingsDao(): KioskSettingsDao

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
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database)
                    }
                }
            }
        }

        private suspend fun populateInitialData(database: AppDatabase) {
            // Seed Settings
            val defaultSettings = KioskSettings()
            database.kioskSettingsDao().insertOrUpdate(defaultSettings)

            // Seed Employees
            val employees = listOf(
                Employee(
                    employeeCode = "EMP-0821",
                    name = "Budi Santoso",
                    department = "IT & Engineering",
                    role = "Senior Lead Engineer",
                    email = "budi.santoso@digitalnusantara.id",
                    phone = "0812-3456-7890",
                    shiftStart = "08:00",
                    shiftEnd = "17:00",
                    photoUri = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300",
                    faceFeaturesJson = """{"descriptor": [0.24, 0.51, 0.88, 0.12, 0.65], "tag": "budi_santoso"}"""
                ),
                Employee(
                    employeeCode = "EMP-0914",
                    name = "Siti Rahmawati",
                    department = "Finance & Accounting",
                    role = "Finance Manager",
                    email = "siti.rahmawati@digitalnusantara.id",
                    phone = "0813-9876-5432",
                    shiftStart = "08:30",
                    shiftEnd = "17:30",
                    photoUri = "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=300",
                    faceFeaturesJson = """{"descriptor": [0.18, 0.44, 0.92, 0.31, 0.58], "tag": "siti_rahmawati"}"""
                ),
                Employee(
                    employeeCode = "EMP-1042",
                    name = "Ahmad Fauzi",
                    department = "Operations & Logistics",
                    role = "Operations Supervisor",
                    email = "ahmad.fauzi@digitalnusantara.id",
                    phone = "0857-1122-3344",
                    shiftStart = "08:00",
                    shiftEnd = "17:00",
                    photoUri = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300",
                    faceFeaturesJson = """{"descriptor": [0.35, 0.62, 0.77, 0.25, 0.71], "tag": "ahmad_fauzi"}"""
                ),
                Employee(
                    employeeCode = "EMP-1108",
                    name = "Dewi Lestari",
                    department = "Product & Design",
                    role = "UI/UX Lead Designer",
                    email = "dewi.lestari@digitalnusantara.id",
                    phone = "0819-5566-7788",
                    shiftStart = "09:00",
                    shiftEnd = "18:00",
                    photoUri = "https://images.unsplash.com/photo-1580489944761-15a19d654956?w=300",
                    faceFeaturesJson = """{"descriptor": [0.29, 0.48, 0.83, 0.40, 0.63], "tag": "dewi_lestari"}"""
                ),
                Employee(
                    employeeCode = "EMP-1250",
                    name = "Rizky Pratama",
                    department = "Human Capital & HR",
                    role = "People Operations Specialist",
                    email = "rizky.pratama@digitalnusantara.id",
                    phone = "0821-8899-0011",
                    shiftStart = "08:00",
                    shiftEnd = "17:00",
                    photoUri = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=300",
                    faceFeaturesJson = """{"descriptor": [0.15, 0.58, 0.81, 0.19, 0.69], "tag": "rizky_pratama"}"""
                ),
                Employee(
                    employeeCode = "EMP-1388",
                    name = "Maya Anggraini",
                    department = "Marketing & Growth",
                    role = "Brand Marketing Executive",
                    email = "maya.anggraini@digitalnusantara.id",
                    phone = "0811-2233-4455",
                    shiftStart = "08:30",
                    shiftEnd = "17:30",
                    photoUri = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=300",
                    faceFeaturesJson = """{"descriptor": [0.22, 0.49, 0.89, 0.28, 0.61], "tag": "maya_anggraini"}"""
                )
            )
            database.employeeDao().insertAll(employees)
        }
    }
}
