package com.uag.micros.idrunkapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.uag.micros.idrunkapp.adapter.BluetoohDevicesRecyclerViewAdapter;
import com.uag.micros.idrunkapp.fragment.PairWithDeviceDialogFragment;
import com.uag.micros.idrunkapp.receiver.BluetoothScanReceiver;
import com.uag.micros.idrunkapp.ui.ItemMarginDecoration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity
        implements View.OnClickListener,
        BluetoohDevicesRecyclerViewAdapter.BluetoothDeviceClickListener,
        BluetoothScanReceiver.Listener,
        PairWithDeviceDialogFragment.PositiveClickListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int BLUETOOTH_PERMISSIONS_REQUEST_CODE = 0;

    private boolean mBluetoothDiscoveryReceiverRegistered;
    private boolean mDiscoveryIsFinished;

    private Button mButtonScanForDevices;

    private BluetoothAdapter mBlueToothAdapter;
    private BluetoohDevicesRecyclerViewAdapter mBluetoothDevicesRecyclerViewAdapter;
    private List<BluetoothDevice> mDevicesList;
    private BluetoothScanReceiver mBluetoothScanReceiver;
    private Set<String> deviceSet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUI();
        mDiscoveryIsFinished = true;
        mBlueToothAdapter = BluetoothAdapter.getDefaultAdapter();
        deviceSet = new HashSet<>();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerBluetoothDiscoveryReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unRegisterBluetoothDiscoveryReceiver();
        mBlueToothAdapter.cancelDiscovery();
        mDiscoveryIsFinished = true;
        mButtonScanForDevices.setEnabled(true);
    }

    private void initUI() {
        mButtonScanForDevices = findViewById(R.id.btn_search_devices);
        mButtonScanForDevices.setOnClickListener(this);
        mBluetoothDevicesRecyclerViewAdapter = new BluetoohDevicesRecyclerViewAdapter(this,
                this);
        mDevicesList = new ArrayList<>();
        mBluetoothDevicesRecyclerViewAdapter.setDevicesList(mDevicesList);
        final LinearLayoutManager layoutManager =
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL,
                        false);
        final RecyclerView devicesRecyclerView = findViewById(R.id.rv_devices);
        // 8dp margin
        devicesRecyclerView.addItemDecoration(
                new ItemMarginDecoration((int) (8 * Resources.getSystem().getDisplayMetrics().density))
        );
        devicesRecyclerView.setLayoutManager(layoutManager);
        devicesRecyclerView.setAdapter(mBluetoothDevicesRecyclerViewAdapter);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == BLUETOOTH_PERMISSIONS_REQUEST_CODE) {
            Log.d(TAG, "permissions given");

            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startBluetoothAndScanForDevices();
            } else {
                Toast.makeText(this, "The app needs permission to continue", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void unRegisterBluetoothDiscoveryReceiver() {
        if (!mBluetoothDiscoveryReceiverRegistered) {
            return;
        }
        unregisterReceiver(mBluetoothScanReceiver);
        mBluetoothDiscoveryReceiverRegistered = false;
        mBluetoothScanReceiver = null;
    }

    private void registerBluetoothDiscoveryReceiver() {
        if (mBluetoothDiscoveryReceiverRegistered) {
            return;
        }

        if (mBluetoothScanReceiver == null) {
            mBluetoothScanReceiver = new BluetoothScanReceiver(this);
        }
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(android.bluetooth.BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mBluetoothScanReceiver, intentFilter);
        mBluetoothDiscoveryReceiverRegistered = true;
    }

    private void startBluetoothAndScanForDevices() {
        // clear items
        deviceSet.clear();
        mDevicesList.clear();
        mBluetoothDevicesRecyclerViewAdapter.notifyDataSetChanged();
        // stop discovery first
        mDiscoveryIsFinished = false;
        mButtonScanForDevices.setEnabled(false);
        mBlueToothAdapter.cancelDiscovery();
        mBlueToothAdapter.startDiscovery();
    }

    private void scanForBluetoothDevices() {
        if (mBlueToothAdapter == null) {
            Toast.makeText(this, "Device bluetooth not enabled", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // ask for bluetooth permissions
            ActivityCompat.requestPermissions(this,
                    new String[]{ Manifest.permission.ACCESS_FINE_LOCATION },
                    BLUETOOTH_PERMISSIONS_REQUEST_CODE);
        } else {
            startBluetoothAndScanForDevices();
        }
    }

    @Override
    public void onClick(View v) {
        if (!mDiscoveryIsFinished) {
            return;
        }
        scanForBluetoothDevices();
    }

    @Override
    public void onBluetoothDeviceClick(int position) {
        Log.d(TAG, "clicked position " + position);
        final PairWithDeviceDialogFragment dialog = new PairWithDeviceDialogFragment(position);
        dialog.show(getSupportFragmentManager(), "PairWithDeviceDialogFragment");
    }

    @Override
    public void onBluetoothScanReceive(BluetoothDevice device) {
        if (deviceSet.contains(device.getAddress())) {
            return;
        }
        deviceSet.add(device.getAddress());
        mDevicesList.add(device);
        mBluetoothDevicesRecyclerViewAdapter.notifyItemInserted(mDevicesList.size() - 1);
    }

    @Override
    public void onBluetoothScanStarted() {
        Log.d(TAG, "bluetooth scan started");
    }

    @Override
    public void onBluetoothScanFinished() {
        Log.d(TAG, "bluetooth scan finished");
        mDiscoveryIsFinished = true;
        mButtonScanForDevices.setEnabled(true);
    }

    @Override
    public void onPositiveClickListener(int deviceClickedPosition) {
        Log.d(TAG, "positive click");
        final Intent pairedDeviceActivityIntent = new Intent(this,
                PairedDeviceActivity.class);
        pairedDeviceActivityIntent.putExtra("device", mDevicesList.get(deviceClickedPosition));
        startActivity(pairedDeviceActivityIntent);
    }
}
