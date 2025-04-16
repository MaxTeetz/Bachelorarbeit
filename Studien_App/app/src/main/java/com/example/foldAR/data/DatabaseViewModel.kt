package com.example.foldAR.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DatabaseViewModel(
    private val usersDAO: UsersDAO,
    private val scenariosDAO: ScenariosDAO,
    private val testCaseDAO: TestCaseDAO,
    private val dataSetsDAO: DataSetsDAO,
    private val motionEventsFragmentDAO: MotionEventsFragmentDAO,
    private val motionEventsGlSurfaceDAO: MotionEventsGlSurfaceDAO
) : ViewModel() {

    suspend fun insertUser(user: User) {
        usersDAO.insertUser(user)
    }

    suspend fun insertScenario(scenario: Scenario) {
        scenariosDAO.insertScenario(scenario)

    }

    suspend fun insertTestCase(testCase: TestCase) {
        testCaseDAO.insertTestCase(testCase)
    }

    fun insertDataSets(list: List<DataSet>) {
        viewModelScope.launch(Dispatchers.IO) {
            dataSetsDAO.insertDataSets(list)
        }
    }

    fun insertMotionEventFragmentData(motionEventData: List<DataSetFragmentMotionEvent>) {
        viewModelScope.launch(Dispatchers.IO) {
            motionEventsFragmentDAO.insertMotionEventFragmentData(motionEventData)
        }
    }

    fun insertMotionEventGlSurfaceData(motionEventData: List<DataSetGlSurfaceViewMotionEvent>) {
        viewModelScope.launch(Dispatchers.IO) {
            motionEventsGlSurfaceDAO.insertMotionEventGlSurfaceData(motionEventData)
        }
    }

    suspend fun getLastUser(): User? {
        return withContext(Dispatchers.IO) {
            usersDAO.getLastUser()
        }
    }

    //if app crashes and testCase of scenario is not finished, delete the data for it to fill it again
    suspend fun getLastTestCaseOfScenario(id: Int): TestCase? {
        return withContext(Dispatchers.IO) {
            testCaseDAO.getLastTestCaseOfScenario(id)
        }
    }

    fun deleteDataSet(testCaseId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            dataSetsDAO.deleteDataSetsForTestCase(testCaseId)
        }
    }

    fun deleteFragmentDataSet(testCaseId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            motionEventsFragmentDAO.deleteMotionEventsFragmentDataForTestCase(testCaseId)
        }
    }

    fun deleteGlSurfaceDataSet(testCaseId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            motionEventsGlSurfaceDAO.deleteMotionEventsGlSurfaceDataForTestCase(testCaseId)
        }
    }

    suspend fun getLastScenarioByUserId(userId: Int): Scenario? {
        return withContext(Dispatchers.IO) {
            scenariosDAO.getLastScenarioOfUser(userId)
        }
    }

    suspend fun updateUser(userId: Int) {
        usersDAO.updateUser(userId)
    }

    suspend fun updateEndTime(currentTime: Long, testCaseId: Int) {
        testCaseDAO.updateEndTime(currentTime, testCaseId)
    }

    suspend fun updateStartTime(currentTime: Long, testCaseId: Int) {
        testCaseDAO.updateStartTime(currentTime, testCaseId)
    }

    suspend fun updateDistance(distance: Float, testCaseId: Int) {
        testCaseDAO.updateDistance(distance, testCaseId)
    }

    suspend fun updateTimeReached(time: Long, testCaseId: Int) {
        testCaseDAO.updateTimeReached(time, testCaseId)
    }

    suspend fun updateDistanceReached(distance: Float, testCaseId: Int) {
        testCaseDAO.updateDistanceReached(distance, testCaseId)
    }
}

class DatabaseViewModelFactory(
    private val usersDAO: UsersDAO,
    private val scenariosDAO: ScenariosDAO,
    private val testCaseDAO: TestCaseDAO,
    private val dataSetsDAO: DataSetsDAO,
    private val motionEventsFragmentDAO: MotionEventsFragmentDAO,
    private val motionEventsGlSurfaceDAO: MotionEventsGlSurfaceDAO
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DatabaseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DatabaseViewModel(
                usersDAO,
                scenariosDAO,
                testCaseDAO,
                dataSetsDAO,
                motionEventsFragmentDAO,
                motionEventsGlSurfaceDAO
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}