package com.example.foldAR.data.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.foldAR.data.entities.TestCase

@Dao
interface TestCaseDAO {
    @Insert
    suspend fun insertTestCase(testCase: TestCase)

    //check if the last testCase of the scenario was finished in case the app crashes -> used for deleting DataSets
    @Query("SELECT * FROM TestCase WHERE ScenarioID = :scenarioId ORDER BY TestCaseID DESC LIMIT 1")
    suspend fun getLastTestCaseOfScenario(scenarioId: Int): TestCase?

    @Query("UPDATE TESTCASE SET EndTime = :endTime WHERE TestCaseID = :testCaseId")
    suspend fun updateEndTime(endTime: Long, testCaseId: Int)

    @Query("UPDATE TESTCASE SET StartTime = :startTime WHERE TestCaseID = :testCaseId")
    suspend fun updateStartTime(startTime: Long, testCaseId: Int)

    @Query("UPDATE TESTCASE SET Distance = :distance WHERE TestCaseID = :testCaseId")
    suspend fun updateDistance(distance: Float, testCaseId: Int)

    @Query("UPDATE TESTCASE SET TimeReached = :timeReached WHERE TestCaseID = :testCaseId")
    suspend fun updateTimeReached(timeReached: Long, testCaseId: Int)

    @Query("UPDATE TESTCASE SET DistanceReached = :distanceReached WHERE TestCaseID = :testCaseId")
    suspend fun updateDistanceReached(distanceReached: Float, testCaseId: Int)
}