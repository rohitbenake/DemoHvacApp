package com.clarion.demohvacapp;

import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseLongArray;

import androidx.annotation.GuardedBy;

public class DataStore {
    private static final long COALESCE_TIME_MS = 0L;

    @GuardedBy("mTemperature")
    private SparseArray<Float> mTemperature = new SparseArray<Float>();
    @GuardedBy("mTemperatureAvailable")
    private SparseBooleanArray mTemperatureAvailable = new SparseBooleanArray();
    @GuardedBy("mTemperature")
    private SparseLongArray mLastTemperatureSet = new SparseLongArray();

    public float getTemperature(int zone) {
        synchronized (mTemperature) {
            return mTemperature.get(zone);
        }
    }

    public void setTemperature(int zone, float temperature, boolean available) {
        synchronized (mTemperature) {
            synchronized (mTemperatureAvailable) {
                Log.d("HvacDataStore", "setTemperature(" + zone + ", " + temperature + ")");
                mTemperature.put(zone, temperature);
                mTemperatureAvailable.put(zone, available);
                mLastTemperatureSet.put(zone, SystemClock.uptimeMillis());
            }
        }
    }

    public boolean shouldPropagateTempUpdate(int zone, float temperature, boolean available) {
        synchronized (mTemperature) {
            synchronized (mTemperatureAvailable) {
                if (SystemClock.uptimeMillis() - mLastTemperatureSet.get(zone) < COALESCE_TIME_MS) {
                    if (available == mTemperatureAvailable.get(zone)) {
                        return false;
                    }
                }
            }
            setTemperature(zone, temperature, available);
        }
        return true;
    }
}
