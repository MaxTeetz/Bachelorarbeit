package com.example.foldAR.data.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.foldAR.data.entities.DataSetGlSurfaceViewMotionEvent


@Dao
interface MotionEventsGlSurfaceDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMotionEventGlSurfaceData(motionEvent: List<DataSetGlSurfaceViewMotionEvent>)

    @Query("DELETE FROM DataSetGlSurfaceViewMotionEvent WHERE TestCaseID = :testCaseId")
    suspend fun deleteMotionEventsGlSurfaceDataForTestCase(testCaseId: Int)
}