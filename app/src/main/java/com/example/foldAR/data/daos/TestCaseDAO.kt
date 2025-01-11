package com.example.foldAR.data.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.foldAR.data.entities.TestCase
import kotlinx.coroutines.flow.Flow

@Dao
interface TestCaseDAO {
    @Insert
    suspend fun insertTestCase(testCase: TestCase)

    @Query("SELECT * FROM TestCase WHERE ScenarioID = :scenarioId")
    suspend fun getTestCaseByScenarioId(scenarioId: Int): List<TestCase>


    //check if the last TestCase was finished in case the app crashes -> used for deleting DataSets
    @Query("SELECT * FROM TestCase ORDER BY TestCaseID DESC LIMIT 1")
    fun getLastTestCase(): Flow<TestCase>
}