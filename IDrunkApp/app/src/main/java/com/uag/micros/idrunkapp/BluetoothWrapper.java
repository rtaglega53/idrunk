package com.uag.micros.idrunkapp;

import android.bluetooth.BluetoothAdapter;

public class BluetoothWrapper {
    private static BluetoothWrapper sInstance;
    private BluetoothAdapter mBluetoohAdapter;

    private BluetoothWrapper() {
        mBluetoohAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public static synchronized BluetoothWrapper getInstance() {
        if (sInstance == null) {
            sInstance = new BluetoothWrapper();
        }
        return sInstance;
    }

    public boolean deviceSupportsBluetooth() {
        return mBluetoohAdapter != null;
    }

    public boolean enableBluetooth() {
        if (!deviceSupportsBluetooth()) {
            return false;
        }

        if (mBluetoohAdapter.isEnabled()) {
            // already enabled
            return true;
        } else {

        }

        return false;
    }
}
