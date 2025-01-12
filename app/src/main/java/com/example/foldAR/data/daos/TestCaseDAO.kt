package com.example.foldAR.data.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.foldAR.data.entities.TestCase

@Dao
interface TestCaseDAO {
    @Insert
    suspend fun insertTestCase(testCase: TestCase)

    @Query("SELECT * FROM TestCase WHERE ScenarioID = :scenarioId ORDER BY TestCaseID DESC")
    suspend fun getTestCaseByScenarioId(scenarioId: Int): List<TestCase>


    //check if the last testCase of the scenario was finished in case the app crashes -> used for deleting DataSets
    @Query("SELECT * FROM TestCase WHERE ScenarioID = :scenarioId ORDER BY TestCaseID DESC LIMIT 1")
    suspend fun getLastTestCaseOfScenario(scenarioId: Int): TestCase?

    //check if the last testCase was finished in case the app crashes -> used for deleting DataSets
    @Query("SELECT * FROM TestCase ORDER BY TestCaseID DESC LIMIT 1")
    suspend fun getLastTestCase(): TestCase

    @Query("UPDATE TESTCASE SET EndTime = :endTime WHERE TestCaseID = :testCaseId")
    suspend fun updateTestCase(endTime: String, testCaseId: Int)

}