package com.example.foldAR.data.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.foldAR.data.entities.Scenario

@Dao
interface ScenariosDAO {
    @Insert
    suspend fun insertScenario(scenario: Scenario)

    @Query("SELECT * FROM Scenario WHERE UserID = :userId ORDER BY ScenarioID DESC LIMIT 1")
    suspend fun getLastScenarioOfUser(userId: Int): Scenario?

    @Query("UPDATE SCENARIO SET TestCaseNumber = :testCaseNumber WHERE ScenarioID = :scenarioID")
    suspend fun updateTestCaseNumber(testCaseNumber: Int, scenarioID: Int)
}