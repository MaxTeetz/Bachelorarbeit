package com.example.foldAR.kotlin.cameraPlane

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.example.foldAR.kotlin.helloar.databinding.FragmentCameraPlaneBinding
import com.example.foldAR.kotlin.mainActivity.MainActivityViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class CameraPlaneFragment : Fragment() {

    private val viewModelActivity: MainActivityViewModel by activityViewModels()
    private val viewModel: CameraPlaneViewModel by viewModels()

    private var _binding: FragmentCameraPlaneBinding? = null
    private val binding get() = _binding!!

    private val coroutine1 = Job()
    private val coroutineScope1 = CoroutineScope(coroutine1 + Dispatchers.Main)

//    private var scaleFactor = Constants.scaleFactor
    private var previousCount = 0

    private lateinit var startingPoint: Pair<Float, Float>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCameraPlaneBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setObservers()
        moveObject()
        binding.imageMoveObjectPlane.setImageBitmap(viewModel.drawCoordinateSystem())
        //setZoomListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
        coroutine1.cancel()
    }


    private fun setObservers() {
        viewModelActivity.renderer.camera.observe(viewLifecycleOwner) { camera ->
            coroutineScope1.launch {
                binding.imageMoveObjectPlane.setImageBitmap(
                    viewModel.mapAnchors(
                        camera,
                        viewModelActivity.renderer.wrappedAnchors,
                        viewModelActivity.renderer.refreshAngle(),
                    )
                )
            }
        }

        viewModelActivity.scale.observe(viewLifecycleOwner) {
            viewModel.setRange(it)
            binding.imageMoveObjectPlane.setImageBitmap(viewModel.drawCoordinateSystem())
        }

        viewModelActivity.currentPosition.observe(viewLifecycleOwner) {
            viewModel.setCurrentPosition(it)
        }

    }

//    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
//        override fun onScale(detector: ScaleGestureDetector): Boolean {
//            scaleFactor *= detector.scaleFactor
//            scaleFactor = scaleFactor.coerceIn(1.0f, 5.0f)
//            viewModelActivity.setScale(scaleFactor)
//            return true
//        }
//
//        override fun onScaleEnd(detector: ScaleGestureDetector) {
//            super.onScaleEnd(detector)
//            //Todo detach and stuff ig
//        }
//    }

    @SuppressLint("ClickableViewAccessibility", "SuspiciousIndentation")
    private fun moveObject() {
//        val scaleGestureDetector = ScaleGestureDetector(requireContext(), ScaleListener())

        binding.imageMoveObjectPlane.setOnTouchListener { view, event ->
            viewModelActivity.renderer.wrappedAnchors.takeIf { it.isNotEmpty() }?.let {

                //scaleGestureDetector.onTouchEvent(event)

                if (event.pointerCount != previousCount) {
                    previousCount = event.pointerCount
                }

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        this.startingPoint = Pair(event.x, event.y)
                        viewModelActivity.setPose()
                    }

                    MotionEvent.ACTION_MOVE ->
                        if (previousCount == 1) {
                            if (event.y >= 0)
                                viewModelActivity.changeAnchorsPlaneCamera(
                                    viewModel.moveAnchors(this.startingPoint, event, binding.imageMoveObjectPlane)
                                )

                        }
                }
            }

            view.performClick()
            true
        }
    }

}