package com.example.foldAR.data.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.foldAR.data.entities.User

@Dao
interface UsersDAO {
    @Insert
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM User WHERE UserID = :id")
    suspend fun getUserById(id: Int): User?

    @Query("SELECT * FROM User ORDER BY UserID DESC LIMIT 1")
    suspend fun getLastUser() : User?
}