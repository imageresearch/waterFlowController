package com.waterflow.waterFlowController;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    final private String TAG = this.getClass().toString();

    /*public ActivityMainBinding binding;*/
    private static final int PERMISSION_REQUEST_BLE = 1000;

    private BluetoothAdapter    mBluetoothAdapter;
    private Handler             mHandler;
    private BluetoothLeScanner  mLEScanner;
    public  BluetoothDevice     activatedBTDevice = null;

    public boolean mScanning;

    public boolean              bNeedUpdateFirmware = false;

    List<BLEDeviceItem> BLEDeviceItemList = new ArrayList<>();
    private BluetoothGatt mGatt;

    /***************************************************************************
     * Request Bluetooth LE enabling
     *
     * public String DEVICE_SERVICE_STRING         = "";
     * public UUID UUID_SERVICE = UUID.fromString(DEVICE_SERVICE_STRING);
     **************************************************************************/
    private static final int REQUEST_ENABLE_BT = 1;



    public String BLE_DEVICE_TARGET_NAME="FLOW";

    private static final long SCAN_PERIOD = 10000;
    public boolean foundDevices = false;

    public String SERVICE_UUID_STRING = "18424398-7cbc-11e9-8f9e-2a86e4085a59";
    public String CHARACTERSTIC_UUID_STRING = "2d86686a-53dc-25b3-0c4a-f0e10c8dee20";

    public UUID UUID_SERVICE = UUID.fromString(SERVICE_UUID_STRING);
    public UUID UUID_CHARACTERSTIC = UUID.fromString(CHARACTERSTIC_UUID_STRING);

    BluetoothGattCharacteristic mIOCharacteristic = null;



    private int     advertisement_offset = 29;




    ImageView waterflow_status;


    /***************************************************************************
     *
     * @param savedInstanceState
     **************************************************************************/
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

        waterflow_status = findViewById(R.id.waterflow_status);
        checkPermission();
    }


    /***************************************************************************
     *  Called when the permission check request has been done
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     **************************************************************************/
    @SuppressLint("MissingPermission")
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
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.permission_ble_not_connected_alert_title);
                builder.setMessage(R.string.permission_blue_not_connected);
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(dialogInterface -> {

                });

                builder.show();
                return;
            }

            requestBLEActivate();   // Now, Request to scan BLE
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                //mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
                scanLeDevice(true);
                return;
            }

            Toast.makeText(
                        this, "취소했습니다",
                        Toast.LENGTH_LONG).show();
            finish();

        }
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


    /***************************************************************************
     * Initializes a bluetooth adapter on the mobile device.
     * For API level 18 and above, The app gets a reference to BluetoothAdapter
     * through BluetoothManager.
     *
     **************************************************************************/
    private void initializeBluetoothAdapter(){
        if (mHandler != null)
            mHandler = null;    // To re-initialize the handler
        mHandler = new Handler();

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
    }

    @SuppressLint("MissingPermission")
    private void requestBLEActivate(){

        initializeBluetoothAdapter();

        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            scanLeDevice(true);
        }
        else {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(
                    enableBtIntent,
                    REQUEST_ENABLE_BT
            );
        }
    }



    /***************************************************************************
     * show the alert dialog which asks the user to update the filter date or
     * not
     *
     * It'll be called only when the advertisement filed has the default
     * unix timestamp.
     **************************************************************************/
    private void showFilterUpdateAlertDialog(){

        AlertDialog.Builder alertDialogBuilder;
        alertDialogBuilder = new AlertDialog.Builder(this);

        alertDialogBuilder.setTitle(R.string.custom_dialog_title);
        alertDialogBuilder.setMessage(R.string.filter_update_content)
                .setCancelable(false)
                .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        writeCharacteristic(
                                mIOCharacteristic,
                                0xA1,
                                convertUnixTimeStamp2Bytes(getCurrentUnixTimeStamp()));
/*
                        writeCharacteristic(
                                mIOCharacteristic,
                                0xA4, null);
*/
                        finish();
                    }
                })
                .setNegativeButton(R.string.CANCEL, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //  Action for 'NO' Button
                        dialog.cancel();
                        Toast.makeText(getApplicationContext(),"you choose no action for alertbox",
                                Toast.LENGTH_SHORT).show();
                    }
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();

    }

    /***************************************************************************
     * check & request the permission
     *
     **************************************************************************/
    private void checkPermission(){
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
        } else
            requestBLEActivate();   // This case means that all permission
                                    // has been activated so that we need
                                    // no more and just go to request to
                                    // activate BLE
    }

    /***************************************************************************
     *
     * @param days
     * @return
     */
    private int getElapsedMonth(long days){
        return (int)(days / 30);
    }

    private int getElapsedDay(long days){
        return (int)(days % 30);
    }

    private long getElapsedDays(long elapsed_time_stamp){
        return TimeUnit.MILLISECONDS.toDays(elapsed_time_stamp);
    }

    private long getElapsedTimeStamp(long legacy_time_stamp){
        return getCurrentUnixTimeStamp() - legacy_time_stamp;
    }

    /***************************************************************************
     * Get the unix time stamp from bytes
     *
     * the lower-indexed byte is converted into LSB and
     * the higher-indexed byte is converted into MSB
     *
     * @param time_bytes
     * @return
     **************************************************************************/
    private long convertBytesToUnixTimeStamp(byte[] time_bytes){
        return (time_bytes[0]) |
               (time_bytes[1] << 8) |
               (time_bytes[2] << 16) |
               (time_bytes[3] << 24);
    }

    /***************************************************************************
     * Convert the byte arrays into short
     *
     * @param bytes
     * @return
     **************************************************************************/
    private int convertBytesToShort(byte[] bytes){
        return (bytes[0] | (bytes[1] << 8)) & 0xFFFF;
    }

    /***************************************************************************
     * Generate the unix timestamp using the mobile system time clock
     * @return  long unix timestamp formatted
     *
     **************************************************************************/
    private long getCurrentUnixTimeStamp(){
        long temp_time_stamp = System.currentTimeMillis() / 1000L;
        return temp_time_stamp;
    }

    /***************************************************************************
     * Convert the incoming unix timestamp (long) into bytes
     * @param unix_time_stamp       the incoming unix timestamp (long type)
     * @return byte array
     **************************************************************************/
    private byte[] convertUnixTimeStamp2Bytes(long unix_time_stamp){
        byte[] bytes = new byte[4];
        bytes[0] = (byte)((unix_time_stamp & 0x000000FF));
        bytes[1] = (byte)((unix_time_stamp & 0x0000FF00) >> 8);
        bytes[2] = (byte)((unix_time_stamp & 0x00FF0000) >> 16);
        bytes[3] = (byte)((unix_time_stamp & 0xFF000000) >> 24);
        return bytes;
    }

    @SuppressLint("MissingPermission")
    private void scanLeDevice(final boolean enable) {

        if (enable) {
            mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
            mHandler.postDelayed(() -> {
                if (!BLEDeviceItemList.isEmpty()) { // we found the device(s)
/*
                    mScanning = false;
                    mLEScanner.stopScan(mLeScanCallback);
*/
                    mLEScanner.startScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;

            List<ScanFilter> filters = new ArrayList<>();
/*
            ScanFilter scanFilter = new ScanFilter.Builder().setDeviceName("FLOW_.*").build();
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
        } else {
            mScanning = false;
            mLEScanner.stopScan(mLeScanCallback);
        }
    }


    private void UpdateContent(
                    byte[] filter_update_date,
                    byte[] filter_capacity,
                    byte[] accumulated_flow) {

        int filter_capacity_int = convertBytesToShort(filter_capacity); // fixed to 50000
        int accumulated_flow_int = convertBytesToShort(accumulated_flow);
        int left_flow_int = filter_capacity_int - accumulated_flow_int;
        long filter_update_unix_time_stamp = convertBytesToUnixTimeStamp(filter_update_date);
        long elapsed_time_stamp = getElapsedTimeStamp(filter_update_unix_time_stamp);
        long elapsed_days = getElapsedDays(elapsed_time_stamp);
        int elapsed_month = getElapsedMonth(elapsed_days);
        int elapsed_day = getElapsedDay(elapsed_days);

        long left_days = left_flow_int / elapsed_days;
        long left_months = getElapsedMonth(left_days);
        long left_day = getElapsedDay(left_days);

        String formatted_string = getResources().getString(R.string.expectation_date_string);
        Calendar cal = Calendar.getInstance(Locale.getDefault());

        SimpleDateFormat s = new SimpleDateFormat(formatted_string, Locale.getDefault());

        cal.add(Calendar.DAY_OF_YEAR, (int)left_days);

        String st = s.format(cal.getTime());


        TextView expected_date_view = findViewById(R.id.expected_date);
        expected_date_view.setText(st);

        TextView blank_used_month_textview = findViewById(R.id.blank_used_month);
        blank_used_month_textview.setText(String.format("%02d", elapsed_month));

        TextView blank_used_days_textview = findViewById(R.id.blank_used_days);
        blank_used_days_textview.setText(String.format("%02d", elapsed_day));


        TextView blank_used_litters_textview = findViewById(R.id.blank_used_litters);
        blank_used_litters_textview.setText(String.format("%04d", accumulated_flow_int));

        TextView blank_left_months_textview = findViewById(R.id.blank_left_months);
        blank_left_months_textview.setText(String.format("%02d", left_months));

        TextView blank_left_days_textview = findViewById(R.id.blank_left_days);
        blank_left_days_textview.setText(String.format("%02d", left_day));

        TextView blank_left_flow_litter_textview = findViewById(R.id.blank_left_flow_litter);
        blank_left_flow_litter_textview.setText(String.format("%04d", left_flow_int));

        if (left_days < 0){
            TextView expectation_date_string_prefix_textview = findViewById(R.id.expectation_date_string_prefix);
            String first_string = getResources().getString(R.string.passed_expectation_date);
            int modified_left_days = -(int)left_days;
            String day_string = String.format("%d", modified_left_days);
            String finish_string = getResources().getString(R.string.passed_expectation_days);
            expectation_date_string_prefix_textview.setText(first_string + " " +  day_string + finish_string);
            expected_date_view.setText(R.string.request_direct_change);

            Glide
                    .with(this)
                    .load(R.drawable.short_water_flow)
                    .skipMemoryCache(true)
                    .into(waterflow_status);

        } else {
            Glide
                    .with(this)
                    .load(R.drawable.normal_water_flow)
                    .skipMemoryCache(true)
                    .into(waterflow_status);

        }

    }


    private void notifyDisconnected(){
        Toast.makeText (
                MainActivity.this,
                R.string.ble_not_connected,
                Toast.LENGTH_SHORT).show();
    }


    /***************************************************************************
     *
     **************************************************************************/
    private void notifyConnected(){
        Toast.makeText (
                MainActivity.this,
                R.string.ble_connected,
                Toast.LENGTH_SHORT).show();

        byte[] filterDateBytes = BLEDeviceItemList.get(0).getFilterDateBytes();
        byte[] filter_capacity = BLEDeviceItemList.get(0).getFilterCapacity();
        byte[] accumulated_flow = BLEDeviceItemList.get(0).getWaterCapacity();

        if (needToUpdateFirmware(filterDateBytes))
            showFilterUpdateAlertDialog();

        UpdateContent(
                filterDateBytes,
                filter_capacity,
                accumulated_flow
        );



    }

    private boolean needToUpdateFirmware(byte[] dateBytes){
        if (dateBytes[0] == 0x00 && dateBytes[1] == 0x00 &&
            dateBytes[2] == 0x00 && dateBytes[3] == 0x00)
            return true;
        else if (dateBytes[0] == 0xFF && dateBytes[1] == 0xFF &&
                 dateBytes[2] == 0xFF && dateBytes[3] == 0xFF)
            return true;

        return false;
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

            if (deviceName != null && deviceName.substring(0, 4).equals(BLE_DEVICE_TARGET_NAME)) {
                String deviceMac = btDevice.getAddress();

                Log.i ("Device", "Name :" + deviceName);
                Log.i ("Device", "Mac : " + deviceMac);

                byte[] scanRecord = result.getScanRecord().getBytes();

                byte[] date_bytes = new byte[4];
                byte[] flow_capacity = new byte[2];
                byte[] accumulated_flow = new byte[2];

                System.arraycopy(
                        scanRecord,
                        advertisement_offset,
                        date_bytes,
                        0,
                        4
                );

                System.arraycopy(
                        scanRecord,
                        advertisement_offset + 4,
                        flow_capacity,
                        0,
                        2
                );

                System.arraycopy(
                        scanRecord,
                        advertisement_offset + 6,
                        accumulated_flow,
                        0,
                        2
                );

                Log.i (
                        "BLE", "Time Stamp Byte : " +
                                date_bytes[0] + " " +
                                date_bytes[1] + " " +
                                date_bytes[2] + " " +
                                date_bytes[3]);


                if (BLEDeviceItemList.isEmpty()){
                    BLEDeviceItem newBLEDeviceItem = new BLEDeviceItem(
                            deviceName,
                            btDevice.getAddress(),
                            flow_capacity,
                            accumulated_flow,
                            date_bytes,
                            result.getRssi(),
                            true);
                    BLEDeviceItemList.add(newBLEDeviceItem);
                    connectToDevice(btDevice);  // we connect only to the first one found.

                } else {

                    for (BLEDeviceItem item : BLEDeviceItemList) {
                        if (item.getDeviceMac().equals(deviceMac)) {    // we found the same device
                                                                        // and we update the field values

                            item.setFilter_capacity(flow_capacity);
                            item.setFilter_date(date_bytes);
                            item.setAccumulated_flow(accumulated_flow);
                            return;
                        }
                    }

                    BLEDeviceItem newBLEDeviceItem = new BLEDeviceItem(
                            deviceName,
                            btDevice.getAddress(),
                            flow_capacity,
                            accumulated_flow,
                            date_bytes,
                            result.getRssi(),
                            false);
                    BLEDeviceItemList.add(newBLEDeviceItem);

                }
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

        activatedBTDevice = deviceToConnect;

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
                        mIOCharacteristic = characteristic;
                        mHandler.post(() -> notifyConnected());

                    }
                }
            }
        }


        @SuppressLint("MissingPermission")
        @Override
        public void onCharacteristicRead(
                BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic,
                int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            Log.i("onCharacteristicRead", characteristic.toString());

            byte[] newValue = characteristic.getValue();

            if (newValue == null) {
                Log.e(TAG, "Null Data Received.... Check the H/W");
                return;
            }
        }


        /***********************************************************************
         * Called when the write process has been finished.
         * we should read the corresponding characteristic to get the real
         * device response.
         * Note that the callback for readCharactersitci will be called
         *
         * @param gatt
         * @param characteristic
         * @param status
         **********************************************************************/
        @SuppressLint("MissingPermission")
        @Override
        public void onCharacteristicWrite(
                BluetoothGatt               gatt,
                BluetoothGattCharacteristic characteristic,
                int                         status) {
            super.onCharacteristicWrite(
                    gatt,
                    characteristic,
                    status);
            if (status == BluetoothGatt.GATT_SUCCESS)
                mGatt.readCharacteristic(characteristic);
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
     * Write the data with the command to a specific characteristic
     * When finishing writing the data, the callback (onCharacteristicWrite)of
     * BluetoothGattCallback will be called
     * @param characteristic
     * @param command
     * @param data
     **************************************************************************/
    @SuppressLint("MissingPermission")
    public void writeCharacteristic(
                        BluetoothGattCharacteristic characteristic,
                        int     command,
                        byte[]  data) {

        if (mGatt == null)
            return;

        byte[] totalBytes = new byte[1 + ((data != null && data.length != 0)? data.length:0)];
        totalBytes[0] = (byte) command;

        if (data != null && data.length != 0) {
            System.arraycopy(data, 0, totalBytes, 1, data.length);
        }


        mIOCharacteristic.setValue(totalBytes);
        mGatt.writeCharacteristic(mIOCharacteristic);

    }

}