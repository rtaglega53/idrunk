package com.uag.micros.idrunkapp;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.uag.micros.idrunkapp.request.VolleyRequester;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

public class PairedDeviceActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = PairedDeviceActivity.class.getSimpleName();

    private final int RECEIVE_MESSAGE = 1;
    private static final UUID MY_RANDOM_UUID
            = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothSocket mBluetoothSocket;
    private BluetoothDevice mBluetoothDevice;
    private ConnectedThread mConnectedThread;
    private Handler mHandler;
    private Handler mSendingHandler;
    private StringBuilder sb = new StringBuilder();

    private Button mConnectButton;
    private Button mStartReceivingButton;
    private Button mStopReceivingButton;
    private EditText mEditTextNIP;
    private TextView mTvDeviceStatus;
    private TextView mTvAlcoholLevel;
    private TextView mTvVoltageLevel;
    private static final int SENDER_FREQUENCY = 500;
    private double mAlcoholLevel = 0;

    private Runnable mSenderRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                sendInfo();
            } finally {
                mSendingHandler.postDelayed(mSenderRunnable, SENDER_FREQUENCY);
            }
        }
    };

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paired_device);
        mBluetoothDevice = getIntent().getParcelableExtra("device");
        initUI(mBluetoothDevice);
        // initialize handler
        mHandler = new Handler(){
            public void handleMessage(android.os.Message msg) {
                if (msg.what == RECEIVE_MESSAGE) {
                    byte[] readBuf = (byte[]) msg.obj;
                    String incomingData = new String(readBuf, 0, msg.arg1);
                    sb.append(incomingData);
                    // check for space
                    int spaceIndex = sb.indexOf(" ");
                    Log.d(TAG, "index of space:" + spaceIndex);
                    Log.d(TAG, "sb length:" + sb.length());
                    Log.d(TAG, "sb raw: " + sb.toString());

                    if (spaceIndex > 0) {
                        final String incomingVoltageString = sb.toString().trim();
                        Log.d("TAG", "incoming voltage as string:" + incomingVoltageString);
                        final double incomingVoltage = Integer.parseInt(incomingVoltageString);
                        double incomingVoltageDouble = (incomingVoltage / 10000) - 0.5;

                        if (incomingVoltageDouble < 0) {
                            incomingVoltageDouble = 0.0;
                        }
                        Log.d(TAG, "incoming voltage: " + incomingVoltage);
                        final double rs = ((5 - (incomingVoltageDouble)) / (incomingVoltageDouble)) * 1000;
                        double tempAlcoholLevel = (0.4091 * Math.pow((rs / 5463), -1.497));

                        if (tempAlcoholLevel < 0) {
                            tempAlcoholLevel = 0.0;
                        }

                        if ((tempAlcoholLevel > mAlcoholLevel) && !(Double.isInfinite(tempAlcoholLevel))) {
                            mAlcoholLevel = tempAlcoholLevel;
                        }
                        Log.d(TAG, "alcohol level after conversion: " + tempAlcoholLevel);
                        // 0.6
                        mTvAlcoholLevel.setText(String.format(Locale.getDefault(), "%.2f mg/L", tempAlcoholLevel));
                        mTvVoltageLevel.setText(String.format(Locale.getDefault(), "%.2f V", incomingVoltageDouble));

                        if (tempAlcoholLevel >= 1) {
                            // RED:
                            mTvAlcoholLevel.setTextColor(Color.parseColor("#D62031"));
                        } else if (tempAlcoholLevel >= 0.6) {
                            // ORANGE:
                            mTvAlcoholLevel.setTextColor(Color.parseColor("#E78237"));
                        } else {
                            // GREEN
                            mTvAlcoholLevel.setTextColor(Color.parseColor("#2C9B16"));
                        }
                        // clear the string builder
                        sb.delete(0, sb.length());
                    }
                }
            }
        };
        mSendingHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
            }
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mConnectedThread != null) {
            mHandler.removeCallbacks(mConnectedThread);
        }

        if (mSenderRunnable != null) {
            mSendingHandler.removeCallbacks(mSenderRunnable);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBluetoothSocket != null) {
            try {
                mBluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendRequest() {
        try {
            final JSONObject payload = new JSONObject();
            payload.put("Value", mAlcoholLevel);
            payload.put("Device",
                    Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));

            if (mAlcoholLevel >= 1) {
                // RED:
                payload.put("Comment", "Muy borracho");
            } else if (mAlcoholLevel >= 0.6) {
                // ORANGE:
                payload.put("Comment", "Borracho");
            } else {
                // GREEN
                payload.put("Comment", "Normal");
            }
            payload.put("Comment", "");
            final SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            sf.setTimeZone(TimeZone.getTimeZone("UTC"));
            payload.put("CreationDate", sf.format(new Date()));
            payload.put("Name", "Antonio Tagle");
            final JsonObjectRequest jsonRequest
                    = new JsonObjectRequest(
                    Request.Method.POST,
                    "http://189.171.69.125/AlcoholTest/AlcoholTest/api/alcoholtest",
                    payload,
                    new Response.Listener<JSONObject>() {

                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                Log.d(TAG, "response received");
                                Log.d(TAG, "request response" + response.toString(2));
                                Toast.makeText(getApplicationContext(), "Data sent...", Toast.LENGTH_SHORT).show();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            error.printStackTrace();
                            Toast.makeText(getApplicationContext(), "Could not send data...", Toast.LENGTH_SHORT).show();
                        }
                    });
            VolleyRequester.getInstance(this).enqueueJSONRequest(jsonRequest);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private BluetoothSocket initSocket(BluetoothDevice bluetoothDevice) throws IOException {
        if(Build.VERSION.SDK_INT >= 23){
            try{
                final Method m = bluetoothDevice.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
                return (BluetoothSocket) m.invoke(bluetoothDevice, MY_RANDOM_UUID);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return bluetoothDevice.createRfcommSocketToServiceRecord(MY_RANDOM_UUID);
    }

    private void initUI(BluetoothDevice device) {
        final TextView tvDeviceName = findViewById(R.id.tv_device_name);
        final String deviceName = device.getName() == null ? "?" : device.getName();
        tvDeviceName.setText("Name: " + deviceName);
        final TextView tvDeviceAddress = findViewById(R.id.tv_device_address);
        tvDeviceAddress.setText("Address: " + device.getAddress());
        mConnectButton = findViewById(R.id.btn_connect);
        mConnectButton.setOnClickListener(this);
        mEditTextNIP = findViewById(R.id.et_nip_input);
        mTvDeviceStatus = findViewById(R.id.tv_device_status);
        mTvDeviceStatus.setText("Connection Status: NOT CONNECTED");
        mTvAlcoholLevel = findViewById(R.id.tv_alcohol_level);
        mStopReceivingButton = findViewById(R.id.btn_stop_receiving);
        mStopReceivingButton.setOnClickListener(this);
        mStartReceivingButton = findViewById(R.id.btn_receive);
        mStartReceivingButton.setOnClickListener(this);
        mTvVoltageLevel = findViewById(R.id.tv_voltage_value);
    }

    private void updateUIForConnection() {
        mStopReceivingButton.setEnabled(true);
        mStartReceivingButton.setEnabled(true);
        mTvDeviceStatus.setText("Connection Status: CONNECTED");
        Toast.makeText(this, "Device connected", Toast.LENGTH_LONG).show();
    }

    private void updateUIForFailedConnection() {
        mConnectButton.setEnabled(true);
        mEditTextNIP.setEnabled(true);
        mTvDeviceStatus.setText("Connection Status: NOT CONNECTED");
        Toast.makeText(this, "Could not connect to device", Toast.LENGTH_LONG).show();
    }

    private void disableConnect() {
        mConnectButton.setEnabled(false);
        mEditTextNIP.setEnabled(false);
    }

    private void connectToDevice() {
        try {
            mBluetoothSocket = initSocket(mBluetoothDevice);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        try {
            mBluetoothSocket.connect();
            updateUIForConnection();
        } catch (IOException e) {
            Log.d(TAG, "exception connecting");
            e.printStackTrace();
            updateUIForFailedConnection();
            try {
                mBluetoothSocket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
                return;
            }
        }
    }

    private void sendInfo() {
        if (mBluetoothSocket == null || !mBluetoothSocket.isConnected() || mConnectedThread == null) {
            return;
        }
        mConnectedThread.write("s");
    }

    private void startSendingTask() {
        if (mBluetoothSocket == null) {
            Toast.makeText(this, "Error receiving data from device",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (mConnectedThread == null) {
            mConnectedThread = new ConnectedThread(mBluetoothSocket);
        }

        if (!mConnectedThread.isAlive()) {
            Log.d(TAG, "starting connected thread");
            mConnectedThread.start();
        }
        mSenderRunnable.run();
    }

    private void stopSendingTask() {
        if (mSenderRunnable != null) {
            mSendingHandler.removeCallbacks(mSenderRunnable);
        }
        // send the last value to the API
        sendRequest();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_connect:
                disableConnect();
                connectToDevice();
                break;

            case R.id.btn_receive:
                startSendingTask();
                break;

            case R.id.btn_stop_receiving:
                stopSendingTask();
                break;
        }
    }

    private class ConnectedThread extends Thread {
        private final InputStream mInStream;
        private final OutputStream mOutStream;


        public ConnectedThread(BluetoothSocket bluetoothSocket) {
            InputStream tmpInputStream = null;
            OutputStream tmpOutputStream = null;

            try {
                tmpInputStream = bluetoothSocket.getInputStream();
                tmpOutputStream = bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                Log.d(TAG, "error getting intput, output streams from bluetooth socket");
            }
            mInStream = tmpInputStream;
            mOutStream = tmpOutputStream;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            while (true) {
                try {
                    //Log.i(TAG, "running thread");
                    bytes = mInStream.read(buffer);
                    //Log.i(TAG, "bytes received: " + bytes);
                    // check if the byte is an space
                    mHandler.obtainMessage(RECEIVE_MESSAGE, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    Log.d(TAG, "error getting data from stream");
                    break;
                }
            }
        }

        public void write(String message) {
            //Log.d(TAG, "sending: " + message + "...");
            byte[] msgBuffer = message.getBytes();
            try {
                mOutStream.write(msgBuffer);
            } catch (IOException e) {
                Log.d(TAG, "error sending data: " + e.getMessage() + "...");
            }
        }
    }
}
