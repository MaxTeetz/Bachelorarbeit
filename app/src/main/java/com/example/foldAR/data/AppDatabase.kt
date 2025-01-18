package com.example.foldAR.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.foldAR.data.daos.DataSetsDAO
import com.example.foldAR.data.daos.ScenariosDAO
import com.example.foldAR.data.daos.TestCaseDAO
import com.example.foldAR.data.daos.UsersDAO
import com.example.foldAR.data.entities.DataSet
import com.example.foldAR.data.entities.Scenario
import com.example.foldAR.data.entities.TestCase
import com.example.foldAR.data.entities.User

@Database(entities = [User::class, Scenario::class, TestCase::class, DataSet::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase(){
    abstract fun usersDao(): UsersDAO
    abstract fun scenariosDao(): ScenariosDAO
    abstract fun testCasesDao(): TestCaseDAO
    abstract fun dataSetsDao(): DataSetsDAO

    companion object{
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
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }
}