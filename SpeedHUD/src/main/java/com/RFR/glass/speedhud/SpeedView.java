package com.RFR.glass.speedhud;

import android.content.Context;
import android.location.Location;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.graphics.Canvas;
import android.os.SystemClock;


import java.text.NumberFormat;
import java.util.concurrent.TimeUnit;


/**
 * Created by game1_000 on 12/23/13.
 */

public class SpeedView implements SurfaceHolder.Callback {
	
    /** The refresh rate, in frames per second, of the compass. */
    private static final int REFRESH_RATE_FPS = 45;

    /** The duration, in milliseconds, of one frame. */
    private static final long FRAME_TIME_MILLIS = TimeUnit.SECONDS.toMillis(1) / REFRESH_RATE_FPS;
    
	private static final String TAG = "SpeedView";
    private final NumberFormat mSpeedFormat;
    private final FrameLayout mLayout;
    private final TextView mSpeedView;
    private SurfaceHolder mHolder;
    private RenderThread mRenderThread;
    private final OrientationManager mOrientationManager;

    /**
     * Creates a new instance of the {@code SpeedView} with the specified context and
     * orientation manager.
     */
    public SpeedView(Context context, OrientationManager orientationManager) {
    	Log.d(TAG, "Create Instance SpeedView");
        LayoutInflater inflater = LayoutInflater.from(context);
        mLayout = (FrameLayout) inflater.inflate(R.layout.activity_speed_view, null);
        mLayout.setWillNotDraw(false);
        mSpeedView = (TextView) mLayout.findViewById(R.id.speedText);
        mSpeedFormat = NumberFormat.getNumberInstance();
        mSpeedFormat.setMinimumFractionDigits(0);
        mSpeedFormat.setMaximumFractionDigits(1);
        mOrientationManager = orientationManager;
    }

    private final OrientationManager.OnChangedListener mSpeedListener =
            new OrientationManager.OnChangedListener() {
                public void onOrientationChanged(OrientationManager orientationManager) {}
                public void onAccuracyChanged(OrientationManager orientationManager) {}

                public void onLocationChanged(OrientationManager orientationManager) {
                	Log.d(TAG, "Location Changed");
                    Location location = orientationManager.getLocation();
                    Double currentSpeed = orientationManager.getCurrentSpeed();
                    if(location == null)
                    {
                    	Log.d(TAG, "Default View Value");
                        mSpeedView.setText("-.- mph");
                    }
                    else
                    {
                    	Log.d(TAG, "Speed Updated");
                        double CurrentSpeedMPH = currentSpeed*2.23694;
                        mSpeedView.setText(mSpeedFormat.format(CurrentSpeedMPH) + " mph");
                    }
                }
            };
            
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "Surface created");
        
        mHolder = holder;
        
        mOrientationManager.addOnChangedListener(mSpeedListener);
        mOrientationManager.start();
        
        Log.d(TAG, "New Render Thread");
        mRenderThread = new RenderThread();
        mRenderThread.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    	Log.d(TAG, "Surface Destroyed");
    	
        mRenderThread.quit();

        mOrientationManager.removeOnChangedListener(mSpeedListener);
        mOrientationManager.stop();
    }

    private synchronized void repaint() {
        Canvas canvas = null;

        try {
            canvas = mHolder.lockCanvas();
        } catch (RuntimeException e) {
            Log.d(TAG, "lockCanvas failed", e);
            return;
        }

        if (canvas != null) {
            mLayout.draw(canvas);

            try {
                mHolder.unlockCanvasAndPost(canvas);
            } catch (RuntimeException e) {
                Log.d(TAG, "unlockCanvasAndPost failed", e);
            }
        }
    }

    /**
     * Redraws the View in the background.
     */
    private class RenderThread extends Thread {
        private boolean mShouldRun;

        /**
         * Initializes the background rendering thread.
         */
        public RenderThread() {
            mShouldRun = true;
        }

        /**
         * Returns true if the rendering thread should continue to run.
         *
         * @return true if the rendering thread should continue to run
         */
        private synchronized boolean shouldRun() {
            return mShouldRun;
        }

        /**
         * Requests that the rendering thread exit at the next opportunity.
         */
        public synchronized void quit() {
            mShouldRun = false;
        }

        @Override
        public void run() {
            while (shouldRun()) {
                long frameStart = SystemClock.elapsedRealtime();
                repaint();
                long frameLength = SystemClock.elapsedRealtime() - frameStart;

                long sleepTime = FRAME_TIME_MILLIS - frameLength;
                if (sleepTime > 0) {
                    SystemClock.sleep(sleepTime);
                }
            }
        }
    }
}
