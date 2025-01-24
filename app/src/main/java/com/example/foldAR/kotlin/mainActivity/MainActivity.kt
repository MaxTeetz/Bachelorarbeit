package com.example.foldAR.kotlin.mainActivity

import android.annotation.SuppressLint
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.foldAR.data.DatabaseApplication
import com.example.foldAR.data.DatabaseViewModel
import com.example.foldAR.data.DatabaseViewModelFactory
import com.example.foldAR.java.helpers.CameraPermissionHelper
import com.example.foldAR.java.helpers.DepthSettings
import com.example.foldAR.java.helpers.InstantPlacementSettings
import com.example.foldAR.java.helpers.SnackbarHelper
import com.example.foldAR.java.helpers.TapHelper
import com.example.foldAR.java.samplerender.SampleRender
import com.example.foldAR.kotlin.constants.Finished
import com.example.foldAR.kotlin.constants.PositioningCase
import com.example.foldAR.kotlin.constants.Scenarios
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

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivityTest"
    }

    private lateinit var databaseViewModel: DatabaseViewModel
    private val viewModel: MainActivityViewModel by viewModels()

    private lateinit var navController: NavController
    private lateinit var navHostFragment: NavHostFragment
    private lateinit var _binding: ActivityMainBinding
    private val binding get() = _binding

    private lateinit var surfaceView: GLSurfaceView
    private lateinit var renderer: HelloArRenderer
    lateinit var tapHelper: TapHelper

    lateinit var arCoreSessionHelper: ARCoreSessionLifecycleHelper

    val snackbarHelper = SnackbarHelper()
    val instantPlacementSettings = InstantPlacementSettings()
    val depthSettings = DepthSettings()


    private var isAlertDialogOpen = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)

        val usersDAO = (application as DatabaseApplication).database.usersDao()
        val scenariosDAO = (application as DatabaseApplication).database.scenariosDao()
        val testCaseDAO = (application as DatabaseApplication).database.testCasesDao()
        val dataSetsDAO = (application as DatabaseApplication).database.dataSetsDao()

        val factory = DatabaseViewModelFactory(usersDAO, scenariosDAO, testCaseDAO, dataSetsDAO)

        databaseViewModel = ViewModelProvider(this, factory)[DatabaseViewModel::class.java]

        setContentView(binding.root)
        setupBinding()
        setupNavigation()
        setupArCoreSessionHelper()
        setupRenderer()
        setupSettings()
        setupButtons()
        setScale()
        setUpClickableObserver()
        setUpNextRoundObserver()
        setUpNextTargetObserver()
        setUpDatabaseObservers()
        setUpDatabase()
        setUpFrameObserver()
        setupExplicitCaseObserver()
    }

    /**
     * Binding
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setupBinding() {
        surfaceView = findViewById(R.id.surfaceview)
        tapHelper = TapHelper(this, viewModel).also {
            surfaceView.setOnTouchListener(it)
        }
    }

    //start scenario button
    private fun setupButtons() {

        binding.settingsButton.apply {
            //wait until db is set
            isClickable = false

            setOnClickListener {
                if (!viewModel.clickable.value!!)
                    renderer.reachedTrue()
                else
                    DialogObjectOptions.newInstance().show(supportFragmentManager, "")
            }
        }
    }

    /**
     * Navigation
     */
    private fun setupNavigation() {
        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        navController = navHostFragment.navController
        supportActionBar?.hide()
    }

    /**
     * Database
     */
    private fun setUpDatabase() {
        viewModel.setUpDatabase(databaseViewModel)
        viewModel.setLastUser()
    }

    /**
     * Observers
     **/

    //change button outcome
    private fun setUpClickableObserver() {
        viewModel.clickable.observe(this) {
            binding.settingsButton.apply {
                if (it)
                    setBackgroundResource(R.drawable.ic_settings)
                else
                    setBackgroundResource(R.drawable.green_arrow)
            }

        }
    }

    //next target within scenario
    private fun setUpNextTargetObserver() {
        renderer.reached.observe(this) {
            if (it == true) {
                viewModel.updateTestCaseEndTime()
            }
        }
    }

    private fun setUpDatabaseObservers() {
        viewModel.dataBaseObjectsSet.observe(this) {
            viewModel.setClickable(it)
        }

        viewModel.currentUser.observe(this) {

            viewModel.checkCurrentUser()

            if (it != null) {
                if (it.Done)
                    tapHelper.onResume()
            }
        }

        viewModel.currentScenario.observe(this) {
            viewModel.checkCurrentScenario()
        }

        viewModel.currentTestCase.observe(this) {
            viewModel.checkCurrentTestCase()
        }
    }

    private fun setUpFrameObserver() {
        renderer.camera.observe(this) {
            viewModel.insertDataSet()
            viewModel.setRotationAngle()
        }
    }

    //Layout observer
    private fun setupExplicitCaseObserver() {
        viewModel.currentScenario.observe(this) { scenario ->
            scenario?.ScenarioCase?.let { scenarioCase ->
                tapHelper.setScenario(scenarioCase)
                selectLayout(scenarioCase)
            }
        }
    }

    //next scenario i.e. folded or unfolded
    private fun setUpNextRoundObserver() {
        viewModel.finished.observe(this) {
            if (it == Finished.SCENARIO)
                nextScenarioAlert()

            if (it == Finished.TEST)
                nextRoundAlert()
        }
    }

    /**
     * Alerts
     */
    private fun nextScenarioAlert() {
        renderer.deleteAnchor()
        viewModel.resetTargetIndex()
        renderer.resetReached()

        Toast.makeText(
            this,
            "Scenario ${viewModel.currentScenario.value!!.ScenarioCase}. Platziere ein neues Objekt, um fortfahren zu können!",
            Toast.LENGTH_LONG
        ).show()

        viewModel.setClickable(true)
        tapHelper.onResume()
    }

    //shows alert if target is reached
    private fun nextRoundAlert() {
        if (isAlertDialogOpen) {
            return
        }
        isAlertDialogOpen = true

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(getString(android.R.string.dialog_alert_title))
            .setMessage("Nächste Runde ${viewModel.targetIndex.value}/20")
            .setCancelable(false)
            .setPositiveButton("Nächste Runde") { dialogInterface, _ -> //Todo disable reached until both are placed
                if (viewModel.checkCorrectUserPosition() == PositioningCase.CORRECT) {
                    if (renderer.wrappedAnchors.isNotEmpty()) {
                        viewModel.placeTargetOnNewPosition()
                        viewModel.placeObjectInFocus()
                        renderer.resetReached()
                        viewModel.updateTestCaseStartTime()
                    }
                    dialogInterface.dismiss()
                } else
                    Toast.makeText(
                        this,
                        "Bitte zum Start zurückkehren und richtig ausrichten",
                        Toast.LENGTH_LONG
                    ).show()
            }.setOnDismissListener { isAlertDialogOpen = false }
            .create()

        dialog.apply {
            val window = dialog.window
            window?.setGravity(Gravity.TOP)

            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(window?.attributes)

            layoutParams.y = 100
            window?.attributes = layoutParams
        }
        dialog.show()
    }

    /**
     * Layout
     */
    private fun selectLayout(scenarioCase: Scenarios) {
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val height = displayMetrics.heightPixels

        if (scenarioCase == Scenarios.STATEOFTHEART) {
            navHostFragment.let { fragment ->
                supportFragmentManager.beginTransaction().detach(fragment).commit()
            }
            setFoldARLayout(height)
        } else {
            navHostFragment.let { fragment ->
                supportFragmentManager.beginTransaction().attach(fragment).commit()
            }
            setFoldARLayout(height / 2)
        }
    }

    private fun setFoldARLayout(height: Int) {

        val layoutParams = binding.surfaceview.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.height = height
        layoutParams.width = ConstraintLayout.LayoutParams.MATCH_PARENT
        binding.surfaceview.layoutParams = layoutParams

        binding.surfaceview.requestLayout()
        viewModel.setDimension(height)
    }

    /**
     * Layout dimensions for object manipulation
     */
    //gets view width after its completely inflated
    private fun setScale() {
        binding.surfaceview.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // Ensure this only happens once
                binding.surfaceview.viewTreeObserver.removeOnGlobalLayoutListener(this)

                // Now that the view is fully laid out, set the scale factor
                viewModel.setScaleFactor(binding.surfaceview)
            }
        })
    }

    /**
     * ArCore functions
     **/
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