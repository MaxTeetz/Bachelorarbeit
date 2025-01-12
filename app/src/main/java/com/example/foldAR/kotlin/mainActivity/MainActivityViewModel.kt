package com.example.foldAR.kotlin.mainActivity

import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foldAR.data.DatabaseViewModel
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

    private var pose: Pose? = null
    private var newHeight = 0f
    private lateinit var _renderer: HelloArRenderer
    val renderer get() = _renderer

    //to keep it simple just use some workarounds by not manipulating it at all.
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

    private var viewScale = 0f;

    private fun setTargetIndexToCurrentTestIndex(id: Int) {
        _targetIndex.value = id
    }

    fun setTargetIndex() {
        this._targetIndex.value?.let { _targetIndex.value = it + 1 }
    }

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

        if (targetIndex.value!! < Constants.maxTargets) {
            val rotation = renderer.refreshAngle()
            val camPos = renderer.camera.value!!.pose

            val position = ObjectCoords.positions[targetIndex.value!!]

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
            _currentUser.value = database.getLastUser()
        }
    }

    fun checkCurrentUser() {
        if (currentUser.value == null)
            resetData()
        else {
            if (currentUser.value!!.Done) {
                resetData()
            } else {
                viewModelScope.launch(Dispatchers.IO) {
                    _currentScenario.value =
                        database.getLastScenarioByUserId(currentUser.value!!.UserID)
                }
            }
        }
    }

    fun checkCurrentScenario() {
        //erstmal Nutzer nicht aus DB löschen. Egal, wenn Nutzer leer
        if (currentScenario.value == null)
            resetData()
        else
            viewModelScope.launch(Dispatchers.IO) {
                _currentTestCase.value =
                    database.getLastTestCaseOfScenario(currentScenario.value!!.ScenarioID)
            }
    }

    fun checkCurrentTestCase() {
        if (currentTestCase.value == null) {
            resetTargetIndex()
            createTestCase()
        } else {
            if (currentTestCase.value!!.EndTime == null) {
                database.deleteDataSet(currentTestCase.value!!.TestCaseID)
                targetIndex.value = currentTestCase.value!!.TestCaseName
            } else {
                if (currentTestCase.value!!.TestCaseName == Constants.maxTargets - 1) {
                    if (currentScenario.value!!.ScenarioName == 2) {
                        setUserDone()
                    } else {
                        createNewScenario()
                    }
                } else {
                    createTestCase()
                }
            }
        }
    }

    private fun createNewScenario() {
        val scenario = Scenario(
            UserID = currentUser.value!!.UserID,
            ScenarioName = currentScenario.value!!.ScenarioName + 1
        )

        viewModelScope.launch(Dispatchers.IO) {
            database.insertScenario(scenario)
            currentScenario.value = scenario
        }
    }

    private fun setUserDone() { //Todo check completely
        viewModelScope.launch(Dispatchers.IO) {
            database.updateUser(currentUser.value!!.UserID)
            _currentUser.value = database.getLastUser()
        }
    }

    private fun createTestCase() {

        val testCase = TestCase(
            ScenarioID = currentScenario.value!!.ScenarioID,
            TestCaseName = targetIndex.value!!,
            StartTime = System.currentTimeMillis().toString(),
            EndTime = null
        )

        viewModelScope.launch(Dispatchers.IO) {
            database.insertTestCase(testCase)

            currentTestCase.value = testCase
        }
    }


    fun updateTestCase() {
        val currentTime = System.currentTimeMillis().toString()

        viewModelScope.launch(Dispatchers.IO) {
            database.updateTestCase(currentTime, currentTestCase.value!!.TestCaseID)
            currentTestCase.value = database.getLastTestCaseOfScenario(currentScenario.value!!.ScenarioID)
        }
    }

    private fun resetData() {
        _currentUser.value = null
        _currentScenario.value = null
        _currentTestCase.value = null
        _dataBaseObjectsSet.value = true
    }


}