package com.waterflow.waterFlowController;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;


import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    final private String TAG = this.getClass().toString();

    /*public ActivityMainBinding binding;*/
    private static final int PERMISSION_REQUEST_BLE = 1000;

    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;
    private BluetoothLeScanner mLEScanner;
    public boolean mScanning;

    List<BLEDeviceItem> BLEDeviceItemList = new ArrayList<>();
    private BluetoothGatt mGatt;

    /***************************************************************************
     * Request Bluetooth LE enabling
     *
     * public String DEVICE_SERVICE_STRING         = "";
     * public UUID UUID_SERVICE = UUID.fromString(DEVICE_SERVICE_STRING);
     **************************************************************************/
    private static final int REQUEST_ENABLE_BT = 1;



    public String BLE_DEVICE_TARGET_NAME="FLOW_";

    private static final long SCAN_PERIOD = 10000;
    public String DEVICE_WRITE_STRING = "5c3a659e-897e-45e1-b016-007107c96df7";
    public String NOTIFY_CHARACTERISTIC_STRING = "5c3a659e-897e-45e1-b016-007107c96df8";
    public String NOTIFY_DESCRIPTOR_STRING = "00002902-0000-1000-8000-00805f9b34fb";
    public String SERVICE_UUID_STRING = "18424398-7cbc-11e9-8f9e-2a86e4085a59";
    public String CHARACTERSTIC_UUID_STRING = "2d86686a-53dc-25b3-0c4a-f0e10c8dee20";


    public UUID UUID_CHARACTERSTIC = UUID.fromString(CHARACTERSTIC_UUID_STRING);
    public UUID UUID_SERVICE = UUID.fromString(SERVICE_UUID_STRING);
    public UUID UUID_DEVICE_WRITE = UUID.fromString(DEVICE_WRITE_STRING);
    public UUID UUID_NOTIFY_CHARACTER = UUID.fromString(NOTIFY_CHARACTERISTIC_STRING);
    public UUID UUID_NOTIFY_DESCRIPTOR = UUID.fromString(NOTIFY_DESCRIPTOR_STRING);

    BluetoothGattCharacteristic mControlCharacteristic = null;
    BluetoothGattCharacteristic mNotificationCharacteristic;

    private int     advertisement_offset = 29;

    private Date filter_date;
    private short filter_capacity;
    private short water_capacity;



    ImageView waterflow_status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(
                    this,
                    R.string.ble_not_supporting,
                    Toast.LENGTH_SHORT).show();
            finish();
        }

        mHandler = new Handler();


        // Initializes a Bluetooth adapter.
        // For API level 18 and above, get a reference to BluetoothAdapter
        // through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);


        mBluetoothAdapter = bluetoothManager.getAdapter();





        waterflow_status = findViewById(R.id.waterflow_status);
        Glide
            .with(this)
            .load(R.drawable.normal_water_flow)
            .skipMemoryCache(true)
            .into(waterflow_status);


    }



    @Override
    public void onStart(){
        super.onStart();

        String[] permissions;


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions = new String[]{
                    //Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
            };
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            permissions = new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION

            };

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions = new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };
        } else {
            permissions = new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };
        }

        checkPermission(permissions);

        scanLeDevice(true);
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(
                R.menu.waterflow_menu, menu
        );
        return true;
    }


    /***************************************************************************
     *
     * @param item
     * @return
     **************************************************************************/
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_setting) {
            Intent intent = new Intent(
                    this,
                    DeviceScanActivity.class);
            intent.putParcelableArrayListExtra(
                    "BLEDevice",
                    (ArrayList<? extends Parcelable>) BLEDeviceItemList);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults);

        if (requestCode == PERMISSION_REQUEST_BLE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // Checks if Bluetooth is supported on the device.
                if (mBluetoothAdapter == null ||
                        !mBluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(
                                                                        this,
                                                                        Manifest.permission.BLUETOOTH_CONNECT)) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    startActivityForResult(
                            enableBtIntent,
                            REQUEST_ENABLE_BT
                    );
                } else {
                    mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
                }

//                scanLeDevice(true);

            } else {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("권한 제한");
                builder.setMessage(
                        "위치 정보 및 액세스 권한이 허용되지 않아서 Bluetooth를 검색 및 연결할 수 없습니다.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(dialogInterface -> {

                });
                builder.show();
            }
        }

    }


    /***************************************************************************
     * check & request the permission
     *
     **************************************************************************/
    private void checkPermission(String[] permissions){
        ArrayList<String> permissionList = new ArrayList<>();

        for (String permission : permissions) {
            int result = ContextCompat.checkSelfPermission(this, permission);
            if (result != PackageManager.PERMISSION_GRANTED)
                permissionList.add(permission);
        }

        if (permissionList.size() != 0) {
            int listSize = permissionList.size();
            String[] arr = permissionList.toArray(new String[listSize]);
            ActivityCompat.requestPermissions(
                    this,
                    arr,
                    PERMISSION_REQUEST_BLE);


        }


    }



    private void scanLeDevice(final boolean enable) {

        if (enable) {
            mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
            mHandler.postDelayed(() -> {
                mScanning = false;
                if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(
                                                                    this,
                                                                            Manifest.permission.BLUETOOTH_SCAN)) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                mLEScanner.stopScan(mLeScanCallback);
            }, SCAN_PERIOD);

            mScanning = true;

            List<ScanFilter> filters = new ArrayList<>();

/*
            ScanFilter scanFilter = new ScanFilter.
                    Builder().
                    setDeviceName(BLE_DEVICE_TARGET_NAME).
                    build();
            filters.add(scanFilter);
*/

            ScanSettings scanSettings = new ScanSettings.
                    Builder().
                    setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).
                    build();

            mLEScanner.startScan(
                    filters,
                    scanSettings,
                    mLeScanCallback);
/*

            mLEScanner.startScan(mLeScanCallback);
*/
        } else {
            mScanning = false;
            mLEScanner.stopScan(mLeScanCallback);
        }
    }


    private void notifyDisconnected(){
        Toast.makeText (
                MainActivity.this,
                R.string.ble_not_connected,
                Toast.LENGTH_SHORT).show();
    }


    private void notifyConnected(){
        Toast.makeText (
                MainActivity.this,
                R.string.ble_connected,
                Toast.LENGTH_SHORT).show();
    }

    private void showDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        builder.setNegativeButton(R.string.CANCEL, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }





    /***************************************************************************
     * Scan callback
     **************************************************************************/
    @SuppressLint("MissingPermission")
    private final ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice btDevice = result.getDevice();
            String deviceName = btDevice.getName();
            String deviceMac = btDevice.getAddress();
            Log.i ("Device", "Name :" + deviceName);
            Log.i ("Device", "Mac : " + deviceMac);


            boolean bAlreadyRegistered = false;

            for (BLEDeviceItem item:BLEDeviceItemList) {
                if (item.getDeviceMac().equals(deviceMac)) {
                    bAlreadyRegistered = true;
                    break;
                }
            }

            if (bAlreadyRegistered == false) {
                BLEDeviceItem newBLEDeviceItem = new BLEDeviceItem(
                        btDevice.getName(),
                        btDevice.getAddress(),
                        result.getRssi(),
                        false);

                BLEDeviceItemList.add(newBLEDeviceItem);
            }

            if (deviceName != null && deviceName.substring(0, 5).equals(BLE_DEVICE_TARGET_NAME)) {
                // Get the advertisement packet
                byte[] scanRecord = result.getScanRecord().getBytes();
                long unix_time =    (scanRecord[advertisement_offset] |
                                    (scanRecord[advertisement_offset + 1] << 8) |
                                    (scanRecord[advertisement_offset + 2] << 16) |
                                    (scanRecord[advertisement_offset+ 3] << 24));
                Date date = new java.util.Date(Long.parseLong(String.valueOf(unix_time)));

                filter_capacity = (short)(scanRecord[advertisement_offset + 4] + (scanRecord[advertisement_offset+5] << 8));
                water_capacity = (short)(scanRecord[advertisement_offset + 6] + (scanRecord[advertisement_offset + 7] << 8));
                mHandler.post(() -> showDialog());
                connectToDevice(btDevice);
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e ("Scan Failed", "Error Code :" + errorCode);
        }
    };


    @SuppressLint("MissingPermission")
    public void connectToDevice(BluetoothDevice deviceToConnect){
        if (mGatt != null)
            mGatt = null;

        mGatt = deviceToConnect.connectGatt(
                    this,
                    true,
                    gattCallback);

        for (BLEDeviceItem bleDeviceItem:BLEDeviceItemList) {

            String device_name = bleDeviceItem.getDeviceName();
            String device_name_to_connect = deviceToConnect.getName();
            if (device_name != null){
                if (device_name.equals(device_name_to_connect))
                    bleDeviceItem.setConnected(true);
            }
        }

        scanLeDevice(false);    // will stop after first device detection
    }

    private BluetoothAdapter.LeScanCallback ScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
            Log.i (TAG, "Hahahaha");
        }
    };


    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            super.onPhyUpdate(gatt, txPhy, rxPhy, status);
        }

        @Override
        public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            super.onPhyRead(gatt, txPhy, rxPhy, status);
        }


        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(
                BluetoothGatt       gatt,
                int                 status,
                int                 newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.i ("onConnectionStateChange", "Status : " + status);
            switch (newState){
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i ("gattCallback", "STATE_CONNECTED");
                    gatt.discoverServices(); // Now, try to find the services
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");

                    for (BLEDeviceItem deviceItem:BLEDeviceItemList){

                        if (mGatt.getDevice().getAddress().equals(deviceItem.deviceMac) &&
                                mGatt.getDevice().getName().equals(deviceItem.deviceName))
                            deviceItem.setConnected(false);
                    }


                    mHandler.post(() -> notifyDisconnected());
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if (status != BluetoothGatt.GATT_SUCCESS)
                return;


            List<BluetoothGattService> services = gatt.getServices();
            Log.i("onServicesDiscovered", services.toString());



            for (BluetoothGattService service: services){
                Log.i("Service UUID : ", service.getUuid().toString());
                if (!service.getUuid().toString().equals(SERVICE_UUID_STRING))
                    continue;   // Try to find the valid service only

                List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();

                for (BluetoothGattCharacteristic characteristic: characteristics){
                    Log.i("Characteristic UUID :", characteristic.getUuid().toString());

                    if (characteristic.getUuid().toString().equals(CHARACTERSTIC_UUID_STRING)) {
                        mControlCharacteristic = characteristic;
                        mHandler.post(() -> notifyConnected());

                    } else if (characteristic.getUuid().equals(UUID_NOTIFY_CHARACTER)) {

                        mNotificationCharacteristic = characteristic;
                        updateCharacteristicNotification(
                                mNotificationCharacteristic,
                                true);

                    }
                }
            }


            if (mControlCharacteristic != null){
                mHandler.postDelayed(() -> {
                    notifyConnected();
                }, 2);
            }
        }


        @SuppressLint("MissingPermission")
        @Override
        public void onCharacteristicRead(
                BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic,
                int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            if (!characteristic.getUuid().equals(UUID_DEVICE_WRITE))
                return;

            Log.i("onCharacteristicRead", characteristic.toString());


            byte[] newValue = characteristic.getValue();



            if (newValue == null) {
                Log.e(TAG, "Null Data Received.... Check the H/W");
                return;
            }
/*

            if (bInWriteMode) {
*/
                mHandler.postDelayed(() -> {
/*
                    bInWriteMode = false;
*/

                }, 10);

            mControlCharacteristic.setValue(newValue);
            mControlCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            mGatt.writeCharacteristic(mControlCharacteristic);
        }

        /***********************************************************************
         * Called when the write process has finished.
         **********************************************************************/
        @SuppressLint("MissingPermission")
        @Override
        public void onCharacteristicWrite(
                BluetoothGatt               gatt,
                BluetoothGattCharacteristic characteristic,
                int                         status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.d(
                    TAG,
                    "Characteristic " +
                            characteristic.getUuid() + "written");

            if (!characteristic.getUuid().equals(UUID_NOTIFY_CHARACTER))
                mGatt.readCharacteristic(mControlCharacteristic);
        }

        /***********************************************************************
         * Called when you are trying to send data using writeCharacteristic
         * and the BLE device responds with some value.
         *
         **********************************************************************/
        @Override
        public void onCharacteristicChanged(
                BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            if (!characteristic.getUuid().equals(UUID_NOTIFY_CHARACTER))
                return;
        }

        /***********************************************************************
         * Used to read the configuration settings for the BLE device.
         * Some manufactures might require to send some data to the BLE device
         * and acknowledge it by reading,
         * before you can connect to the BLE device.
         *
         **********************************************************************/
        @Override
        public void onDescriptorRead(
                BluetoothGatt gatt,
                BluetoothGattDescriptor descriptor,
                int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(
                BluetoothGatt           gatt,
                BluetoothGattDescriptor descriptor,
                int                     status) {
            super.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
        }

        @Override
        public void onServiceChanged(@NonNull BluetoothGatt gatt) {
            super.onServiceChanged(gatt);
        }
    };


    /***************************************************************************
     * After this function call, the onCharacteristicWrite will be called.
     * Please refer to onCharacteristicWrite
     *
     **************************************************************************/
    @SuppressLint("MissingPermission")
    public void writeCharacteristic(
            int                         newType,
            int                         data) {

        if (mGatt == null)
            return;

        mGatt.readCharacteristic(mControlCharacteristic);

    }


    @SuppressLint("MissingPermission")
    public void updateCharacteristicNotification (
            BluetoothGattCharacteristic characteristic,
            boolean                     enabled){

        if (mGatt == null){
            Log.w (TAG, "BluetoothGatt not initialized");
            return;
        }

        if (characteristic.getUuid().equals(UUID_NOTIFY_CHARACTER)) {

            mGatt.setCharacteristicNotification(characteristic, enabled);
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID_NOTIFY_DESCRIPTOR);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mGatt.writeDescriptor(descriptor);
        }
    }



    BluetoothLeAdvertiser advertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();





}