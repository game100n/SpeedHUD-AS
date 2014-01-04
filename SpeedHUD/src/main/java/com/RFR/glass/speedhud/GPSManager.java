package com.RFR.glass.speedhud;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by game1_000 on 12/23/13.
 */
public class GPSManager
{

    /**
     * The minimum distance desired between location notifications.
     * Update data when available
     */
    private static final long METERS_BETWEEN_LOCATIONS = 0;

    /**
     * The minimum elapsed time desired between location notifications.
     * Update data when available
     */
    private static final long MILLIS_BETWEEN_LOCATIONS = TimeUnit.SECONDS.toMillis(0);

    /**
     * The maximum age of a location retrieved from the passive location provider before it is
     * considered too old to use when the compass first starts up.
     */
    private static final long MAX_LOCATION_AGE_MILLIS = TimeUnit.MINUTES.toMillis(30);

    /** Interface implemented to be notified of changes in the user's location. */
    public interface OnChangedListener
    {
        /** Called when the user's orientation changes. */
        void onOrientationChanged(GPSManager gpsManager);

        /** Called when the user's location changes. */
        void onLocationChanged(GPSManager gpsManager);

        /** Called when the accuracy of the compass changes. */
        void onAccuracyChanged(GPSManager gpsManager);
    }

    private final LocationManager mLocationManager;
    private final String mLocationProvider;
    private final Set<OnChangedListener> mListeners;

    private boolean isTracking;
    private Location currentLocation;
    private double currentSpeed;

    /** The location listener used by the GPS manager. */
    private LocationListener mLocationListener = new LocationListener()
    {
        @Override
        public void onLocationChanged(Location location)
        {
            currentLocation = location;
            currentSpeed = location.getSpeed();
            notifyLocationChanged();
        }

        @Override
        public void onProviderDisabled(String provider)
        {
            // Don't need to do anything here.
        }

        @Override
        public void onProviderEnabled(String provider)
        {
            // Don't need to do anything here.
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
            // Don't need to do anything here.
        }
    };

    /**
     * Initializes a new instance of  GPSManager, using the specified context to
     * access system services.
     */
    public GPSManager(LocationManager locationManager)
    {
        mLocationManager = locationManager;
        mListeners = new LinkedHashSet<OnChangedListener>();

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setSpeedRequired(false);

        mLocationProvider = mLocationManager.getBestProvider(criteria, true /* enabledOnly */);
    }

    /** Adds a listener that will be notified when the user's location. */
    public void addOnChangedListener(OnChangedListener listener)
    {
        mListeners.add(listener);
    }

    /** Removes a listener from the list of those that will be notified when the user's location. */
    public void removeOnChangedListener(OnChangedListener listener)
    {
        mListeners.remove(listener);
    }

    /**
     * Starts tracking the user's location. After calling this method, any
     * OnChangedListener's added to this object will be notified of these events.
     */
    public void start()
    {
        if (!isTracking)
        {
            Location lastLocation = mLocationManager
                    .getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            if (lastLocation != null)
            {
                long locationAge = lastLocation.getTime() - System.currentTimeMillis();
                if (locationAge < MAX_LOCATION_AGE_MILLIS)
                {
                    currentLocation = lastLocation;
                }
            }

            if (mLocationProvider != null)
            {
                mLocationManager.requestLocationUpdates(mLocationProvider,
                        MILLIS_BETWEEN_LOCATIONS, METERS_BETWEEN_LOCATIONS, mLocationListener,
                        Looper.getMainLooper());
            }

            isTracking = true;
        }
    }

    /** Stops tracking the user's location. Listeners will no longer be notified of these events. */
    public void stop() {
        if (isTracking) {
            mLocationManager.removeUpdates(mLocationListener);
            isTracking = false;
        }
    }

    /** Gets a value indicating whether the orientation manager knows the user's current location. */
    public boolean hasLocation()
    {
        return currentLocation != null;
    }

    /** Gets the user's current location. */
    public Location getLocation()
    {
        return currentLocation;
    }

    /** Gets the user's current speed */
    public Double getCurrentSpeed()
    {
        return currentSpeed;
    }

    /** Notifies all listeners that the user's orientation has changed. */
    private void notifyOrientationChanged()
    {
        for (OnChangedListener listener : mListeners)
        {
            listener.onOrientationChanged(this);
        }
    }

    /** Notifies all listeners that the user's location has changed. */
    private void notifyLocationChanged()
    {
        for (OnChangedListener listener : mListeners)
        {
            listener.onLocationChanged(this);
        }
    }

    /** Notifies all listeners that the compass's accuracy has changed. */
    private void notifyAccuracyChanged()
    {
        for (OnChangedListener listener : mListeners)
        {
            listener.onAccuracyChanged(this);
        }
    }
}
