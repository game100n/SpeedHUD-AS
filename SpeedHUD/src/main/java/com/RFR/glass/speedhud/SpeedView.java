package com.RFR.glass.speedhud;

import android.content.Context;
import android.location.Location;
import android.text.TextPaint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.SystemClock;


import java.io.File;
import java.text.NumberFormat;
import java.util.concurrent.TimeUnit;


/**
 * Created by game1_000 on 12/23/13.
 */

public class SpeedView implements SurfaceHolder.Callback {
	
    /** The refresh rate, in frames per second, of the compass. */
    private static final int REFRESH_RATE_FPS = 15;

    /** The duration, in milliseconds, of one frame. */
    private static final long FRAME_TIME_MILLIS = TimeUnit.SECONDS.toMillis(1) / REFRESH_RATE_FPS;
    
	private static final String TAG = "SpeedView";
    private final NumberFormat mSpeedFormat;
    private final FrameLayout mLayout;
    private final TextView mSpeedView;
    private SurfaceHolder mHolder;
    private RenderThread mRenderThread;
    private final OrientationManager mOrientationManager;
    
    private String speedReading = "-.- mph";
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    
    private final TextPaint textPaint;


    private final OrientationManager.OnChangedListener mSpeedListener =
            new OrientationManager.OnChangedListener() {
	    public void onOrientationChanged(OrientationManager orientationManager) {}
	    public void onAccuracyChanged(OrientationManager orientationManager) {}

	    public void onLocationChanged(OrientationManager orientationManager) {
	        Log.d(TAG, "Location Changed");
	        Location location = orientationManager.getLocation();
	        Double currentSpeed = orientationManager.getCurrentSpeed();
	        Log.d(TAG, currentSpeed.toString() + " m/s");
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
		        speedReading = mSpeedFormat.format(CurrentSpeedMPH).toString() + " mph";
	        }
        }
    };
            
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
	   
	   textPaint = new TextPaint();
	   textPaint.setStyle(Paint.Style.FILL);
	   textPaint.setAntiAlias(true);
	   textPaint.setColor(Color.WHITE);
	   textPaint.setTextSize(50);
	   textPaint.setTypeface(Typeface.createFromFile(new File("/system/glass_fonts",
	            "Roboto-Light.ttf")));
   }
            
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    	mSurfaceWidth = width;
        mSurfaceHeight = height;
        doLayout();
    }

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
    
    /**
     * Requests that the views redo their layout. This must be called manually every time the
     * tips view's text is updated because this layout doesn't exist in a GUI thread where those
     * requests will be enqueued automatically.
     */
    private void doLayout() {
        // Measure and update the layout so that it will take up the entire surface space
        // when it is drawn.
        int measuredWidth = View.MeasureSpec.makeMeasureSpec(mSurfaceWidth,
                View.MeasureSpec.EXACTLY);
        int measuredHeight = View.MeasureSpec.makeMeasureSpec(mSurfaceHeight,
                View.MeasureSpec.EXACTLY);

        mLayout.measure(measuredWidth, measuredHeight);
        mLayout.layout(0, 0, mLayout.getMeasuredWidth(), mLayout.getMeasuredHeight());
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
        	doLayout();
        	//textPaint.setColor(Color.WHITE);
            //textPaint.setTextSize(50);
            float centerX = canvas.getWidth() / 2.0f;
            float centerY = canvas.getHeight() / 2.0f;
            Log.d(TAG, speedReading);
            canvas.drawText(speedReading, centerX, centerY, textPaint);
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
