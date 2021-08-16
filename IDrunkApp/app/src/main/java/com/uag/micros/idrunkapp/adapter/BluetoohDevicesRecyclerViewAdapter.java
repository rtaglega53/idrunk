package com.uag.micros.idrunkapp.adapter;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.uag.micros.idrunkapp.R;

import java.lang.ref.WeakReference;
import java.util.List;

public class BluetoohDevicesRecyclerViewAdapter
        extends RecyclerView.Adapter<BluetoohDevicesRecyclerViewAdapter.ViewHolder> {
    private LayoutInflater mInflater;
    private BluetoothDeviceClickListener mBluetoothDeviceClickListener;
    private List<BluetoothDevice> mDevicesList;

    public BluetoohDevicesRecyclerViewAdapter(
            Context context,
            BluetoothDeviceClickListener bluetoothDeviceClickListener) {
        mInflater = LayoutInflater.from(context);
        mBluetoothDeviceClickListener = bluetoothDeviceClickListener;
    }

    public void setDevicesList(List<BluetoothDevice> devicesList) {
        mDevicesList = devicesList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(mInflater.inflate(R.layout.bluetooth_device_list_item, parent, false),
                mBluetoothDeviceClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final BluetoothDevice bluetoothDevice = mDevicesList.get(position);
        final String deviceName = bluetoothDevice.getName() != null ? bluetoothDevice.getName() : "?";
        holder.deviceTextView.setText("Name: " + deviceName + " - Address: " + bluetoothDevice.getAddress());
    }

    @Override
    public int getItemCount() {
        return mDevicesList == null ? 0 : mDevicesList.size();
    }

    public interface BluetoothDeviceClickListener {
        void onBluetoothDeviceClick(int position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {
        private TextView deviceTextView;
        private WeakReference<BluetoothDeviceClickListener> mBlueToothDeviceClickListenerWR;

        ViewHolder(@NonNull View itemView,
                          BluetoothDeviceClickListener bluetoothDeviceClickListener) {
            super(itemView);
            deviceTextView = itemView.findViewById(R.id.tv_device);
            mBlueToothDeviceClickListenerWR = new WeakReference<>(bluetoothDeviceClickListener);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            final BluetoothDeviceClickListener clickListener =
                    mBlueToothDeviceClickListenerWR.get();

            if (clickListener != null) {
                clickListener.onBluetoothDeviceClick(getAdapterPosition());
            }
        }
    }
}
