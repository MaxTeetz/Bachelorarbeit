package com.example.foldAR.kotlin.mainActivity

import android.annotation.SuppressLint
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.foldAR.java.helpers.CameraPermissionHelper
import com.example.foldAR.java.helpers.DepthSettings
import com.example.foldAR.java.helpers.InstantPlacementSettings
import com.example.foldAR.java.helpers.SnackbarHelper
import com.example.foldAR.java.helpers.TapHelper
import com.example.foldAR.java.samplerender.SampleRender
import com.example.foldAR.kotlin.dialog.DialogObjectOptions
import com.example.foldAR.kotlin.helloar.R
import com.example.foldAR.kotlin.helloar.databinding.ActivityMainBinding
import com.example.foldAR.kotlin.helpers.ARCoreSessionLifecycleHelper
import com.example.foldAR.kotlin.renderer.HelloArRenderer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.ar.core.Config
import com.google.ar.core.Config.InstantPlacementMode
import com.google.ar.core.Session
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivityTest"
    }

    private lateinit var navController: NavController
    private lateinit var _binding: ActivityMainBinding
    private val binding get() = _binding
    private val viewModel: MainActivityViewModel by viewModels()

    private lateinit var surfaceView: GLSurfaceView
    private lateinit var renderer: HelloArRenderer
    lateinit var tapHelper: TapHelper

    lateinit var arCoreSessionHelper: ARCoreSessionLifecycleHelper

    val snackbarHelper = SnackbarHelper()
    val instantPlacementSettings = InstantPlacementSettings()
    val depthSettings = DepthSettings()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupBinding()
        setupNavigation()
        setupArCoreSessionHelper()
        setupRenderer()
        setupSettings()
        setupButtons()
        setUpMovementObserver()
        setUpClickableObserver()
        //setUpNextRoundObserver()
        setUpNextTargetObserver()
    }

    private fun setUpMovementObserver() {
        viewModel.touchEvent.observe(this) {
            viewModel.changeAnchorPosition(binding.surfaceview)
        }
    }

    private fun setUpClickableObserver() {
        viewModel.clickable.observe(this) {
            binding.settingsButton.isClickable = it
        }
    }

    private fun setUpNextRoundObserver() {
        viewModel.listIndex.observe(this) {
            if (viewModel.listIndex.value == 0 && renderer.wrappedAnchors.isNotEmpty()) {
                Log.d("CrashTest", "1")
                //renderer.deleteAnchor()

                Log.d("CrashTest", "2")

                Toast.makeText(
                    this,
                    "Scenario abgeschlossen. Platziere ein neues Objekt, um fortfahren zu können!",
                    Toast.LENGTH_LONG
                ).show()

                Log.d("CrashTest", "3")

                viewModel.setClickable(true)
                Log.d("CrashTest", "4")

            }
        }
    }

    private fun setUpNextTargetObserver() {
        renderer.reached.observe(this) {
            if (it == true && viewModel.listIndex.value!! > 0) {
                showAlert()
                renderer.resetReached()
            }
        }
    }

    //shows if target is reached
    private fun showAlert() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(android.R.string.dialog_alert_title))
            .setMessage("${viewModel.listIndex.value!!}/20")
            .setCancelable(false)
            .setPositiveButton("Nächste Runde") { dialogInterface, _ ->
                viewModel.setIndex()
                viewModel.placeTargetOnNewPosition()
                viewModel.placeObjectInFocus()
                dialogInterface.dismiss()
                Log.d("AlertTest", viewModel.listIndex.value!!.toString())
            }
            .show()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupBinding() {
        surfaceView = findViewById(R.id.surfaceview)
        tapHelper = TapHelper(this, viewModel).also { surfaceView.setOnTouchListener(it) }
    }

    private fun setupNavigation() {

//        val navView: BottomNavigationView = binding.navView

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        navController = navHostFragment.navController

        supportActionBar?.hide()

    }

    private fun setupButtons() {
        binding.apply {
            settingsButton.setOnClickListener {
                DialogObjectOptions.newInstance().show(supportFragmentManager, "")
            }
        }
    }

    private fun setupArCoreSessionHelper() {
        arCoreSessionHelper = ARCoreSessionLifecycleHelper(this)

        arCoreSessionHelper.exceptionCallback = { exception ->
            val message = when (exception) {
                is UnavailableUserDeclinedInstallationException -> "Please install Google Play Services for AR"

                is UnavailableApkTooOldException -> "Please update ARCore"
                is UnavailableSdkTooOldException -> "Please update this app"
                is UnavailableDeviceNotCompatibleException -> "This device does not support AR"
                is CameraNotAvailableException -> "Camera not available. Try restarting the app."
                else -> "Failed to create AR session: $exception"
            }
            Log.e(TAG, "ARCore threw an exception", exception)
            snackbarHelper.showError(this, message)

        }
        arCoreSessionHelper.beforeSessionResume = ::configureSession

        lifecycle.addObserver(arCoreSessionHelper)
    }

    private fun setupRenderer() {
        renderer = HelloArRenderer(this)
        lifecycle.addObserver(renderer)
        SampleRender(surfaceView, renderer, assets)
        viewModel.setRenderer(renderer)
    }

    private fun setupSettings() {
        depthSettings.onCreate(this)
        instantPlacementSettings.onCreate(this)
    }

    // Configure the session, using Lighting Estimation, and Depth mode.
    private fun configureSession(session: Session) {
        session.configure(session.config.apply {
            lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR

            // Depth API is used if it is configured in Hello AR's settings.
            depthMode = if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                Config.DepthMode.AUTOMATIC
            } else {
                Config.DepthMode.DISABLED
            }

            // Instant Placement is used if it is configured in Hello AR's settings.
            instantPlacementMode = if (instantPlacementSettings.isInstantPlacementEnabled) {
                InstantPlacementMode.LOCAL_Y_UP
            } else {
                InstantPlacementMode.DISABLED
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        results: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(
                this, "Camera permission is needed to run this application", Toast.LENGTH_LONG
            ).show()
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                CameraPermissionHelper.launchPermissionSettings(this)
            }
            finish()
        }
    }

    /**
     * Not necessary for current app but useful if used on other devices
     * **/
    fun showOcclusionDialogIfNeeded() {
        val session = arCoreSessionHelper.session ?: return
        val isDepthSupported = session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)
        if (!this.depthSettings.shouldShowDepthEnableDialog() || !isDepthSupported) {
            return // Don't need to show dialog.
        }

        // Asks the user whether they want to use depth-based occlusion.
        AlertDialog.Builder(this).setTitle(R.string.options_title_with_depth)
            .setMessage(R.string.depth_use_explanation)
            .setPositiveButton(R.string.button_text_enable_depth) { _, _ ->
                this.depthSettings.setUseDepthForOcclusion(true)
            }.setNegativeButton(R.string.button_text_disable_depth) { _, _ ->
                this.depthSettings.setUseDepthForOcclusion(false)
            }.show()
    }

    override fun onResume() {
        super.onResume()
        surfaceView.onResume()
    }

    override fun onPause() {
        super.onPause()
        surfaceView.onPause()
    }
}