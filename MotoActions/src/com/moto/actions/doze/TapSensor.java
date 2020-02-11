/*
 * Copyright (c) 2017 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.moto.actions.doze;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.util.Log;

import com.moto.actions.actions.UpdatedStateNotifier;
import com.moto.actions.MotoActionsSettings;
import com.moto.actions.SensorAction;
import com.moto.actions.SensorHelper;

import com.moto.actions.util.FileUtils;

public class TapSensor implements SensorEventListener, UpdatedStateNotifier {
    private static final String TAG = "MotoActions-TapSensor";

    private static final String mTapNode = "/sys/class/sensors/dt-gesture/enable";
    private static final String mTapWakeNode = "/sys/class/sensors/dt-gesture/enable_wakeup";

    private final MotoActionsSettings mMotoActionsSettings;
    private final SensorHelper mSensorHelper;
    private final SensorAction mSensorAction;
    private final Sensor mSensor;
    private final Sensor mProx;

    private boolean mIsEnabled;
    private boolean mProxIsCovered;

    public TapSensor(MotoActionsSettings motoActionsSettings, SensorHelper sensorHelper,
                SensorAction action) {
        mMotoActionsSettings = motoActionsSettings;
        mSensorHelper = sensorHelper;
        mSensorAction = action;

        mSensor = sensorHelper.getTapSensor();
        mProx = sensorHelper.getProximitySensor();
    }

    @Override
    public synchronized void updateState() {
        if (mMotoActionsSettings.isTapToWakeEnabled() && !mIsEnabled) {
            Log.d(TAG, "Enabling");
            FileUtils.writeLine(mTapNode, "1");
            FileUtils.writeLine(mTapWakeNode,  "1");
            mSensorHelper.registerListener(mSensor, this);
            mSensorHelper.registerListener(mProx, mProxListener);
            mIsEnabled = true;
        } else if (! mMotoActionsSettings.isChopChopGestureEnabled() && mIsEnabled) {
            Log.d(TAG, "Disabling");
            FileUtils.writeLine(mTapNode, "0");
            FileUtils.writeLine(mTapWakeNode,  "0");
            mSensorHelper.unregisterListener(this);
            mSensorHelper.unregisterListener(mProxListener);
            mIsEnabled = false;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.d(TAG, "chop chop triggered");
        if (mProxIsCovered) {
            Log.d(TAG, "proximity sensor covered, ignoring chop-chop");
            return;
        }
        mSensorAction.action();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private SensorEventListener mProxListener = new SensorEventListener() {
        @Override
        public synchronized void onSensorChanged(SensorEvent event) {
            mProxIsCovered = event.values[0] < mProx.getMaximumRange();
        }

        @Override
        public void onAccuracyChanged(Sensor mSensor, int accuracy) {
        }
    };
}
