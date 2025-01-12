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

    fun insertUser(user: User) {
        viewModelScope.launch {
            usersDAO.insertUser(user)
        }
    }

    fun insertScenario(scenario: Scenario) {
        viewModelScope.launch {
            scenariosDAO.insertScenario(scenario)
        }
    }

    fun insertTestCase(testCase: TestCase) {
        viewModelScope.launch {
            testCaseDAO.insertTestCase(testCase)
        }
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


    //if testCase done just start dialog after setting testCase and scenario number else
    suspend fun scenarioGetTestCases(id: Int): List<TestCase> {
        return withContext(Dispatchers.IO) {
            testCaseDAO.getTestCaseByScenarioId(id)
        }
    }

    //if app crashes and testCase of scenario is not finished, delete the data for it to fill it again
    suspend fun getLastTestCaseOfScenario(id: Int): TestCase {
        return withContext(Dispatchers.IO) {
            val testCase = testCaseDAO.getLastTestCaseOfScenario(id)
            if (testCase.EndTime == null)
                deleteDataSet(testCase.TestCaseID)
            testCase
        }
    }

    //if app crashes and testCase is not finished, delete the data for it to fill it again
    suspend fun getLastTestCase(): Boolean {
        return withContext(Dispatchers.IO) {
            val testCase = testCaseDAO.getLastTestCase()
            if (testCase.EndTime == null) {
                deleteDataSet(testCase.TestCaseID)
                true
            } else
                false
        }
    }

    private fun deleteDataSet(testCaseId: Int) {
        viewModelScope.launch {
            dataSetsDAO.deleteDataSetsForTestCase(testCaseId)
        }
    }

    suspend fun getLastScenarioByUserId(userId: Int): Scenario? {
        return withContext(Dispatchers.IO) {
            scenariosDAO.getLastScenarioOfUser(userId)
        }
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