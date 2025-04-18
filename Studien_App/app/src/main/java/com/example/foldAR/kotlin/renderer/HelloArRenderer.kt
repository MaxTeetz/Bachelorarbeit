package com.example.foldAR.kotlin.renderer

import android.opengl.GLES30
import android.opengl.Matrix
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.example.foldAR.java.helpers.DisplayRotationHelper
import com.example.foldAR.java.helpers.TrackingStateHelper
import com.example.foldAR.java.samplerender.Framebuffer
import com.example.foldAR.java.samplerender.GLError
import com.example.foldAR.java.samplerender.Mesh
import com.example.foldAR.java.samplerender.SampleRender
import com.example.foldAR.java.samplerender.Shader
import com.example.foldAR.java.samplerender.Texture
import com.example.foldAR.java.samplerender.VertexBuffer
import com.example.foldAR.java.samplerender.arcore.BackgroundRenderer
import com.example.foldAR.java.samplerender.arcore.PlaneRenderer
import com.example.foldAR.java.samplerender.arcore.SpecularCubemapFilter
import com.example.foldAR.kotlin.constants.Constants
import com.example.foldAR.kotlin.helloar.R
import com.example.foldAR.kotlin.mainActivity.MainActivity
import com.google.ar.core.Anchor
import com.google.ar.core.Camera
import com.google.ar.core.DepthPoint
import com.google.ar.core.Frame
import com.google.ar.core.InstantPlacementPoint
import com.google.ar.core.LightEstimate
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.Trackable
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.NotYetAvailableException
import java.io.IOException
import java.nio.ByteBuffer
import java.util.Collections
import java.util.UUID
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

/** Renders the HelloAR application using our example Renderer. */
class HelloArRenderer(val activity: MainActivity) : SampleRender.Renderer,
    DefaultLifecycleObserver {
    companion object {
        val TAG = "HelloArRenderer"

        // See the definition of updateSphericalHarmonicsCoefficients for an explanation of these
        // constants.
        private val sphericalHarmonicFactors = floatArrayOf(
            0.282095f,
            -0.325735f,
            0.325735f,
            -0.325735f,
            0.273137f,
            -0.273137f,
            0.078848f,
            -0.273137f,
            0.136569f
        )

        private val Z_NEAR = 0.1f
        private val Z_FAR = 100f

        // Assumed distance from the device camera to the surface on which user will try to place
        // objects.
        // This value affects the apparent scale of objects while the tracking method of the
        // Instant Placement point is SCREENSPACE_WITH_APPROXIMATE_DISTANCE.
        // Values in the [0.2, 2.0] meter range are a good choice for most AR experiences. Use lower
        // values for AR experiences where users are expected to place objects on surfaces close to the
        // camera. Use larger values for experiences where the user will likely be standing and trying
        // to
        // place an object on the ground or floor in front of them.
        val APPROXIMATE_DISTANCE_METERS = 1.0f

        val CUBEMAP_RESOLUTION = 16
        val CUBEMAP_NUMBER_OF_IMPORTANCE_SAMPLES = 32
    }

    var camera: MutableLiveData<Camera> = MutableLiveData<Camera>()

    lateinit var render: SampleRender
    lateinit var planeRenderer: PlaneRenderer
    lateinit var backgroundRenderer: BackgroundRenderer
    lateinit var virtualSceneFramebuffer: Framebuffer
    var hasSetTextureNames = false

    // Point Cloud
    lateinit var pointCloudVertexBuffer: VertexBuffer
    lateinit var pointCloudMesh: Mesh
    lateinit var pointCloudShader: Shader

    // Keep track of the last point cloud rendered to avoid updating the VBO if point cloud
    // was not changed.  Do this using the timestamp since we can't compare PointCloud objects.
    var lastPointCloudTimestamp: Long = 0

    // Virtual object (ARCore pawn)
    private lateinit var virtualObjectMesh: Mesh
    private lateinit var virtualObjectShader: Shader
    private lateinit var virtualObjectAlbedoTexture: Texture
    private lateinit var virtualObjectAlbedoTextureAlt: Texture
    private lateinit var virtualObjectAlbedoTextureFinish: Texture
    private lateinit var virtualObjectAlbedoInstantPlacementTexture: Texture

    lateinit var secondAnchor: WrappedAnchor

    val wrappedAnchors = Collections.synchronizedList(mutableListOf<WrappedAnchor>())
    private lateinit var idFirstAnchor: UUID

    private var _reached: MutableLiveData<Boolean> = MutableLiveData(false)
    val reached get() = _reached

    private var done = true

    private var _distance: Float = 10f
    val distance get() = _distance

    private var _timeDone: Long = 0
    val timeDone get() = _timeDone

    private var _distanceDone = 0f
    val distanceDone get() = _distanceDone

    // Environmental HDR
    lateinit var dfgTexture: Texture
    lateinit var cubemapFilter: SpecularCubemapFilter

    // Temporary matrix allocated here to reduce number of allocations for each frame.
    val modelMatrix = FloatArray(16)
    val viewMatrix = FloatArray(16)
    val projectionMatrix = FloatArray(16)
    val modelViewMatrix = FloatArray(16) // view x model

    val modelViewProjectionMatrix = FloatArray(16) // projection x view x model

    val sphericalHarmonicsCoefficients = FloatArray(9 * 3)
    val viewInverseMatrix = FloatArray(16)
    val worldLightDirection = floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f)
    val viewLightDirection = FloatArray(4) // view x world light direction

    val session
        get() = activity.arCoreSessionHelper.session

    val displayRotationHelper = DisplayRotationHelper(activity)
    val trackingStateHelper = TrackingStateHelper(activity)

    override fun onResume(owner: LifecycleOwner) {
        displayRotationHelper.onResume()
        hasSetTextureNames = false
    }

    override fun onPause(owner: LifecycleOwner) {
        displayRotationHelper.onPause()
    }

    override fun onSurfaceCreated(render: SampleRender) {

        // Prepare the rendering objects.
        // This involves reading shaders and 3D model files, so may throw an IOException.
        try {
            planeRenderer = PlaneRenderer(render)
            backgroundRenderer = BackgroundRenderer(render)
            virtualSceneFramebuffer = Framebuffer(render, /*width=*/ 1, /*height=*/ 1)

            cubemapFilter = SpecularCubemapFilter(
                render, CUBEMAP_RESOLUTION, CUBEMAP_NUMBER_OF_IMPORTANCE_SAMPLES
            )
            // Load environmental lighting values lookup table
            dfgTexture = Texture(
                render, Texture.Target.TEXTURE_2D, Texture.WrapMode.CLAMP_TO_EDGE,/*useMipmaps=*/
                false
            )
            // The dfg.raw file is a raw half-float texture with two channels.
            val dfgResolution = 64
            val dfgChannels = 2
            val halfFloatSize = 2

            val buffer: ByteBuffer =
                ByteBuffer.allocateDirect(dfgResolution * dfgResolution * dfgChannels * halfFloatSize)
            activity.assets.open("models/dfg.raw").use { it.read(buffer.array()) }

            // SampleRender abstraction leaks here.
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, dfgTexture.textureId)
            GLError.maybeThrowGLException("Failed to bind DFG texture", "glBindTexture")
            GLES30.glTexImage2D(
                GLES30.GL_TEXTURE_2D,/*level=*/
                0, GLES30.GL_RG16F,/*width=*/
                dfgResolution,/*height=*/
                dfgResolution,/*border=*/
                0, GLES30.GL_RG, GLES30.GL_HALF_FLOAT, buffer
            )
            GLError.maybeThrowGLException("Failed to populate DFG texture", "glTexImage2D")

            // Point cloud
            pointCloudShader = Shader.createFromAssets(
                render, "shaders/point_cloud.vert", "shaders/point_cloud.frag",/*defines=*/ null
            ).setVec4(
                "u_Color", floatArrayOf(31.0f / 255.0f, 188.0f / 255.0f, 210.0f / 255.0f, 1.0f)
            ).setFloat("u_PointSize", 5.0f)

            // four entries per vertex: X, Y, Z, confidence
            pointCloudVertexBuffer =
                VertexBuffer(render, /*numberOfEntriesPerVertex=*/ 4, /*entries=*/ null)
            val pointCloudVertexBuffers = arrayOf(pointCloudVertexBuffer)
            pointCloudMesh = Mesh(
                render, Mesh.PrimitiveMode.POINTS, /*indexBuffer=*/
                null, pointCloudVertexBuffers
            )

            // Virtual object to render (ARCore pawn)
            virtualObjectAlbedoTexture = Texture.createFromAsset(
                render,
                "models/cube/manipulated_cube_albedo.png",
                Texture.WrapMode.CLAMP_TO_EDGE,
                Texture.ColorFormat.SRGB
            )

            virtualObjectAlbedoTextureAlt = Texture.createFromAsset(
                render,
                "models/cube/target_cube_albedo.png",
                Texture.WrapMode.CLAMP_TO_EDGE,
                Texture.ColorFormat.SRGB
            )

            virtualObjectAlbedoTextureFinish = Texture.createFromAsset(
                render,
                "models/cube/target_cube_finish_albedo.png",
                Texture.WrapMode.CLAMP_TO_EDGE,
                Texture.ColorFormat.SRGB
            )

            virtualObjectAlbedoInstantPlacementTexture = Texture.createFromAsset(
                render,
                "models/cube/manipulated_cube_transparent_albedo.png",
                Texture.WrapMode.CLAMP_TO_EDGE,
                Texture.ColorFormat.SRGB
            )

            val virtualObjectPbrTexture = Texture.createFromAsset(
                render,
                "models/cube/cube_metallic_alt_albedo.png",
                Texture.WrapMode.CLAMP_TO_EDGE,
                Texture.ColorFormat.LINEAR
            )
            virtualObjectMesh =
                Mesh.createFromAsset(render, "models/cube/manipulated_cube_transparent.obj")
            virtualObjectShader = Shader.createFromAssets(
                render,
                "shaders/environmental_hdr.vert",
                "shaders/environmental_hdr.frag",
                mapOf("NUMBER_OF_MIPMAP_LEVELS" to cubemapFilter.numberOfMipmapLevels.toString())
            ).setTexture("u_AlbedoTexture", virtualObjectAlbedoTexture).setTexture(
                "u_RoughnessMetallicAmbientOcclusionTexture", virtualObjectPbrTexture
            ).setTexture("u_Cubemap", cubemapFilter.filteredCubemapTexture)
                .setTexture("u_DfgTexture", dfgTexture)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to read a required asset file", e)
            showError("Failed to read a required asset file: $e")
        }
    }

    override fun onSurfaceChanged(render: SampleRender, width: Int, height: Int) {
        displayRotationHelper.onSurfaceChanged(width, height)
        virtualSceneFramebuffer.resize(width, height)
    }

    override fun onDrawFrame(render: SampleRender) {
        val session = session ?: return

        // Texture names should only be set once on a GL thread unless they change. This is done during
        // onDrawFrame rather than onSurfaceCreated since the session is not guaranteed to have been
        // initialized during the execution of onSurfaceCreated.
        if (!hasSetTextureNames) {
            session.setCameraTextureNames(intArrayOf(backgroundRenderer.cameraColorTexture.textureId))
            hasSetTextureNames = true
        }

        // -- Update per-frame state

        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        displayRotationHelper.updateSessionIfNeeded(session)

        // Obtain the current frame from ARSession. When the configuration is set to
        // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
        // camera framerate.
        val frame = try {
            session.update()
        } catch (e: CameraNotAvailableException) {
            Log.e(TAG, "Camera not available during onDrawFrame", e)
            showError("Camera not available. Try restarting the app.")
            return
        }

        val camera = frame.camera

        this.camera.postValue(camera)

        // Update BackgroundRenderer state to match the depth settings.
        try {
            backgroundRenderer.setUseDepthVisualization(
                render, activity.depthSettings.depthColorVisualizationEnabled()
            )
            backgroundRenderer.setUseOcclusion(
                render, activity.depthSettings.useDepthForOcclusion()
            )
        } catch (e: IOException) {
            Log.e(TAG, "Failed to read a required asset file", e)
            showError("Failed to read a required asset file: $e")
            return
        }

        // BackgroundRenderer.updateDisplayGeometry must be called every frame to update the coordinates
        // used to draw the background camera image.
        backgroundRenderer.updateDisplayGeometry(frame)
        val shouldGetDepthImage =
            activity.depthSettings.useDepthForOcclusion() || activity.depthSettings.depthColorVisualizationEnabled()
        if (camera.trackingState == TrackingState.TRACKING && shouldGetDepthImage) {
            try {
                val depthImage = frame.acquireDepthImage16Bits()
                backgroundRenderer.updateCameraDepthTexture(depthImage)
                depthImage.close()
            } catch (e: NotYetAvailableException) {
                // This normally means that depth data is not available yet. This is normal so we will not
                // spam the logcat with this.
            }
        }

        // Handle one tap per frame.
        handleTap(frame, camera)

        // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
        trackingStateHelper.updateKeepScreenOnFlag(camera.trackingState)

        // Show a message based on whether tracking has failed, if planes are detected, and if the user
        // has placed any objects.
        val message: String? = when {
            camera.trackingState == TrackingState.PAUSED && camera.trackingFailureReason == TrackingFailureReason.NONE -> activity.getString(
                R.string.searching_planes
            )

            camera.trackingState == TrackingState.PAUSED -> TrackingStateHelper.getTrackingFailureReasonString(
                camera
            )

            session.hasTrackingPlane() && wrappedAnchors.isEmpty() -> activity.getString(R.string.waiting_taps)

            session.hasTrackingPlane() && wrappedAnchors.isNotEmpty() -> null
            else -> activity.getString(R.string.searching_planes)
        }

        // -- Draw background
        if (frame.timestamp != 0L) {
            // Suppress rendering if the camera did not produce the first frame yet. This is to avoid
            // drawing possible leftover data from previous sessions if the texture is reused.
            backgroundRenderer.drawBackground(render)
        }

        // If not tracking, don't draw 3D objects.
        if (camera.trackingState == TrackingState.PAUSED) {
            return
        }

        // -- Draw non-occluded virtual objects (planes, point cloud)

        // Get projection matrix.
        camera.getProjectionMatrix(projectionMatrix, 0, Z_NEAR, Z_FAR)

        // Get camera matrix and draw.
        camera.getViewMatrix(viewMatrix, 0)
        frame.acquirePointCloud().use { pointCloud ->
            if (pointCloud.timestamp > lastPointCloudTimestamp) {
                pointCloudVertexBuffer.set(pointCloud.points)
                lastPointCloudTimestamp = pointCloud.timestamp
            }
            Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
            pointCloudShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix)
            render.draw(pointCloudMesh, pointCloudShader)
        }

        // Visualize planes.
//        planeRenderer.drawPlanes(
//            render,
//            session.getAllTrackables<Plane>(Plane::class.java),
//            camera.displayOrientedPose,
//            projectionMatrix
//        )

        // -- Draw occluded virtual objects

        // Update lighting parameters in the shader
        updateLightEstimation(frame.lightEstimate, viewMatrix)

        // Visualize anchors created by touch.
        render.clear(virtualSceneFramebuffer, 0f, 0f, 0f, 0f)
        wrappedAnchors.filter { (anchor, _) ->
            anchor.trackingState == TrackingState.TRACKING
        }.forEach { (anchor, trackable, id) ->
            // Get the current pose of an Anchor in world space. The Anchor pose is updated
            // during calls to session.update() as ARCore refines its estimate of the world.
            anchor.pose.toMatrix(modelMatrix, 0)

            // Calculate model/view/projection matrices
            Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)

            // Update shader properties and draw
            virtualObjectShader.setMat4("u_ModelView", modelViewMatrix)
            virtualObjectShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix)
            Log.d(TAG, "TextureTest")
            //second anchor is manipulated one
            val texture =
                if ((trackable as? InstantPlacementPoint)?.trackingMethod == InstantPlacementPoint.TrackingMethod.SCREENSPACE_WITH_APPROXIMATE_DISTANCE) {
                    virtualObjectAlbedoInstantPlacementTexture
                } else {
                    if (id == idFirstAnchor) {
                        virtualObjectAlbedoTexture
                    } else {
                        if (done)
                            virtualObjectAlbedoTextureAlt
                        else
                            virtualObjectAlbedoTextureFinish
                    }
                }
            virtualObjectShader.setTexture("u_AlbedoTexture", texture)
            render.draw(virtualObjectMesh, virtualObjectShader, virtualSceneFramebuffer)
        }
        // Compose the virtual scene with the background.
        backgroundRenderer.drawVirtualScene(render, virtualSceneFramebuffer, Z_NEAR, Z_FAR)
    }

    /** Checks if we detected at least one plane. */
    private fun Session.hasTrackingPlane() =
        getAllTrackables(Plane::class.java).any { it.trackingState == TrackingState.TRACKING }

    /** Update state based on the current frame's light estimation. */
    private fun updateLightEstimation(lightEstimate: LightEstimate, viewMatrix: FloatArray) {
        if (lightEstimate.state != LightEstimate.State.VALID) {
            virtualObjectShader.setBool("u_LightEstimateIsValid", false)
            return
        }
        virtualObjectShader.setBool("u_LightEstimateIsValid", true)
        Matrix.invertM(viewInverseMatrix, 0, viewMatrix, 0)
        virtualObjectShader.setMat4("u_ViewInverse", viewInverseMatrix)
        updateMainLight(
            lightEstimate.environmentalHdrMainLightDirection,
            lightEstimate.environmentalHdrMainLightIntensity,
            viewMatrix
        )
        updateSphericalHarmonicsCoefficients(lightEstimate.environmentalHdrAmbientSphericalHarmonics)
        cubemapFilter.update(lightEstimate.acquireEnvironmentalHdrCubeMap())
    }

    private fun updateMainLight(
        direction: FloatArray,
        intensity: FloatArray,
        viewMatrix: FloatArray,
    ) {
        // We need the direction in a vec4 with 0.0 as the final component to transform it to view space
        worldLightDirection[0] = direction[0]
        worldLightDirection[1] = direction[1]
        worldLightDirection[2] = direction[2]
        Matrix.multiplyMV(viewLightDirection, 0, viewMatrix, 0, worldLightDirection, 0)
        virtualObjectShader.setVec4("u_ViewLightDirection", viewLightDirection)
        virtualObjectShader.setVec3("u_LightIntensity", intensity)
    }

    private fun updateSphericalHarmonicsCoefficients(coefficients: FloatArray) {
        // Pre-multiply the spherical harmonics coefficients before passing them to the shader. The
        // constants in sphericalHarmonicFactors were derived from three terms:
        //
        // 1. The normalized spherical harmonics basis functions (y_lm)
        //
        // 2. The lambertian diffuse BRDF factor (1/pi)
        //
        // 3. A <cos> convolution. This is done to so that the resulting function outputs the irradiance
        // of all incoming light over a hemisphere for a given surface normal, which is what the shader
        // (environmental_hdr.frag) expects.
        //
        // You can read more details about the math here:
        // https://google.github.io/filament/Filament.html#annex/sphericalharmonics
        require(coefficients.size == 9 * 3) {
            "The given coefficients array must be of length 27 (3 components per 9 coefficients"
        }

        // Apply each factor to every component of each coefficient
        for (i in 0 until 9 * 3) {
            sphericalHarmonicsCoefficients[i] = coefficients[i] * sphericalHarmonicFactors[i / 3]
        }
        virtualObjectShader.setVec3Array(
            "u_SphericalHarmonicsCoefficients", sphericalHarmonicsCoefficients
        )
    }

    // Handle only one tap per frame, as taps are usually low frequency compared to frame rate.
    private fun handleTap(frame: Frame, camera: Camera) {
        if (camera.trackingState != TrackingState.TRACKING) return
        val tap = activity.tapHelper.poll() ?: return

        val hitResultList = if (activity.instantPlacementSettings.isInstantPlacementEnabled) {
            frame.hitTestInstantPlacement(tap.x, tap.y, APPROXIMATE_DISTANCE_METERS)
        } else {
            frame.hitTest(tap)
        }

        // Hits are sorted by depth. Consider only closest hit on a plane, Oriented Point, Depth Point,
        // or Instant Placement Point.
        val firstHitResult = hitResultList.firstOrNull { hit ->
            when (val trackable = hit.trackable!!) {
                is Plane -> trackable.isPoseInPolygon(hit.hitPose) && PlaneRenderer.calculateDistanceToPlane(
                    hit.hitPose, camera.pose
                ) > 0

                is Point -> trackable.orientationMode == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL
                is InstantPlacementPoint -> true
                // DepthPoints are only returned if Config.DepthMode is set to AUTOMATIC.
                is DepthPoint -> true
                else -> false
            }
        }

        if (firstHitResult != null && wrappedAnchors.size <= Constants.OBJECTS_MAX_SIZE) {
            // Cap the number of objects created. This avoids overloading both the
            // rendering system and ARCore.
            // Adding an Anchor tells ARCore that it should track this position in
            // space. This anchor is created on the Plane to place the 3D model
            // in the correct position relative both to the world and to the plane.
            val anchor = WrappedAnchor(firstHitResult.createAnchor(), firstHitResult.trackable)
            secondAnchor = WrappedAnchor(firstHitResult.createAnchor(), firstHitResult.trackable)

            wrappedAnchors.add(anchor)
            if (wrappedAnchors.size == 1)
                idFirstAnchor = anchor.id

            // For devices that support the Depth API, shows a dialog to suggest enabling
            // depth-based occlusion. This dialog needs to be spawned on the UI thread.
            activity.runOnUiThread { activity.showOcclusionDialogIfNeeded() }
        }
    }

    fun deleteAnchor() = wrappedAnchors.clear()

    private fun moveAnchor(moveX: Float, moveY: Float, moveZ: Float, position: Int) {
        wrappedAnchors.takeIf { it.isNotEmpty() }?.let {

            val rotationQuaternion = wrappedAnchors[position].anchor.pose.rotationQuaternion
            val id = wrappedAnchors[position].id
            //combine new position and rotation
            val translation = Pose.makeTranslation(moveX, moveY, moveZ)
            val rotation = Pose.makeRotation(
                0f,
                rotationQuaternion[1],
                0f,
                rotationQuaternion[3]
            ) //just to make it look normal
            val newPose = translation.compose(rotation)

            wrappedAnchors[position].anchor.detach() //Todo error if no valid session camera obstructed or something like that
            //add to list

            try {
                session?.let {
                    val newAnchor =
                        WrappedAnchor(
                            session!!.createAnchor(newPose),
                            wrappedAnchors[position].trackable,
                            id
                        )

                    wrappedAnchors[position] = newAnchor

                } ?: Log.e("MoveAnchor", "Session is null or invalid")
            } catch (e: Exception) {
                Log.e("MoveAnchor", "Failed to create new Anchor", e)
            }

            if (wrappedAnchors.size >= 2)
                calculateDistance()

            if (this.distance < 0.1 && done) {
                _timeDone = System.currentTimeMillis()
                _distanceDone = distance
                done = false
            }
        }
    }

    private fun calculateDistance() {
        val a = wrappedAnchors[0].anchor.pose
        val b = wrappedAnchors[1].anchor.pose

        this._distance =
            sqrt((b.tx() - a.tx()).pow(2) + (b.ty() - a.ty()).pow(2) + (b.tz() - a.tz()).pow(2))
    }

    fun reachedTrue() {
        _reached.value = true
        done = true
    }

    fun resetReached() {
        this._reached.value = false
    }

    fun moveAnchorPlane(moveX: Float, moveZ: Float, position: Int) {

        moveAnchor(
            moveX,
            wrappedAnchors[position].anchor.pose.ty(),
            moveZ,
            position
        )
    }

    fun moveAnchorHeight(moveY: Float, position: Int) {
        moveAnchor(
            wrappedAnchors[position].anchor.pose.tx(),
            moveY,
            wrappedAnchors[position].anchor.pose.tz(),
            position
        )
    }

    // Only use the yaw for the 2D rotation
    fun refreshAngle(): Float {
        val (w, x, y, z) = camera.value!!.pose.rotationQuaternion
        val sinyCosp = 2 * (w * z + x * y)
        val cosyCosp = 1 - 2 * (y * y + z * z)
        val yaw = atan2(sinyCosp, cosyCosp)
        val deg = (Math.toDegrees(yaw.toDouble()) % 360).toFloat()

        return (deg / 180 * PI).toFloat()
    }

    private fun showError(errorMessage: String) =
        activity.snackbarHelper.showError(activity, errorMessage)

}

/**
 * Associates an Anchor with the trackable it was attached to. This is used to be able to check
 * whether or not an Anchor originally was attached to an {@link InstantPlacementPoint}.
 */
data class WrappedAnchor(
    val anchor: Anchor,
    val trackable: Trackable,
    val id: UUID = UUID.randomUUID(),
)
