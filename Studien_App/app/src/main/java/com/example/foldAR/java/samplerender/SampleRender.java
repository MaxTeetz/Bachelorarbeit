package com.example.foldAR.java.samplerender;

import android.content.res.AssetManager;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/** A SampleRender context. */
public class SampleRender {
  private static final String TAG = SampleRender.class.getSimpleName();

  private final AssetManager assetManager;

  private int viewportWidth = 1;
  private int viewportHeight = 1;

  /**
   * Constructs a SampleRender object and instantiates GLSurfaceView parameters.
   *
   * @param glSurfaceView Android GLSurfaceView
   * @param renderer Renderer implementation to receive callbacks
   * @param assetManager AssetManager for loading Android resources
   */
  public SampleRender(GLSurfaceView glSurfaceView, Renderer renderer, AssetManager assetManager) {
    this.assetManager = assetManager;
    glSurfaceView.setPreserveEGLContextOnPause(true);
    glSurfaceView.setEGLContextClientVersion(3);
    glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
    glSurfaceView.setRenderer(
        new GLSurfaceView.Renderer() {
          @Override
          public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            Log.d("RendererInputFileCheck", "Renderer");

            GLES30.glEnable(GLES30.GL_BLEND);
            GLError.maybeThrowGLException("Failed to enable blending", "glEnable");
            renderer.onSurfaceCreated(SampleRender.this);
          }

          @Override
          public void onSurfaceChanged(GL10 gl, int w, int h) {
            viewportWidth = w;
            viewportHeight = h;
            renderer.onSurfaceChanged(SampleRender.this, w, h);
          }

          @Override
          public void onDrawFrame(GL10 gl) {
            clear(/*framebuffer=*/ null, 0f, 0f, 0f, 1f);
            renderer.onDrawFrame(SampleRender.this);
          }
        });
    glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    glSurfaceView.setWillNotDraw(false);
  }

  /** Draw a {@link Mesh} with the specified {@link Shader}. */
  public void draw(Mesh mesh, Shader shader) {
    draw(mesh, shader, /*framebuffer=*/ null);
  }

  /**
   * Draw a {@link Mesh} with the specified {@link Shader} to the given {@link Framebuffer}.
   *
   * <p>The {@code framebuffer} argument may be null, in which case the default framebuffer is used.
   */
  public void draw(Mesh mesh, Shader shader, Framebuffer framebuffer) {
    useFramebuffer(framebuffer);
    shader.lowLevelUse();
    mesh.lowLevelDraw();
  }

  /**
   * Clear the given framebuffer.
   *
   * <p>The {@code framebuffer} argument may be null, in which case the default framebuffer is
   * cleared.
   */
  public void clear(Framebuffer framebuffer, float r, float g, float b, float a) {
    useFramebuffer(framebuffer);
    GLES30.glClearColor(r, g, b, a);
    GLError.maybeThrowGLException("Failed to set clear color", "glClearColor");
    GLES30.glDepthMask(true);
    GLError.maybeThrowGLException("Failed to set depth write mask", "glDepthMask");
    GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
    GLError.maybeThrowGLException("Failed to clear framebuffer", "glClear");
  }

  /** Interface to be implemented for rendering callbacks. */
  public static interface Renderer {
    /**
     * Called by {@link SampleRender} when the GL render surface is created.
     *
     * <p>See {@link GLSurfaceView.Renderer#onSurfaceCreated}.
     */
    public void onSurfaceCreated(SampleRender render);

    /**
     * Called by {@link SampleRender} when the GL render surface dimensions are changed.
     *
     * <p>See {@link GLSurfaceView.Renderer#onSurfaceChanged}.
     */
    public void onSurfaceChanged(SampleRender render, int width, int height);

    /**
     * Called by {@link SampleRender} when a GL frame is to be rendered.
     *
     * <p>See {@link GLSurfaceView.Renderer#onDrawFrame}.
     */
    public void onDrawFrame(SampleRender render);
  }

  /* package-private */
  AssetManager getAssets() {
    return assetManager;
  }

  private void useFramebuffer(Framebuffer framebuffer) {
    int framebufferId;
    int viewportWidth;
    int viewportHeight;
    if (framebuffer == null) {
      framebufferId = 0;
      viewportWidth = this.viewportWidth;
      viewportHeight = this.viewportHeight;
    } else {
      framebufferId = framebuffer.getFramebufferId();
      viewportWidth = framebuffer.getWidth();
      viewportHeight = framebuffer.getHeight();
    }
    GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, framebufferId);
    GLError.maybeThrowGLException("Failed to bind framebuffer", "glBindFramebuffer");
    GLES30.glViewport(0, 0, viewportWidth, viewportHeight);
    GLError.maybeThrowGLException("Failed to set viewport dimensions", "glViewport");
  }
}
