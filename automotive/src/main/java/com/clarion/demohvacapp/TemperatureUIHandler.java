package com.clarion.demohvacapp;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class TemperatureUIHandler {
    private static final String TAG = "TemperatureUIHandler";
    private static final int DEFAULT_TEMPERATURE = 32;
    private static final int MAX_TEMPERATURE = 256;
    private static final int MIN_TEMPERATURE = 0;
    private int mTemperature = DEFAULT_TEMPERATURE;
    private boolean mIsOn = true;

    private ProgressBar mProgressbar;
    private Button mIncreaseButton;
    private Button mDecreaseButton;
    private TextView mTempTextView;
    private int mStringResId;
    Context mContext;

    private int mTempColor1;
    private int mTempColor2;
    private int mTempColor3;
    private int mTempColor4;
    private int mTempColor5;
    private static final int COLOR_CHANGE_ANIMATION_TIME_MS = 200;

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

    public void Initialize(Button inc,Button dec,TextView temp,ProgressBar bar,int stringResId,Context context){

        mTempColor1 = R.color.temperature_1;
        mTempColor2 = R.color.temperature_2;
        mTempColor3 = R.color.temperature_3;
        mTempColor4 = R.color.temperature_4;
        mTempColor5 = R.color.temperature_5;

        mTempTextView = temp;
        mTempTextView.setText(R.string.hvac_invalid_temperature);

        mIncreaseButton = inc;
        mDecreaseButton = dec;
        mIncreaseButton.setOnTouchListener(new PressAndHoldTouchListener(temperatureClickListener));
        mDecreaseButton.setOnTouchListener(new PressAndHoldTouchListener(temperatureClickListener));

        mProgressbar = bar;
        mProgressbar.setMax(MAX_TEMPERATURE);
        mProgressbar.setMin(MIN_TEMPERATURE);
        mProgressbar.setProgress(DEFAULT_TEMPERATURE);

        mStringResId = stringResId;
        mContext = context;
    }


    public void setTemperature(int temperature) {
        Log.d(TAG, "setTemperature(" + temperature + ")");
        int startColor = getTemperatureColor(mTemperature);
        int endColor = getTemperatureColor(temperature);
        mTemperature = temperature;
        String temperatureString;

        if (mTemperature < MIN_TEMPERATURE || mTemperature > MAX_TEMPERATURE) {
            temperatureString = mContext.getString(R.string.hvac_invalid_temperature);
        } else {
            temperatureString = String.valueOf(mTemperature);
        }

        synchronized (this) {
            mTempTextView.setText(mContext.getString(mStringResId,
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
                    Log.d(TAG, "setting temperature not available");
                    return;
                }
                int startColor = getTemperatureColor(mTemperature);

                if (v == mIncreaseButton && mTemperature < MAX_TEMPERATURE) {
                    mTemperature++;
                    Log.d(TAG, "increased temperature to " + mTemperature);
                } else if (v == mDecreaseButton && mTemperature > MIN_TEMPERATURE) {
                    mTemperature--;
                    Log.d(TAG, "decreased temperature to " + mTemperature);
                } else {
                    Log.d(TAG, "key not recognized");
                }
                int endColor = getTemperatureColor(mTemperature);
                Log.d(TAG, "onClick: mTemperature ="+mTemperature+" startColor ="+startColor
                +"endColor = "+endColor);
                changeTemperatureColor(startColor, endColor);
                String temp = String.valueOf(mTemperature);
                mTempTextView.setText(mContext.getString(mStringResId,
                        temp));

                mProgressbar.setProgress(mTemperature);
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
            Log.d(TAG, "changeTemperatureColor: endColor = "+endColor);
            mTempTextView.setBackgroundColor(endColor);
            ((GradientDrawable) mProgressbar.getProgressDrawable()).setColor(endColor);
        }
    }

    private final ValueAnimator.AnimatorUpdateListener mTemperatureColorListener
            = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            int color = (Integer) animation.getAnimatedValue();
            //((GradientDrawable) mDriverTemp.getBackground()).setColor(color);
            Log.d(TAG, "onAnimationUpdate: color = "+color);
            mTempTextView.setBackgroundColor(color);
           // ((GradientDrawable) mProgressbar.getProgressDrawable()).setColor(color);
        }
    };

}
