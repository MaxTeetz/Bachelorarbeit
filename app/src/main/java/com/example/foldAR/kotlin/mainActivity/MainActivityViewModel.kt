package com.example.foldAR.kotlin.mainActivity

import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.foldAR.kotlin.anchorManipulation.ChangeAnchor
import com.example.foldAR.kotlin.constants.Constants
import com.example.foldAR.kotlin.constants.ObjectCoords
import com.example.foldAR.kotlin.renderer.HelloArRenderer
import com.google.ar.core.Pose
import kotlin.math.cos
import kotlin.math.sin


/**The viewModel handles the renderer as well as the delegation of the calculated data inside
 *  the fragments to the renderer**/
class MainActivityViewModel : ViewModel() {
    private var _changeAnchor = ChangeAnchor()
    private val changeAnchor get() = _changeAnchor

    private var pose: Pose? = null

    private lateinit var _renderer: HelloArRenderer
    val renderer get() = _renderer

    //to keep it simple just use some workarounds by not manipulating it at all.
    private var _currentPosition: MutableLiveData<Int> = MutableLiveData(0)
    val currentPosition get() = _currentPosition

    private var _listIndex: MutableLiveData<Int> = MutableLiveData(0)
    val listIndex get() = _listIndex

    private var _scale: MutableLiveData<Float> = MutableLiveData<Float>(Constants.scaleFactor)
    val scale get() = _scale

    private var _touchEvent: MutableLiveData<MotionEvent> = MutableLiveData()
    val touchEvent get() = _touchEvent

    private var oldDegree = 0f

    private var _clickable: MutableLiveData<Boolean> = MutableLiveData(true)
    val clickable get() = _clickable

    fun setIndex() {

        _listIndex.value = _listIndex.value!! + 1
        Log.d("IndexNumber", listIndex.value.toString())
        if (listIndex.value!! >= 5)
            _listIndex.value = 0
    }

    fun getIndex(): Int = listIndex.value!!

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

    fun changeAnchorPosition(view: View) {
        renderer.wrappedAnchors.takeIf { it.isNotEmpty() }?.let {
            val scaleFactorY = 500 / view.height
            val currentTouchEvent = touchEvent.value


            currentTouchEvent?.let {

                when (it.action) {
                    MotionEvent.ACTION_DOWN -> changeAnchor.setOffset(it.y * scaleFactorY)

                    MotionEvent.ACTION_MOVE -> changeAnchor.getNewPosition(it, view, pose!!, null)
                }
            }
            changeAnchorsHeight()
        }
    }

    private fun changeAnchorsHeight() =
        renderer.moveAnchorHeight(changeAnchor.newY, currentPosition.value!!)

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
        val rotation = renderer.refreshAngle()
        val camPos = renderer.camera.value!!.pose
        val position = ObjectCoords.positions[getIndex() - 1]

        val newX = position.first.toFloat()
        val newZ = -position.second.toFloat()
        val newY = position.third.toFloat()

        val x1 = (cos(rotation) * newX - sin(rotation) * newZ)
        val z1 = (sin(rotation) * newX + cos(rotation) * newZ)

        renderer.moveAnchorPlane(x1 + camPos.tx(), -z1 + camPos.tz(), 1)
        renderer.moveAnchorHeight((camPos.ty() + newY), 0)
    }

}