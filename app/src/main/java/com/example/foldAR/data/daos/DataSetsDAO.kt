package com.example.foldAR.data.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.foldAR.data.entities.DataSet

@Dao
interface DataSetsDAO {
    @Insert
    suspend fun insertDataSet(dataSet: DataSet)

    @Delete
    suspend fun deleteDataSets(dataSet: DataSet)

    @Query("DELETE FROM DataSet WHERE TestCaseID = :testCaseId")
    suspend fun deleteDataSetsForTestCase(testCaseId: Int)
}