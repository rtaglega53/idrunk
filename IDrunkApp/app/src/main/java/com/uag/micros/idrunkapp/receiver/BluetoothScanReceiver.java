package com.uag.micros.idrunkapp.receiver;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.lang.ref.WeakReference;

public class BluetoothScanReceiver extends BroadcastReceiver {
    private String TAG = BluetoothScanReceiver.class.getSimpleName();

    private WeakReference<Listener> mListenerWR;

    public BluetoothScanReceiver(Listener listener) {
        mListenerWR = new WeakReference<>(listener);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (BluetoothDevice.ACTION_FOUND.equalsIgnoreCase(action)) {
            Log.d(TAG, "device found!");
            final Listener listener = mListenerWR.get();

            if (listener != null) {
                final BluetoothDevice device =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                listener.onBluetoothScanReceive(device);

            }
        } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equalsIgnoreCase(action)) {
            final Listener listener = mListenerWR.get();

            if (listener != null) {
                final BluetoothDevice device =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                listener.onBluetoothScanFinished();

            }
        } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equalsIgnoreCase(action)) {
            final Listener listener = mListenerWR.get();

            if (listener != null) {
                final BluetoothDevice device =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                listener.onBluetoothScanStarted();

            }
        }
    }

    public interface Listener {
        void onBluetoothScanReceive(BluetoothDevice device);
        void onBluetoothScanStarted();
        void onBluetoothScanFinished();
    }
}
