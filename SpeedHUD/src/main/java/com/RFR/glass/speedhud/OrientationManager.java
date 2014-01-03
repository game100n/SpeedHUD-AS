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
public class OrientationManager {

    /**
     * The minimum distance desired between location notifications.
     */
    private static final long METERS_BETWEEN_LOCATIONS = 0;

    /**
     * The minimum elapsed time desired between location notifications.
     */
    private static final long MILLIS_BETWEEN_LOCATIONS = TimeUnit.SECONDS.toMillis(0);

    /**
     * The maximum age of a location retrieved from the passive location provider before it is
     * considered too old to use when the compass first starts up.
     */
    private static final long MAX_LOCATION_AGE_MILLIS = TimeUnit.MINUTES.toMillis(30);

    /**
     * Classes should implement this interface if they want to be notified of changes in the user's
     * location or the accuracy.
     */
    public interface OnChangedListener {
        /**
         * Called when the user's orientation changes.
         *
         * @param orientationManager the orientation manager that detected the change
         */
        void onOrientationChanged(OrientationManager orientationManager);

        /**
         * Called when the user's location changes.
         *
         * @param orientationManager the orientation manager that detected the change
         */
        void onLocationChanged(OrientationManager orientationManager);

        /**
         * Called when the accuracy of the compass changes.
         *
         * @param orientationManager the orientation manager that detected the change
         */
        void onAccuracyChanged(OrientationManager orientationManager);
    }

    private final LocationManager mLocationManager;
    private final String mLocationProvider;
    private final Set<OnChangedListener> mListeners;

    private boolean mTracking;
    private Location mLocation;
    private double mSpeed;

    /**
     * The location listener used by the orientation manager.
     */
    private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            mLocation = location;
            mSpeed = location.getSpeed();
            notifyLocationChanged();
        }

        @Override
        public void onProviderDisabled(String provider) {
            // Don't need to do anything here.
        }

        @Override
        public void onProviderEnabled(String provider) {
            // Don't need to do anything here.
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // Don't need to do anything here.
        }
    };

    /**
     * Initializes a new instance of {@code OrientationManager}, using the specified context to
     * access system services.
     */
    public OrientationManager(LocationManager locationManager) {
        mLocationManager = locationManager;
        mListeners = new LinkedHashSet<OnChangedListener>();

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setSpeedRequired(false);

        mLocationProvider = mLocationManager.getBestProvider(criteria, true /* enabledOnly */);
    }

    /**
     * Adds a listener that will be notified when the user's location or orientation changes.
     */
    public void addOnChangedListener(OnChangedListener listener) {
        mListeners.add(listener);
    }

    /**
     * Removes a listener from the list of those that will be notified when the user's location or
     * orientation changes.
     */
    public void removeOnChangedListener(OnChangedListener listener) {
        mListeners.remove(listener);
    }

    /**
     * Starts tracking the user's location. After calling this method, any
     * {@link OnChangedListener}s added to this object will be notified of these events.
     */
    public void start() {
        if (!mTracking) {
            Location lastLocation = mLocationManager
                    .getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            if (lastLocation != null) {
                long locationAge = lastLocation.getTime() - System.currentTimeMillis();
                if (locationAge < MAX_LOCATION_AGE_MILLIS) {
                    mLocation = lastLocation;
                }
            }

            if (mLocationProvider != null) {
                mLocationManager.requestLocationUpdates(mLocationProvider,
                        MILLIS_BETWEEN_LOCATIONS, METERS_BETWEEN_LOCATIONS, mLocationListener,
                        Looper.getMainLooper());
            }

            mTracking = true;
        }
    }

    /**
     * Stops tracking the user's location and orientation. Listeners will no longer be notified of
     * these events.
     */
    public void stop() {
        if (mTracking) {
            mLocationManager.removeUpdates(mLocationListener);
            mTracking = false;
        }
    }

    /**
     * Gets a value indicating whether the orientation manager knows the user's current location.
     *
     * @return true if the user's location is known, otherwise false
     */
    public boolean hasLocation() {
        return mLocation != null;
    }

    /**
     * Gets the user's current location.
     *
     * @return the user's current location
     */
    public Location getLocation() {
        return mLocation;
    }

    /**
     * Gets the user's current speed.
     *
     * @return the user's current speed
     */
    public Double getCurrentSpeed() {
        return mSpeed;
    }

    /**
     * Notifies all listeners that the user's orientation has changed.
     */
    private void notifyOrientationChanged() {
        for (OnChangedListener listener : mListeners) {
            listener.onOrientationChanged(this);
        }
    }

    /**
     * Notifies all listeners that the user's location has changed.
     */
    private void notifyLocationChanged() {
        for (OnChangedListener listener : mListeners) {
            listener.onLocationChanged(this);
        }
    }

    /**
     * Notifies all listeners that the compass's accuracy has changed.
     */
    private void notifyAccuracyChanged() {
        for (OnChangedListener listener : mListeners) {
            listener.onAccuracyChanged(this);
        }
    }
}
