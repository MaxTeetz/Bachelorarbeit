package com.example.foldAR.data.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.foldAR.data.entities.DataSet

@Dao
interface DataSetsDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDataSets(dataSet: List<DataSet>)

    @Query("DELETE FROM DataSet WHERE TestCaseID = :testCaseId")
    suspend fun deleteDataSetsForTestCase(testCaseId: Int)
}