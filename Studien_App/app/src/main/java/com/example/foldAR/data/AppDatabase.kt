package com.example.foldAR.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.foldAR.data.daos.DataSetsDAO
import com.example.foldAR.data.daos.MotionEventsFragmentDAO
import com.example.foldAR.data.daos.MotionEventsGlSurfaceDAO
import com.example.foldAR.data.daos.ScenariosDAO
import com.example.foldAR.data.daos.TestCaseDAO
import com.example.foldAR.data.daos.UsersDAO
import com.example.foldAR.data.entities.DataSet
import com.example.foldAR.data.entities.DataSetFragmentMotionEvent
import com.example.foldAR.data.entities.DataSetGlSurfaceViewMotionEvent
import com.example.foldAR.data.entities.Scenario
import com.example.foldAR.data.entities.TestCase
import com.example.foldAR.data.entities.User

@Database(
    entities = [User::class, Scenario::class, TestCase::class, DataSet::class, DataSetFragmentMotionEvent::class, DataSetGlSurfaceViewMotionEvent::class],
    version = 12,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usersDao(): UsersDAO
    abstract fun scenariosDao(): ScenariosDAO
    abstract fun testCasesDao(): TestCaseDAO
    abstract fun dataSetsDao(): DataSetsDAO
    abstract fun motionEventsFragmentDAO(): MotionEventsFragmentDAO
    abstract fun motionEventsGlSurfaceDAO(): MotionEventsGlSurfaceDAO

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .fallbackToDestructiveMigration()
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }
}