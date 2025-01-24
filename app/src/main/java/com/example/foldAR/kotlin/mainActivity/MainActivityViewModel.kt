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
import com.example.foldAR.kotlin.constants.Finished
import com.example.foldAR.kotlin.constants.ObjectCoords
import com.example.foldAR.kotlin.constants.ScenarioOrder
import com.example.foldAR.kotlin.constants.Scenarios
import com.example.foldAR.kotlin.renderer.HelloArRenderer
import com.google.ar.core.Pose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt


/**The viewModel handles the renderer as well as the delegation of the calculated data inside
 *  the fragments to the renderer**/
@Suppress("DEPRECATION")
class MainActivityViewModel : ViewModel() {

    companion object {
        const val TAG = "ViewModelTesting"
    }

    //Database
    private lateinit var database: DatabaseViewModel

    private var update = false

    private var _currentUser: MutableLiveData<User?> = MutableLiveData(null)
    val currentUser get() = _currentUser

    private var _currentScenario: MutableLiveData<Scenario?> = MutableLiveData(null)
    val currentScenario get() = _currentScenario

    private var _currentTestCase: MutableLiveData<TestCase?> = MutableLiveData(null)
    val currentTestCase get() = _currentTestCase

    private var _dataBaseObjectsSet: MutableLiveData<Boolean> = MutableLiveData(false)
    val dataBaseObjectsSet get() = _dataBaseObjectsSet

    //Dialog variables
    private var _finished: MutableLiveData<Finished> = MutableLiveData(Finished.NOTFINISHED)
    val finished get() = _finished

    private var _isAlertDialogOpen: Boolean = false
    val isAlertDialogOpen get() = _isAlertDialogOpen

    private lateinit var _startingPosition: Pose
    private val startingPosition get() = _startingPosition

    private var _startingDegree: Float = 0f
    private val startingDegree get() = _startingDegree

    private val maxDistance = 0.5
    private val maxRotation = 0.349065f

    //set false if new ui is loaded
    fun setDatabaseObjectsSet(case: Boolean) {
        _dataBaseObjectsSet.value = case
    }

    //general manipulation
    private lateinit var _renderer: HelloArRenderer
    val renderer get() = _renderer

    private var pose: Pose? = null
    private var newHeight = 0f

    //to keep it simple just use some workarounds by not manipulating it at all
    private var _currentPosition: MutableLiveData<Int> = MutableLiveData(0)
    val currentPosition get() = _currentPosition

    private var _clickable: MutableLiveData<Boolean> = MutableLiveData(false)
    val clickable get() = _clickable

    private var _targetIndex: MutableLiveData<Int> = MutableLiveData(0)
    val targetIndex get() = _targetIndex


    //glSurfaceView variables
    private var viewScaleHeight = 0f
    private var viewScaleWidth = 0f

    private var dimension: Int = 0
    private val range = Constants.SCALE_FACTOR * 0.1f
    private var rotation: Float = 0f

    private var initialX = 0f
    private var initialY = 0f
    private var initialZ = 0f

    private var firstFinger = true

    fun setDimension(height: Int) {
        this.dimension = height
    }

    fun resetTargetIndex() {
        this._targetIndex.value = 0
    }

    fun setClickable(b: Boolean) {
        this._clickable.value = b
    }

    fun setRenderer(renderer: HelloArRenderer) {
        _renderer = renderer
    }

    fun setRotationAngle() {
        this.rotation = renderer.refreshAngle()
    }

    fun setIsAlertDialogOpen(b: Boolean) {
        this._isAlertDialogOpen = b
    }

    fun setScaleFactor(view: View) {
        this.viewScaleHeight = (Constants.BITMAP_SIZE.toFloat() / view.height.toFloat())
        this.viewScaleWidth = (Constants.BITMAP_SIZE.toFloat() / view.width.toFloat())
    }

    fun setPose() {
        renderer.wrappedAnchors.takeIf { it.isNotEmpty() }?.let {
            this.pose = renderer.wrappedAnchors[currentPosition.value!!].anchor.pose
        }
    }

    /**
     * Manipulation
     */

    fun glSurfaceViewFoldAR(motionEvent: MotionEvent, placement: Boolean) {

        if (!placement && motionEvent.pointerCount == 1) {
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                this.initialY = motionEvent.y
                setPose()
            }
            if (motionEvent.action == MotionEvent.ACTION_MOVE && motionEvent.y < dimension) {
                glSurfaceViewChangeAnchor(motionEvent)
            }
        }
    }

    //one finger x and y, if second finger is set z
    fun glSurfaceViewStateOfTheArt(
        event: MotionEvent,
        placement: Boolean,
    ) {

        if (placement) return
        when (event.pointerCount) {
            1 -> oneFinger(event)
            2 -> twoFingers(event)
        }
    }


    private fun oneFinger(event: MotionEvent) {


        if (event.action == MotionEvent.ACTION_DOWN || !firstFinger) {
            this.initialX = event.x
            this.initialY = event.y
            setPose()
        }
        if (event.action == MotionEvent.ACTION_MOVE) {
            changeAnchorsPlaneCamera(
                moveAnchorsPlane(
                    Pair(initialX, event.y),
                    Pair(event.x, event.y)
                )
            )
            glSurfaceViewChangeAnchor(event)
        }
        firstFinger = true

    }

    private fun twoFingers(event: MotionEvent) {


        if (event.action == MotionEvent.ACTION_POINTER_2_DOWN || firstFinger) {
            this.initialZ = -event.getY(1)
            setPose()
        }
        changeAnchorsPlaneCamera(
            moveAnchorsPlane(
                Pair(event.getX(1), -initialZ),
                Pair(event.getX(1), event.getY(1))
            )
        )
        firstFinger = false
    }

    private fun moveAnchorsPlane(
        startingPoint: Pair<Float, Float>,
        event: Pair<Float, Float>
    ): Pair<Float, Float> {

        val pointX: Float = (event.first - startingPoint.first) * viewScaleWidth
        val pointZ: Float = (event.second - startingPoint.second) * viewScaleHeight

        val newX = -(pointX / (Constants.METER_TO_CM))
        val newZ = (pointZ / (Constants.METER_TO_CM))

        val x = (cos(rotation) * newX - sin(rotation) * newZ) * range
        val z = -(sin(rotation) * newX + cos(rotation) * newZ) * range

        return Pair(x, z)
    }

    //Todo eventually performance issues due to unnecessary calculations
    private fun glSurfaceViewChangeAnchor(motionEvent: MotionEvent) {

        renderer.wrappedAnchors.takeIf { it.isNotEmpty() }?.let {
            if (motionEvent.action == MotionEvent.ACTION_MOVE) setHeight(motionEvent.y)
            renderer.moveAnchorHeight(newHeight, 0)
        }
    }

    private fun setHeight(newY: Float) {

        val addedHeight = ((initialY - newY) * viewScaleHeight) / 500
        val newHeight = pose!!.ty() + addedHeight

        this.newHeight = newHeight

    }

    fun changeAnchorsPlaneCamera(position: Pair<Float, Float>) =
        renderer.moveAnchorPlane(
            position.first + pose!!.tx(),
            position.second + pose!!.tz(),
            currentPosition.value!!
        )

    /**
     * Tests
     * **/
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
        if (targetIndex.value!! <= Constants.MAX_TARGETS) {
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

    /**
     * Database
     * **/
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
                renderer.deleteAnchor()
                _currentUser.value = null
                _currentScenario.value = null
                _currentTestCase.value = null
                renderer.resetReached()
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
                if (currentTestCase.value!!.TestCaseName == Constants.MAX_TARGETS) {
                    if (currentScenario.value!!.ScenarioName == 2) {
                        viewModelScope.launch(Dispatchers.IO) {
                            setUserDone()
                        }
                    } else {
                        viewModelScope.launch(Dispatchers.IO) {
                            createNewScenario()
                            _dataBaseObjectsSet.postValue(true)
                            _finished.postValue(Finished.SCENARIO)
                        }
                    }
                } else {
                    createTestCase()
                    _finished.postValue(Finished.TEST)
                }
            }
        }
    }

    private suspend fun createNewScenario() {

        if (currentUser.value != null) {
            val id = currentUser.value!!.UserID % 6
            val triple = ScenarioOrder().orderList[id]

            val scenarioName: Int = currentScenario.value?.ScenarioName?.plus(1) ?: 0

            val scenario = Scenario(
                UserID = currentUser.value!!.UserID,
                ScenarioName = scenarioName,
                ScenarioCase = when (scenarioName) {
                    0 -> triple.first
                    1 -> triple.second
                    2 -> triple.third
                    else -> Scenarios.NOVALIDSCENARIO
                }
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

        if (currentUser.value != null && currentScenario.value != null) {
            val testCase = TestCase(
                ScenarioID = currentScenario.value!!.ScenarioID,
                TestCaseName = targetIndex.value!! + 1,
                StartTime = null,
                EndTime = null,
                Distance = null
            )

            viewModelScope.launch(Dispatchers.IO) {
                database.insertTestCase(testCase)
                _currentTestCase.postValue(database.getLastTestCaseOfScenario(currentScenario.value!!.ScenarioID))
            }
        }
    }

    fun updateTestCaseEndTime() {
        update = false
        val currentTime = System.currentTimeMillis().toString()

        viewModelScope.launch(Dispatchers.IO) {

            val currentTestCaseId = currentTestCase.value?.TestCaseID
            currentTestCaseId?.let {
                database.updateEndTime(currentTime, it)
                database.updateDistance(renderer.distance, it)
                _currentTestCase.postValue(database.getLastTestCaseOfScenario(currentScenario.value!!.ScenarioID))
            }
        }
    }

    fun updateTestCaseStartTime() {

        val currentTime = System.currentTimeMillis().toString()
        update = true

        viewModelScope.launch(Dispatchers.IO) {
            database.updateStartTime(currentTime, currentTestCase.value!!.TestCaseID)
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

        if (update) {
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
                        Location_TargetObjectX = targetObject.tx(),
                        Location_TargetObjectY = targetObject.ty(),
                        Location_TargetObjectZ = targetObject.tz(),
                    )
                )
            }
        }
    }

    fun setStartingVariables() {

        this._startingPosition = renderer.camera.value!!.pose
        this._startingDegree = rotation
    }

    fun checkCorrectUserPosition(): String {
        val a = startingPosition
        val b = renderer.camera.value!!.pose

        val distance =
            sqrt((b.tx() - a.tx()).pow(2) + (b.ty() - a.ty()).pow(2) + (b.tz() - a.tz()).pow(2))
        val rotationDifference = abs(startingDegree - rotation)

        Log.d(TAG, "Distanz: $distance      Rotation: $rotationDifference")

        if (distance > maxDistance)
            return "Distanz $distance"
        if (rotationDifference > maxRotation)
            return "Ausrichtung $rotationDifference"

        return ""
    }
}