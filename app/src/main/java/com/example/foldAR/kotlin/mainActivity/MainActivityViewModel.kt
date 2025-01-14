package com.example.foldAR.kotlin.mainActivity

import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foldAR.data.DatabaseViewModel
import com.example.foldAR.data.entities.DataSet
import com.example.foldAR.data.entities.Scenario
import com.example.foldAR.data.entities.TestCase
import com.example.foldAR.data.entities.User
import com.example.foldAR.kotlin.constants.Constants
import com.example.foldAR.kotlin.constants.ObjectCoords
import com.example.foldAR.kotlin.renderer.HelloArRenderer
import com.google.ar.core.Pose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin


/**The viewModel handles the renderer as well as the delegation of the calculated data inside
 *  the fragments to the renderer**/
class MainActivityViewModel : ViewModel() {

    //Database
    private lateinit var database: DatabaseViewModel

    private var _currentUser: MutableLiveData<User?> = MutableLiveData(null)
    val currentUser get() = _currentUser

    private var _currentScenario: MutableLiveData<Scenario?> = MutableLiveData(null)
    val currentScenario get() = _currentScenario

    private var _currentTestCase: MutableLiveData<TestCase?> = MutableLiveData(null)
    val currentTestCase get() = _currentTestCase

    private var _dataBaseObjectsSet: MutableLiveData<Boolean> = MutableLiveData(false)
    val dataBaseObjectsSet get() = _dataBaseObjectsSet

    private var _counting: Boolean = false
    val counting get() = _counting

    var testCase = 0

    //set false if new ui is loaded
    fun setDatabaseObjectsSet(case: Boolean) {
        _dataBaseObjectsSet.value = case
    }

    private var pose: Pose? = null
    private var newHeight = 0f
    private lateinit var _renderer: HelloArRenderer
    val renderer get() = _renderer

    //to keep it simple just use some workarounds by not manipulating it at all
    private var _currentPosition: MutableLiveData<Int> = MutableLiveData(0)
    val currentPosition get() = _currentPosition

    //map scaling
    private var _scale: MutableLiveData<Float> = MutableLiveData<Float>(Constants.scaleFactor)
    val scale get() = _scale

    private var _touchEvent: MutableLiveData<MotionEvent> = MutableLiveData()
    val touchEvent get() = _touchEvent

    private var oldDegree = 0f

    private var _clickable: MutableLiveData<Boolean> = MutableLiveData(true)
    val clickable get() = _clickable

    private var _targetIndex: MutableLiveData<Int> = MutableLiveData(0)
    val targetIndex get() = _targetIndex

    private var initialY = 0f

    private var viewScale = 0f

    fun resetTargetIndex() {
        this._targetIndex.value = 0
    }

    fun setScale(scale: Float) {
        _scale.value = scale
    }

    fun setClickable(b: Boolean) {
        this._clickable.value = b
    }

    fun setRenderer(renderer: HelloArRenderer) {
        _renderer = renderer
    }

    fun setTouchEvent(motionEvent: MotionEvent) {
        if (motionEvent.action == MotionEvent.ACTION_MOVE)
            this._touchEvent.value = motionEvent
    }


    //Todo eventually performance issues due to unnecessary calculations
    fun changeAnchorPosition() {

        renderer.wrappedAnchors.takeIf { it.isNotEmpty() }?.let {

            val currentTouchEvent = touchEvent.value

            currentTouchEvent?.let {
                when (it.action) {

                    MotionEvent.ACTION_MOVE -> {
                        setHeight(it.y)
                    }
                }
            }
            renderer.moveAnchorHeight(newHeight, 0)

        }
    }

    fun setScaleFactor(view: View) {
        this.viewScale = (Constants.bitmapSize.toFloat() / view.height.toFloat())
    }

    fun setInitialY(y: Float) {
        initialY = y
    }

    private fun setHeight(newY: Float) {

        val currentHeight = pose!!.ty()
        val addedHeight = ((initialY - newY) * viewScale) / 500
        val newHeight = currentHeight + addedHeight

        this.newHeight = newHeight

    }

    fun changeAnchorsPlaneCamera(position: Pair<Float, Float>) =
        renderer.moveAnchorPlane(position.first, position.second, currentPosition.value!!)

    fun setPose() {
        renderer.wrappedAnchors.takeIf { it.isNotEmpty() }?.let {
            this.pose = renderer.wrappedAnchors[currentPosition.value!!].anchor.pose
        }
    }

    fun rotateObject(motionEvent: MotionEvent, currentMain: Float) {
        val rotation = ((motionEvent.getX(0) - currentMain) / 1.47) % 360
        renderer.rotateAnchor(rotation.toFloat(), currentPosition.value!!)
    }

    fun resetRotation() {
        oldDegree = 0f
    }

    fun placeObjectInFocus() { //gets camera rotation and places object in front of it
        val rotation = renderer.refreshAngle()
        val camPos = renderer.camera.value!!.pose

        val newX = 0
        val newZ = -1

        val x1 = (cos(rotation) * newX - sin(rotation) * newZ)
        val z1 = (sin(rotation) * newX + cos(rotation) * newZ)

        renderer.moveAnchorPlane(x1 + camPos.tx(), -z1 + camPos.tz(), 0)
        renderer.moveAnchorHeight(camPos.ty(), 0)
    }

    fun createTarget() {
        renderer.wrappedAnchors.add(renderer.secondAnchor)
    }

    //places object around the user; gets data from constant values
    fun placeTargetOnNewPosition() {

        if (targetIndex.value!! <= Constants.maxTargets) {
            val rotation = renderer.refreshAngle()
            val camPos = renderer.camera.value!!.pose

            val position = ObjectCoords.positions[targetIndex.value!! - 1]

            val newX = position.first.toFloat()
            val newZ = -position.second.toFloat()
            val newY = position.third.toFloat()

            val x1 = (cos(rotation) * newX - sin(rotation) * newZ)
            val z1 = (sin(rotation) * newX + cos(rotation) * newZ)

            renderer.moveAnchorPlane(x1 + camPos.tx(), -z1 + camPos.tz(), 1)
            renderer.moveAnchorHeight((camPos.ty() + newY), 0)
        }
    }

    fun setUpDatabase(database: DatabaseViewModel) {
        this.database = database
    }

    fun setLastUser() {
        viewModelScope.launch(Dispatchers.IO) {
            _currentUser.postValue(database.getLastUser())
        }
    }

    fun checkCurrentUser() {
        if (currentUser.value == null) {
            _dataBaseObjectsSet.value = true
        } else {
            if (currentUser.value!!.Done) {
                Log.d("DeleteUserTest", "OK")
                renderer.deleteAnchor()
                _currentUser.value = null
                _currentScenario.value = null
                _currentTestCase.value = null
                resetTargetIndex()
            } else {
                viewModelScope.launch(Dispatchers.IO) {
                    _currentScenario.postValue(database.getLastScenarioByUserId(currentUser.value!!.UserID))
                }
            }
        }
    }

    fun checkCurrentScenario() {
        if (currentScenario.value == null) {
            viewModelScope.launch(Dispatchers.IO) {
                createNewScenario()
            }
        } else
            viewModelScope.launch(Dispatchers.IO) {
                _currentTestCase.postValue(database.getLastTestCaseOfScenario(currentScenario.value!!.ScenarioID))
            }
    }

    fun checkCurrentTestCase() {
        if (currentTestCase.value == null) {
            resetTargetIndex()
            createTestCase()
        } else {
            if (currentTestCase.value!!.EndTime == null) {
                database.deleteDataSet(currentTestCase.value!!.TestCaseID)
                _targetIndex.value = currentTestCase.value!!.TestCaseName

                //only true in case that its after app start and not in between rounds
                if (renderer.wrappedAnchors.size != 2)
                    _dataBaseObjectsSet.value = true
            } else {
                if (currentTestCase.value!!.TestCaseName == Constants.maxTargets) {
                    if (currentScenario.value!!.ScenarioName == 2) {
                        viewModelScope.launch(Dispatchers.IO) {
                            setUserDone()
                        }
                    } else {
                        viewModelScope.launch(Dispatchers.IO) {
                            createNewScenario()
                            _dataBaseObjectsSet.postValue(true)
                        }
                    }
                } else {
                    createTestCase()
                }
            }
        }
    }

    private suspend fun createNewScenario() {
        Log.d("TestingDatabase", "scenario")

        if (currentUser.value != null) {
            val scenarioName: Int = currentScenario.value?.ScenarioName?.plus(1) ?: 0

            val scenario = Scenario(
                UserID = currentUser.value!!.UserID,
                ScenarioName = scenarioName
            )
            database.insertScenario(scenario)
            _currentScenario.postValue(database.getLastScenarioByUserId(currentUser.value!!.UserID))
        }
    }

    private suspend fun setUserDone() { //Todo check completely
        database.updateUser(currentUser.value!!.UserID)
        _currentUser.postValue(database.getLastUser())
    }

    private fun createTestCase() {

        Log.d("TestingDatabase", "testcase")
        if (currentUser.value != null && currentScenario.value != null) {
            val testCase = TestCase(
                ScenarioID = currentScenario.value!!.ScenarioID,
                TestCaseName = targetIndex.value!! + 1,
                StartTime = System.currentTimeMillis().toString(),
                EndTime = null
            )

            viewModelScope.launch(Dispatchers.IO) {
                database.insertTestCase(testCase)

                _currentTestCase.postValue(database.getLastTestCaseOfScenario(currentScenario.value!!.ScenarioID))
                _counting = true
            }
        }
    }


    fun updateTestCase() {
        val currentTime = System.currentTimeMillis().toString()
        Log.d("SpecificTest", "testing the update")
        this.testCase = targetIndex.value!!
        viewModelScope.launch(Dispatchers.IO) {
            database.updateTestCase(currentTime, currentTestCase.value!!.TestCaseID)
            _counting = false
            _currentTestCase.postValue(database.getLastTestCaseOfScenario(currentScenario.value!!.ScenarioID))
        }
    }

    fun insertUser(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            database.insertUser(User(Username = name))
            _currentUser.postValue(database.getLastUser())
        }
    }

    fun insertDataSet() {
        val camera = renderer.camera.value!!.pose
        val movingObject = renderer.wrappedAnchors[0].anchor.pose
        val targetObject = renderer.wrappedAnchors[1].anchor.pose
        viewModelScope.launch(Dispatchers.IO) {
            database.insertDataSet(
                DataSet(
                    TestCaseID = currentTestCase.value!!.TestCaseID,
                    Time = System.currentTimeMillis().toString(),
                    CameraPositionX = camera.tx(),
                    CameraPositionY = camera.ty(),
                    CameraPositionZ = camera.tz(),
                    CameraRoatationX = camera.qx(),
                    CameraRoatationY = camera.ty(),
                    CameraRoatationZ = camera.qz(),
                    CameraRoatationW = camera.qw(),
                    Location_ManipulatedObjectX = movingObject.tx(),
                    Location_ManipulatedObjectY = movingObject.ty(),
                    Location_ManipulatedObjectZ = movingObject.tz(),
                    Location_TargetObjectX =targetObject.tx(),
                    Location_TargetObjectY =targetObject.ty(),
                    Location_TargetObjectZ =targetObject.tz(),
                )
            )
        }
    }
}