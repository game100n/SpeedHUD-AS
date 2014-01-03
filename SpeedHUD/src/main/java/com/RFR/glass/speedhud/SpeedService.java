package com.RFR.glass.speedhud;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;
import com.google.android.glass.timeline.TimelineManager;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.content.Context;
import android.location.LocationManager;

/**
 * Created by game1_000 on 12/23/13.
 */

public class SpeedService extends Service {

    private static final String TAG = "SpeedService";
    private static final String LIVE_CARD_ID = "SpeedHUD";

    private SpeedView mCallback;

    private TimelineManager mTimelineManager;
    private LiveCard mLiveCard;

    private OrientationManager mOrientationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        
        mTimelineManager = TimelineManager.from(this);

        LocationManager locationManager =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        mOrientationManager = new OrientationManager(locationManager);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if (mLiveCard == null) {
            Log.d(TAG, "Publishing LiveCard");
            mLiveCard = mTimelineManager.createLiveCard(LIVE_CARD_ID);

            // Keep track of the callback to remove it before unpublishing.
            mCallback = new SpeedView(this, mOrientationManager);
            mLiveCard.setDirectRenderingEnabled(true).getSurfaceHolder().addCallback(mCallback);

            // Display the options menu when the live card is tapped.
            Intent menuIntent = new Intent(this, MenuActivity.class);
            menuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mLiveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0));
            
            // Jump to the LiveCard when API is available
            mLiveCard.publish(PublishMode.REVEAL);
            Log.d(TAG, "Done publishing LiveCard");
        }
        else
        {
            // TODO(alainv): Jump to the LiveCard when API is available.
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mLiveCard != null && mLiveCard.isPublished()) {
            Log.d(TAG, "Unpublishing LiveCard");
            mLiveCard.getSurfaceHolder().removeCallback(mCallback);
            mLiveCard.unpublish();
            mLiveCard = null;
        }
        
        mOrientationManager = null;

        super.onDestroy();
    }
}
