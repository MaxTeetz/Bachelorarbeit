package com.example.foldAR.data.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.foldAR.data.entities.DataSetFragmentMotionEvent

@Dao
interface MotionEventsFragmentDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMotionEventFragmentData(motionEvent: List<DataSetFragmentMotionEvent>)

    @Query("DELETE FROM DataSetFragmentMotionEvent WHERE TestCaseID = :testCaseId")
    suspend fun deleteMotionEventsFragmentDataForTestCase(testCaseId: Int)
}