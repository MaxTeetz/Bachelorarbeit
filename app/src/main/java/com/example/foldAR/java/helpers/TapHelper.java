package com.example.foldAR.java.helpers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import androidx.annotation.NonNull;

import com.example.foldAR.kotlin.constants.Scenarios;
import com.example.foldAR.kotlin.mainActivity.MainActivityViewModel;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Helper to detect taps using Android GestureDetector, and pass the taps between UI thread and
 * render thread.
 */
public final class TapHelper implements OnTouchListener {

    private final MainActivityViewModel viewModel;
    private Scenarios scenario;

    //simple check to see if placement or moving action
    Boolean placement = true; //Todo for not lazy person

  private final GestureDetector gestureDetector;
  private final BlockingQueue<MotionEvent> queuedSingleTaps = new ArrayBlockingQueue<>(16);


  /**
   * Creates the tap helper.
   *
   * @param context the application's context.
   */
  public TapHelper(Context context, MainActivityViewModel viewModel) {
      this.viewModel = viewModel;

    gestureDetector =
        new GestureDetector(
            context,
            new GestureDetector.SimpleOnGestureListener() {
              @Override
              public boolean onSingleTapUp(@NonNull MotionEvent e) {
                // Queue tap if there is space. Tap is lost if queue is full.
                  if(placement){
                      placement = false;
                      queuedSingleTaps.offer(e);
                return true;
              }
                  return false;

              }

              @Override
              public boolean onDown(@NonNull MotionEvent e) {
                return true;
              }
            });
  }

  /**
   * Polls for a tap.
   *
   * @return if a tap was queued, a MotionEvent for the tap. Otherwise null if no taps are queued.
   */
  public MotionEvent poll() {
    return queuedSingleTaps.poll();
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public boolean onTouch(View view, MotionEvent motionEvent) {

      if(this.scenario == Scenarios.STATEOFTHEART)
          viewModel.glSurfaceViewStateOfTheArt(motionEvent, placement, view);
      else
          viewModel.glSurfaceViewFoldAR( motionEvent, placement);

      return gestureDetector.onTouchEvent(motionEvent);
  }

    public void onResume(){
        this.placement = true;
    }

    public void setScenario(Scenarios scenario){
      this.scenario = scenario;
    }

}
