package com.waterflow.waterFlowController;

import androidx.appcompat.app.AppCompatActivity;

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


        for (BLEDeviceItem item:mBLEDeviceItemList){
            deviceListAdapter.addItem (
                    item.getDeviceName(),
                    item.getDeviceMac(),
                    item.getDeviceRSSI(),
                    item.isConnected());

        }
    }

}