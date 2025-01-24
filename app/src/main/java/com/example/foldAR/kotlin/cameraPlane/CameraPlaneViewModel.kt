package com.example.foldAR.kotlin.cameraPlane

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Color.TRANSPARENT
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.ViewModel
import com.example.foldAR.kotlin.constants.Constants
import com.example.foldAR.kotlin.renderer.WrappedAnchor
import com.google.ar.core.Camera
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/** Initially facing towards negative z. Thus swapping left/right and front/behind.
 * Because a bitmaps origin is in the upper left, it need to be centered and the x-axis needs to be swapped.
 * No need to swap the z-axis, because towards top is already negative.
 * Because the bitmap has a size of 500x500, the points drawn and moved need to be normalized to meters.
 * */
class CameraPlaneViewModel : ViewModel() {

    //Only formatting because I need to call Constants less
    companion object {
        private const val BITMAP_SIZE = Constants.BITMAP_SIZE
        private const val BITMAP_SIZE_FLOAT = Constants.BITMAP_SIZE.toFloat()
        private const val MID_POINT = BITMAP_SIZE_FLOAT / 2
        private const val RADIUS = 5f

        private val paintObjects = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.RED
            strokeWidth = 2f
        }

        private val paintObjectChosen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.GREEN
            strokeWidth = 2f
        }

        private val paintAxis = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLUE
            strokeWidth = 2f
        }

        private val paintGrid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLUE
            strokeWidth = 1f
        }

    }

    private var _range = 1f

    /**Times 0.4 because bitmap sides are 250cm
     * Without it range of 1m would be 2.5m at each side scaling it out of proportion
     * I.e. 1m = 250/2.5
     *      2m = 250 / 2.5 * 2
     *    ..5m = 250 / 2.5 * 5
     *    Using multiplication because it needs less computing power than division
     *    */
    private val range get() = _range * 0.4f

    private var _currentPosition = 0
    private val currentPosition get() = _currentPosition

    private var camPosX = 0f
    private var camPosZ = 0f
    private var rotation: Float = 0f
    private var center = BITMAP_SIZE / 2

    private val bitmap: Bitmap =
        Bitmap.createBitmap(BITMAP_SIZE, BITMAP_SIZE, Bitmap.Config.ARGB_8888)

    private val bitmapCoordinateSystem: Bitmap =
        Bitmap.createBitmap(BITMAP_SIZE, BITMAP_SIZE, Bitmap.Config.ARGB_8888)

    fun setRange(range: Float) {
        if (range in Constants.SLIDER_MIN..Constants.SLIDER_MAX)
            _range = range
    }

    fun setCurrentPosition(position: Int) {
        if (position in 0..Constants.OBJECTS_MAX_SIZE + 1)
            _currentPosition = position
    }

    //map the available objects onto the bitmap
    suspend fun mapAnchors(
        camera: Camera,
        wrappedAnchors: MutableList<WrappedAnchor>,
        refreshAngle: Float,
    ): Bitmap {

        if (wrappedAnchors.isEmpty()) return combineBitmaps()

        this.rotation = refreshAngle
        this.camPosX = camera.pose.translation[0]
        this.camPosZ = camera.pose.translation[2]

        bitmap.eraseColor(TRANSPARENT)

        val mutex = Mutex()

        val anchor = wrappedAnchors[0].anchor

        //Todo necessary
        mutex.withLock {

            val anchorPose = anchor.pose
            val (anchorPoseX, anchorPoseZ) = arrayOf(anchorPose.tx(), anchorPose.tz())

            if (isInRange(anchorPoseX, anchorPoseZ, camPosX, camPosZ)) {
                drawPoint(anchorPoseX, anchorPoseZ, 0)
            }

            return combineBitmaps()
        }
    }

    private fun combineBitmaps(): Bitmap {
        val combinedBitmap =
            Bitmap.createBitmap(Constants.BITMAP_SIZE, Constants.BITMAP_SIZE, bitmap.config)
        val canvas = Canvas(combinedBitmap)
        canvas.drawBitmap(bitmapCoordinateSystem, 0f, 0f, null)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        return combinedBitmap
    }

    /** Draw the point by rotating it by the phones yaw and adding the camera coordinates*/
    private fun drawPoint(
        poseX: Float,
        poseZ: Float,
        index: Int
    ) {

        val canvas = Canvas(bitmap)
        val newX = (poseX - camPosX) * (Constants.METER_TO_CM)
        val newZ = (poseZ - camPosZ) * (Constants.METER_TO_CM)

        val newRotatedX = (cos(rotation) * newX - sin(rotation) * newZ) / range
        val newRotatedZ = (sin(rotation) * newX + cos(rotation) * newZ) / range

        canvas.drawCircle(
            (-newRotatedX + center),
            (-newRotatedZ + center),
            RADIUS,
            if (index == currentPosition) paintObjectChosen else paintObjects
        )
    }

    //checks if the point is in a circle with the radius of the designated range
    private fun isInRange(poseX: Float, poseZ: Float, camPosX: Float, camPosZ: Float): Boolean {
        val distance = kotlin.math.sqrt((poseX - camPosX).pow(2) + (poseZ - camPosZ).pow(2))
        return distance * 0.4 < range
    }


    /**Moving the anchor to the point clicked by rotating the point by the phones yaw and adding the cameras
     * position to it.
     * */
    fun moveAnchors(
        startingPoint: Pair<Float, Float>,
        event: MotionEvent,
        view: View
    ): Pair<Float, Float> {

        val scaleFactorX = bitmap.width.toFloat() / view.width
        val scaleFactorY = bitmap.height.toFloat() / view.height

        val pointX = (event.x - startingPoint.first) * scaleFactorX
        val pointZ = (event.y - startingPoint.second) * scaleFactorY

        val newX = -(pointX / (Constants.METER_TO_CM))
        val newZ = (pointZ / (Constants.METER_TO_CM))

        val x = (cos(rotation) * newX - sin(rotation) * newZ) * range
        val z = -(sin(rotation) * newX + cos(rotation) * newZ) * range

        return Pair(x, z)
    }

    fun drawCoordinateSystem(): Bitmap {
        bitmapCoordinateSystem.eraseColor(TRANSPARENT)
        val canvas = Canvas(bitmapCoordinateSystem)


        drawAxis(canvas)
        drawGrid(canvas)

        return combineBitmaps()
    }

    private fun drawAxis(canvas: Canvas) {
        val endOffset = 20f

        // Draw horizontal and vertical lines
        drawLine(0f, MID_POINT, BITMAP_SIZE_FLOAT, MID_POINT, paintAxis, canvas)
        drawLine(MID_POINT, 0f, MID_POINT, BITMAP_SIZE_FLOAT, paintAxis, canvas)

        // Draw arrow heads for x-axis
        drawLine(
            BITMAP_SIZE_FLOAT,
            MID_POINT,
            BITMAP_SIZE_FLOAT - endOffset,
            MID_POINT - endOffset,
            paintAxis,
            canvas
        )
        drawLine(
            BITMAP_SIZE_FLOAT,
            MID_POINT,
            BITMAP_SIZE_FLOAT - endOffset,
            MID_POINT + endOffset,
            paintAxis,
            canvas
        )

        // Draw arrow heads for y-axis
        drawLine(
            MID_POINT, 0f, MID_POINT - endOffset, endOffset, paintAxis, canvas
        )
        drawLine(
            MID_POINT, 0f, MID_POINT + endOffset, endOffset, paintAxis, canvas
        )
    }

    private fun drawGrid(canvas: Canvas) {

        val distance = (BITMAP_SIZE / 2) / (_range + 1)

        var i = 1f
        do {
            drawLine(
                MID_POINT - (distance * i),
                0f,
                MID_POINT - (distance * i),
                BITMAP_SIZE_FLOAT,
                paintGrid,
                canvas
            )
            drawLine(
                MID_POINT + (distance * i),
                0f,
                MID_POINT + (distance * i),
                BITMAP_SIZE_FLOAT,
                paintGrid,
                canvas
            )
            drawLine(
                0f,
                MID_POINT - (distance * i),
                BITMAP_SIZE_FLOAT,
                MID_POINT - (distance * i),
                paintGrid,
                canvas
            )
            drawLine(
                0f,
                MID_POINT + (distance * i),
                BITMAP_SIZE_FLOAT,
                MID_POINT + (distance * i),
                paintGrid,
                canvas
            )

            i++
        } while (i < _range + 1)
    }

    private fun drawLine(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        paint: Paint,
        canvas: Canvas
    ) =
        canvas.drawLine(startX, startY, endX, endY, paint)
}