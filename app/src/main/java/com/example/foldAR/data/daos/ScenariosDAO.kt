package com.example.foldAR.data.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.foldAR.data.entities.Scenario

@Dao
interface ScenariosDAO {
    @Insert
    suspend fun insertScenario(scenario: Scenario)

    @Query("SELECT * FROM Scenario WHERE UserID = :userId")
    suspend fun getScenariosByUserId(userId: Int): List<Scenario>
}