package com.example.foldAR.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.foldAR.data.daos.DataSetsDAO
import com.example.foldAR.data.daos.ScenariosDAO
import com.example.foldAR.data.daos.TestCaseDAO
import com.example.foldAR.data.daos.UsersDAO
import com.example.foldAR.data.entities.DataSet
import com.example.foldAR.data.entities.Scenario
import com.example.foldAR.data.entities.TestCase
import com.example.foldAR.data.entities.Users
import kotlinx.coroutines.launch

class DatabaseViewModel(
    private val usersDAO: UsersDAO,
    private val scenariosDAO: ScenariosDAO,
    private val testCaseDAO: TestCaseDAO,
    private val dataSetsDAO: DataSetsDAO
) : ViewModel() {

    fun insertUser(user: Users) {
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

    fun getLastTestCase(): LiveData<TestCase> {
        return testCaseDAO.getLastTestCase().asLiveData()
    }

    fun deleteDataSet(testCaseId: Int) {
        viewModelScope.launch {
            dataSetsDAO.deleteDataSetsForTestCase(testCaseId)
        }
    }
}

class DatabaseViewModelFactory(
    private val usersDAO: UsersDAO,
    private val scenariosDAO: ScenariosDAO,
    private val testCaseDAO: TestCaseDAO,
    private val dataSetsDAO: DataSetsDAO) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(DatabaseViewModel::class.java)){
            @Suppress("UNCHECKED_CAST")
            return DatabaseViewModel(usersDAO, scenariosDAO, testCaseDAO, dataSetsDAO) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
    }