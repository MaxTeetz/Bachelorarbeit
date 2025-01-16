package com.example.foldAR.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.foldAR.data.daos.DataSetsDAO
import com.example.foldAR.data.daos.ScenariosDAO
import com.example.foldAR.data.daos.TestCaseDAO
import com.example.foldAR.data.daos.UsersDAO
import com.example.foldAR.data.entities.DataSet
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
    private val dataSetsDAO: DataSetsDAO
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

    fun insertDataSet(dataSet: DataSet) {
        viewModelScope.launch {
            dataSetsDAO.insertDataSet(dataSet)
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

    suspend fun getLastScenarioByUserId(userId: Int): Scenario? {
        return withContext(Dispatchers.IO) {
            scenariosDAO.getLastScenarioOfUser(userId)
        }
    }

    suspend fun updateUser(userId: Int) {
        usersDAO.updateUser(userId)
    }

    suspend fun updateEndTime(currentTime: String, testCaseId: Int) {
        testCaseDAO.updateEndTime(currentTime, testCaseId)
    }

    suspend fun updateStartTime(currentTime: String, testCaseId: Int){
        testCaseDAO.updateStartTime(currentTime, testCaseId)
    }
}

class DatabaseViewModelFactory(
    private val usersDAO: UsersDAO,
    private val scenariosDAO: ScenariosDAO,
    private val testCaseDAO: TestCaseDAO,
    private val dataSetsDAO: DataSetsDAO
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DatabaseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DatabaseViewModel(usersDAO, scenariosDAO, testCaseDAO, dataSetsDAO) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}