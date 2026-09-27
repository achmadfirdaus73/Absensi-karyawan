package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Employee
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeDao {
    @Query("SELECT * FROM employees WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveEmployees(): Flow<List<Employee>>

    @Query("SELECT * FROM employees ORDER BY id DESC")
    fun getAllEmployees(): Flow<List<Employee>>

    @Query("SELECT * FROM employees WHERE id = :id LIMIT 1")
    suspend fun getEmployeeById(id: Long): Employee?

    @Query("SELECT * FROM employees WHERE employeeCode = :code LIMIT 1")
    suspend fun getEmployeeByCode(code: String): Employee?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: Employee): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(employees: List<Employee>)

    @Update
    suspend fun updateEmployee(employee: Employee)

    @Delete
    suspend fun deleteEmployee(employee: Employee)

    @Query("DELETE FROM employees WHERE id = :id")
    suspend fun deleteEmployeeById(id: Long)

    @Query("SELECT COUNT(*) FROM employees WHERE isActive = 1")
    fun getActiveEmployeeCount(): Flow<Int>
}
