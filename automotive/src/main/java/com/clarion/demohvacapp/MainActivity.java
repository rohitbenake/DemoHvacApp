package com.clarion.demohvacapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.car.Car;
import android.car.CarNotConnectedException;
import android.car.hardware.CarPropertyValue;
import android.car.hardware.hvac.CarHvacManager;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private Object mCarHvacManagerReady = new Object();
    private Car mCarService;
    private final String[] permissions = new String[]{Car.PERMISSION_CONTROL_CAR_CLIMATE};
    CarHvacHandler mCarHvacHandler = new CarHvacHandler();
    TemperatureUIHandler mDriverTemperatureUIHandler = new TemperatureUIHandler();
    TemperatureUIHandler mPassengerTemperatureUIHandler = new TemperatureUIHandler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDriverTemperatureUIHandler.Initialize((ImageButton) findViewById(R.id.btn_driver_temp_inc),
                (ImageButton)findViewById(R.id.btn_driver_temp_dec),
                (TextView) findViewById(R.id.driver_temp_textview),
                (ProgressBar) findViewById(R.id.progressBar_temp_driver),
                R.string.driver_temp_string,
                this
                );

        mDriverTemperatureUIHandler.setTemperatureChangeListener(mDriverTempClickListener);

        mPassengerTemperatureUIHandler.Initialize((ImageButton) findViewById(R.id.btn_passenger_temp_inc),
                (ImageButton)findViewById(R.id.btn_passenger_temp_dec),
                (TextView) findViewById(R.id.passenger_temp_textview),
                (ProgressBar) findViewById(R.id.progressbar_temp_passenger),
               R.string.passenger_temp_string,
                this
        );
        mPassengerTemperatureUIHandler.setTemperatureChangeListener(mPassengerTempClickListener);

        Log.i(TAG, "onCreate: calling EstablishCarServiceConnection");
        EstablishCarServiceConnection();
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: startService");
        startService();//Can this be done in OnCreate
    }

    @Override
    protected void onPause() {
        super.onPause();
        //Can Service be disconnected ?
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mCarHvacHandler.unregisterCallback(mValueCallback);
        mCarHvacHandler.DeInitHvacManager();
        if (mCarService != null) {
            mCarService.disconnect();
        }
    }

    private void startService() {
        if(checkSelfPermission(permissions[0]) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "startService: Permission for"+permissions[0]+" is GRANTED");

            if(mCarService == null) {
                Log.d(TAG, "EstablishCarServiceConnection:  mCarService is NULL");
                return;
            }

            if (!mCarService.isConnected() && !mCarService.isConnecting()) {
                mCarService.connect();
            }
        }
        else
        {
            Log.d(TAG, "startService: Permission for "+permissions[0]+" is NOT GRANTED");
            //requestPermissions(permissions, 0);//causing multiple times OnResume
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (permissions[0].equals(Car.PERMISSION_CONTROL_CAR_CLIMATE )&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //if (permissions[0] == Car.PERMISSION_CONTROL_CAR_CLIMATE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "onRequestPermissionsResult: Permission for"+ permissions[0]+" GRANTED");
            startService();
        }
    }

    private void EstablishCarServiceConnection() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
            Log.w(TAG, "EstablishCarServiceConnection: FEATURE_AUTOMOTIVE not available");
            return;
        }

        if(mCarService == null) {
            Log.d(TAG, "EstablishCarServiceConnection:  mCarService is NULL");
            mCarService = Car.createCar(this,mConnection);
        }
        else{
            Log.d(TAG, "EstablishCarServiceConnection: mCarService is already created");
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(TAG, "onServiceConnected: Connected to Car Service");
            initializeCallbacks();
            onCarServiceReady();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "onServiceDisconnected: Disconnected from Car Service");
        }
    };

    private void onCarServiceReady() {
        Log.d(TAG, "onCarServiceReady: entry");

        synchronized (mCarHvacManagerReady) {
            try {
                mCarHvacHandler.initHvacManager((CarHvacManager) mCarService.getCarManager(
                        Car.HVAC_SERVICE));

                mCarHvacManagerReady.notifyAll();

            } catch (CarNotConnectedException e) {
                Log.e(TAG, "Car not connected in onServiceConnected");
            }
        }
    }

    private void initializeCallbacks() {
        mCarHvacHandler.registerCallback(mValueCallback);
    }

    //Callback implementation for Temp value change
    private CarHvacHandler.Callback mValueCallback = new CarHvacHandler.Callback() {
        @Override
        public void onDriverTemperatureChange(CarPropertyValue propValue) {
            super.onDriverTemperatureChange(propValue);
            UpdateDriverTemperatureToUI(propValue);
        }

        @Override
        public void onPassengerTemperatureChange(CarPropertyValue propValue) {
            super.onPassengerTemperatureChange(propValue);
            UpdatePassengerTemperatureToUI(propValue);
        }
    };

    private void UpdatePassengerTemperatureToUI(CarPropertyValue propValue) {
        final boolean available = propValue.getStatus() == CarPropertyValue.STATUS_AVAILABLE;
        //mPassengerTempBarExpanded.setAvailable(available);
       // mPassengerTempBarCollapsed.setAvailable(available);
        if (available) {
            final int temp = ((Float) propValue.getValue()).intValue();
            Log.d(TAG, "UpdatePassengerTemperatureToUI: temparature : "+temp);
            mPassengerTemperatureUIHandler.setTemperature(temp);
        }
    }

    private void UpdateDriverTemperatureToUI(CarPropertyValue propValue) {
        final boolean available = propValue.getStatus() == CarPropertyValue.STATUS_AVAILABLE;
        //mDriverTempBarExpanded.setAvailable(available);
        //mDriverTempBarCollapsed.setAvailable(available);

        if (available) {
            final int temp = ((Float) propValue.getValue()).intValue();
            Log.d(TAG, "UpdateDriverTemperatureToUI: temparature : "+temp);
            mDriverTemperatureUIHandler.setTemperature(temp);
        }
    }

    private final TemperatureUIHandler.TemperatureAdjustClickListener mDriverTempClickListener =
            new TemperatureUIHandler.TemperatureAdjustClickListener() {
                @Override
                public void onTemperatureChanged(int temperature) {
                    mCarHvacHandler.setDriverTemperature(temperature);
                }
            };

    private final TemperatureUIHandler.TemperatureAdjustClickListener mPassengerTempClickListener =
            new TemperatureUIHandler.TemperatureAdjustClickListener() {
                @Override
                public void onTemperatureChanged(int temperature) {
                    mCarHvacHandler.setPassengerTemperature(temperature);
                }
            };
}