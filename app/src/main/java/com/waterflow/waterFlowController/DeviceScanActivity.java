package com.waterflow.waterFlowController;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class DeviceScanActivity extends AppCompatActivity {

    public ListView deviceListview = null;
    public BLEDeviceAdapter     deviceListAdapter = null;


    public List<BLEDeviceItem> mBLEDeviceItemList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.device_scan_list);

        mBLEDeviceItemList = new ArrayList<>();

        Intent intent = getIntent();
        mBLEDeviceItemList = intent.getParcelableArrayListExtra("BLEDevice");

        deviceListAdapter = new BLEDeviceAdapter();
        deviceListview = findViewById(R.id.listDevice);
        deviceListview.setAdapter(deviceListAdapter);

        byte[] new_date = new byte[4];
        byte[] new_flow_capacity = new byte[2];
        byte[] new_accumulated_flow = new byte[2];
        //BluetoothDevice         bluetoothDevice = null;

        for (BLEDeviceItem item:mBLEDeviceItemList){
            deviceListAdapter.addItem (
                    null,
                    item.getDeviceName(),
                    item.getDeviceMac(),
                    new_flow_capacity,
                    new_accumulated_flow,
                    item.getDeviceRSSI(),
                    new_date,
                    item.isConnected());

        }
    }

}