package com.example.foldAR.data.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.foldAR.data.entities.Users

@Dao
interface UsersDAO {
    @Insert
    suspend fun insertUser(user: Users)

    @Query("SELECT * FROM Users WHERE UserID = :id")
    suspend fun getUserById(id: Int): Users?
}