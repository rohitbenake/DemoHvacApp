package com.clarion.demohvacapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.car.Car;
import android.car.CarNotConnectedException;
import android.car.hardware.CarPropertyValue;
import android.car.hardware.hvac.CarHvacManager;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private Object mCarHvacManagerReady = new Object();
    private Car mCarService;
    private final String[] permissions = new String[]{Car.PERMISSION_CONTROL_CAR_CLIMATE};
    CarHvacHandler mCarHvacHandler = new CarHvacHandler();

    private static final int DEFAULT_TEMPERATURE = 32;
    private static final int MAX_TEMPERATURE = 256;
    private static final int MIN_TEMPERATURE = 0;
    private int mTemperature = DEFAULT_TEMPERATURE;
    private boolean mIsOn = true;

    /**
     * A listener that observes clicks on the temperature bar.
     */
    public interface TemperatureAdjustClickListener {
        void onTemperatureChanged(int temperature);
    }
    private TemperatureAdjustClickListener mListener;
    public void setTemperatureChangeListener(TemperatureAdjustClickListener listener) {
        mListener =  listener;
    }

    private final TemperatureAdjustClickListener mDriverTempClickListener =
            new TemperatureAdjustClickListener() {
                @Override
                public void onTemperatureChanged(int temperature) {
                    mCarHvacHandler.setDriverTemperature(temperature);
                }
            };

    private Button mIncreaseButton;
    private Button mDecreaseButton;

    private TextView mPassengerTemp;

    private TextView mDriverTemp;
    private int mTempColor1;
    private int mTempColor2;
    private int mTempColor3;
    private int mTempColor4;
    private int mTempColor5;
    private static final int COLOR_CHANGE_ANIMATION_TIME_MS = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Resources res = getResources();
        mTempColor1 = res.getColor(R.color.temperature_1);
        mTempColor2 = res.getColor(R.color.temperature_2);
        mTempColor3 = res.getColor(R.color.temperature_3);
        mTempColor4 = res.getColor(R.color.temperature_4);
        mTempColor5 = res.getColor(R.color.temperature_5);

        mDriverTemp = findViewById(R.id.driver_temp_textview);
        mPassengerTemp = findViewById(R.id.passenger_temp_textview);
        mDriverTemp.setText(R.string.hvac_invalid_temperature);
        mPassengerTemp.setText(R.string.hvac_invalid_temperature);

        mIncreaseButton = findViewById(R.id.btn_driver_temp_inc);
        mDecreaseButton = findViewById(R.id.btn_driver_temp_dec);
        mIncreaseButton.setOnTouchListener(new PressAndHoldTouchListener(temperatureClickListener));
        mDecreaseButton.setOnTouchListener(new PressAndHoldTouchListener(temperatureClickListener));
        setTemperatureChangeListener(mDriverTempClickListener);

        Log.i(TAG, "onCreate: calling EstablishCarServiceConnection");
        EstablishCarServiceConnection();
    }

    public void setTemperature(int temperature) {
        Log.d(TAG, "setTemperature(" + temperature + ")");
        int startColor = getTemperatureColor(mTemperature);
        int endColor = getTemperatureColor(temperature);
        mTemperature = temperature;
        String temperatureString;

        if (mTemperature < MIN_TEMPERATURE || mTemperature > MAX_TEMPERATURE) {
            temperatureString = "--";
        } else {
            temperatureString = String.valueOf(mTemperature);
        }

        synchronized (this) {
            mDriverTemp.setText(getString(R.string.driver_temp_string,
                    temperatureString));
            // Only animate the color if the button is currently enabled.
            if (mIsOn) {
                changeTemperatureColor(startColor, endColor);
            }
        }
    }

    private final View.OnClickListener temperatureClickListener = new View.OnClickListener() {
        @SuppressLint("StringFormatMatches")
        @Override
        public void onClick(View v) {
            synchronized (this) {
                if (!mIsOn) {
                    Log.d("HvacTempBar", "setting temperature not available");
                    return;
                }
                int startColor = getTemperatureColor(mTemperature);

                if (v == mIncreaseButton && mTemperature < MAX_TEMPERATURE) {
                    mTemperature++;
                    Log.d("HvacTempBar", "increased temperature to " + mTemperature);
                } else if (v == mDecreaseButton && mTemperature > MIN_TEMPERATURE) {
                    mTemperature--;
                    Log.d("HvacTempBar", "decreased temperature to " + mTemperature);
                } else {
                    Log.d("HvacTempBar", "key not recognized");
                }
                int endColor = getTemperatureColor(mTemperature);
                changeTemperatureColor(startColor, endColor);
                String temp = String.valueOf(mTemperature);
                mDriverTemp.setText(getString(R.string.driver_temp_string,
                        temp));
                mListener.onTemperatureChanged(mTemperature);
            }
        }
    };

    private int getTemperatureColor(int temperature) {
        if (temperature >= 78) {
            return mTempColor1;
        } else if (temperature >= 74 && temperature < 78) {
            return mTempColor2;
        } else if (temperature >= 70 && temperature < 74) {
            return mTempColor3;
        } else if (temperature >= 66 && temperature < 70) {
            return mTempColor4;
        } else {
            return mTempColor5;
        }
    }

    private void changeTemperatureColor(int startColor, int endColor) {
        if (endColor != startColor) {
            ValueAnimator animator = ValueAnimator.ofArgb(startColor, endColor);
            animator.addUpdateListener(mTemperatureColorListener);
            animator.setDuration(COLOR_CHANGE_ANIMATION_TIME_MS);
            animator.start();
        } else {
           // ((GradientDrawable) mDriverTemp.getBackground()).setColor(endColor);
            mDriverTemp.setBackgroundColor(endColor);
        }
    }

    private final ValueAnimator.AnimatorUpdateListener mTemperatureColorListener
            = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            int color = (Integer) animation.getAnimatedValue();
            //((GradientDrawable) mDriverTemp.getBackground()).setColor(color);
            mDriverTemp.setBackgroundColor(color);
        }
    };

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
            //mPassengerTempBarExpanded.setTemperature(temp);
           // mPassengerTempBarCollapsed.setTemperature(temp);
        }
    }

    private void UpdateDriverTemperatureToUI(CarPropertyValue propValue) {
        final boolean available = propValue.getStatus() == CarPropertyValue.STATUS_AVAILABLE;
        //mDriverTempBarExpanded.setAvailable(available);
        //mDriverTempBarCollapsed.setAvailable(available);

        if (available) {
            final int temp = ((Float) propValue.getValue()).intValue();
            Log.d(TAG, "UpdateDriverTemperatureToUI: temparature : "+temp);
            setTemperature(temp);
        }
    }
}