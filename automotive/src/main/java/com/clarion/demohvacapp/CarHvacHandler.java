package com.clarion.demohvacapp;

import android.car.CarNotConnectedException;
import android.car.VehicleAreaSeat;
import android.car.hardware.CarPropertyValue;
import android.car.hardware.hvac.CarHvacManager;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.GuardedBy;

import java.util.ArrayList;
import java.util.List;

public class CarHvacHandler {
    private static final String TAG = "CarHvacHandler";

    private DataStore mDataStore = new DataStore();
    private CarHvacManager mCarHvacManager;

    private static final int DRIVER_ZONE_ID = VehicleAreaSeat.SEAT_ROW_1_LEFT |
            VehicleAreaSeat.SEAT_ROW_2_LEFT | VehicleAreaSeat.SEAT_ROW_2_CENTER;
    private static final int PASSENGER_ZONE_ID = VehicleAreaSeat.SEAT_ROW_1_RIGHT |
            VehicleAreaSeat.SEAT_ROW_2_RIGHT;

    public static int getDriverZoneId() {
        return DRIVER_ZONE_ID;
    }

    public static int getPassengerZoneId() {
        return PASSENGER_ZONE_ID;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Callback for receiving updates from the sensor manager. A Callback can be
     * registered using {@link # registerCallback}.
     */
    public static abstract class Callback {

        public void onPassengerTemperatureChange(CarPropertyValue propValue) {
        }

        public void onDriverTemperatureChange(CarPropertyValue propValue) {
        }
    }
    @GuardedBy("mCallbacks")
    public List<Callback> mCallbacks = new ArrayList<>();

    public void registerCallback(Callback callback) {
        synchronized (mCallbacks) {
            mCallbacks.add(callback);
        }
    }

    public void unregisterCallback(Callback callback) {
        synchronized (mCallbacks) {
            mCallbacks.remove(callback);
        }
    }

    public void initHvacManager(CarHvacManager carManager) {
        mCarHvacManager = carManager;
        try {
            mCarHvacManager.registerCallback(mCallbackEventFromHvacManager);
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "Can not connected in ");
        }
    }

    public void DeInitHvacManager() {
        if (mCarHvacManager != null) {
            mCarHvacManager.unregisterCallback(mCallbackEventFromHvacManager);
        }
    }

    public final CarHvacManager.CarHvacEventCallback mCallbackEventFromHvacManager = new CarHvacManager.CarHvacEventCallback() {
        @Override
        public void onChangeEvent(CarPropertyValue carPropertyValue) {
            int areaId = carPropertyValue.getAreaId();
            switch (carPropertyValue.getPropertyId()){
                case CarHvacManager.ID_ZONED_TEMP_SETPOINT:
                    handleTempUpdate(carPropertyValue);
                    break;
                case CarHvacManager.ID_ZONED_AC_ON:
                case CarHvacManager.ID_ZONED_FAN_DIRECTION:
                case CarHvacManager.ID_ZONED_FAN_SPEED_SETPOINT:
                case CarHvacManager.ID_WINDOW_DEFROSTER_ON:
                case CarHvacManager.ID_ZONED_AIR_RECIRCULATION_ON:
                case CarHvacManager.ID_ZONED_SEAT_TEMP:
                case CarHvacManager.ID_ZONED_AUTOMATIC_MODE_ON:
                case CarHvacManager.ID_ZONED_HVAC_POWER_ON:
                default:
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Unhandled HVAC event, id: " + carPropertyValue.getPropertyId());
                    }
            }
        }

        @Override
        public void onErrorEvent(int i, int i1) {
        }
    };


    public void handleTempUpdate(CarPropertyValue carPropertyValue) {
        final int zone = carPropertyValue.getAreaId();
        final float temp = (Float) carPropertyValue.getValue();
        final boolean available = carPropertyValue.getStatus() == CarPropertyValue.STATUS_AVAILABLE;
        boolean shouldPropagate = mDataStore.shouldPropagateTempUpdate(zone, temp, available);
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Temp Update, zone: " + zone + " temp: " + temp +
                    "available: " + available + " should propagate: " + shouldPropagate);
        }
        if (shouldPropagate) {

            synchronized (mCallbacks) {
                for (int i = 0; i < mCallbacks.size(); i++) {
                    if (zone == DRIVER_ZONE_ID) {
                        mCallbacks.get(i)
                                .onDriverTemperatureChange(carPropertyValue);
                    } else if (zone == PASSENGER_ZONE_ID) {
                        mCallbacks.get(i)
                                .onPassengerTemperatureChange(carPropertyValue);
                    } else {
                        Log.w(TAG, "Unknown temperature set area id: " + zone);
                    }
                }
            }
        }
    }

    private void fetchTemperature(int zone) {
        if (mCarHvacManager != null) {
            try {
                float value = mCarHvacManager.getFloatProperty(
                        CarHvacManager.ID_ZONED_TEMP_SETPOINT, zone);
                boolean available = mCarHvacManager.isPropertyAvailable(
                        CarHvacManager.ID_ZONED_TEMP_SETPOINT, zone);
                mDataStore.setTemperature(zone, value, available);
            } catch (android.car.CarNotConnectedException e) {
                Log.e(TAG, "Car not connected in fetchTemperature");
            }
        }
    }

    public boolean isTemperatureControlAvailable(int zone) {
        if (mCarHvacManager != null) {
            try {
                return mCarHvacManager.isPropertyAvailable(
                        CarHvacManager.ID_ZONED_TEMP_SETPOINT, zone);
            } catch (android.car.CarNotConnectedException e) {
                Log.e(TAG, "Car not connected in isTemperatureControlAvailable");
            }
        }

        return false;
    }

    public boolean isDriverTemperatureControlAvailable() {
        return isTemperatureControlAvailable(DRIVER_ZONE_ID);
    }

    public boolean isPassengerTemperatureControlAvailable() {
        return isTemperatureControlAvailable(PASSENGER_ZONE_ID);
    }

    public int getDriverTemperature() {
        return Float.valueOf(mDataStore.getTemperature(DRIVER_ZONE_ID)).intValue();
    }

    public int getPassengerTemperature() {
        return Float.valueOf(mDataStore.getTemperature(PASSENGER_ZONE_ID)).intValue();
    }

    public void setDriverTemperature(int temperature) {
        setTemperature(DRIVER_ZONE_ID, temperature);
    }

    public void setPassengerTemperature(int temperature) {
        setTemperature(PASSENGER_ZONE_ID, temperature);
    }

    public void setTemperature(final int zone, final float temperature) {
        final AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            protected Void doInBackground(Void... unused) {
                if (mCarHvacManager != null) {
                    try {
                        mCarHvacManager.setFloatProperty(
                                CarHvacManager.ID_ZONED_TEMP_SETPOINT, zone, temperature);
                        // if the set() succeeds, consider the property available
                        mDataStore.setTemperature(zone, temperature, true);
                    } catch (android.car.CarNotConnectedException e) {
                        Log.e(TAG, "Car not connected in setTemperature");
                    } catch (Exception e) {
                        Log.e(TAG, "set temp failed", e);
                    }
                }
                return null;
            }
        };
        task.execute();
    }

}
