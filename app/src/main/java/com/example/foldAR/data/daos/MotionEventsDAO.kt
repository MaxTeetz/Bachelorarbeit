package com.example.foldAR.data.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.foldAR.data.entities.DataMotionEvents

@Dao
interface MotionEventsDAO {

    @Insert
    suspend fun insertMotionEventData(motionEvent: DataMotionEvents)

    @Query("DELETE FROM DataMotionEvents WHERE TestCaseID = :testCaseId")
    suspend fun deleteMotionEventsDataForTestCase(testCaseId: Int)
}