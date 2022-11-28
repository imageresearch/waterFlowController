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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    final private String TAG = this.getClass().toString();

    /*public ActivityMainBinding binding;*/
    private static final int PERMISSION_REQUEST_BLE = 1000;

    private BluetoothAdapter    mBluetoothAdapter = null;
    private Handler             mHandler = null;


    List<BLEDeviceItem>         BLEDeviceItemList = new ArrayList<>();


    /***************************************************************************
     * Request Bluetooth LE enabling
     *
     * public String DEVICE_SERVICE_STRING         = "";
     * public UUID UUID_SERVICE = UUID.fromString(DEVICE_SERVICE_STRING);
     **************************************************************************/
    private static final int REQUEST_ENABLE_BT = 1;

    public String SERVICE_UUID_STRING = "18424398-7cbc-11e9-8f9e-2a86e4085a59";
    public String CHARACTERISTIC_UUID_STRING = "2d86686a-53dc-25b3-0c4a-f0e10c8dee20";


    /***************************************************************************
     *
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

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        checkPermission();

        mHandler = new Handler();       // Initialize the message handler
        if (mHandler == null){
            Log.e (TAG, "Fail to initialize the message handler");
            finish();
        }

        requestBLEActivate();
        initializeContent();
    }

    @Override
    public void onResume(){
        Log.d (TAG, "MainActivity Resumed");
        requestBLEActivate();
        super.onResume();
    }

    @Override
    public void onPause(){
        Log.d (TAG, "MainActivity Paused");
        scanLeDevice(false);
        super.onPause();
    }


    /***************************************************************************
     *  Called when the permission check request has been done
     *
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


    /***************************************************************************
     *
     *
     * @param requestCode
     * @param resultCode
     * @param data
     **************************************************************************/
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                scanLeDevice(true);
                return;
            }
            else
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
     **************************************************************************/
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        showFilterUpdateAlertDialog();
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("MissingPermission")
    private void requestBLEActivate(){

        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            scanLeDevice(true);
        }
        else {

            AlertDialog.Builder alertDialogBuilder;
            alertDialogBuilder = new AlertDialog.Builder(this);

            alertDialogBuilder.setTitle(R.string.permission_ble_not_connected_alert_title);
            alertDialogBuilder
                    .setMessage(R.string.permission_blue_not_connected)
                    .setCancelable(false)
                    .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            startActivityForResult(
                                    enableBtIntent,
                                    REQUEST_ENABLE_BT);
                        }
                    })
                    .setNegativeButton(R.string.CANCEL, (dialog, id) -> {
                        finish();
                    });


            final AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();


        }
    }



    /***************************************************************************
     * show the alert dialog which asks the user to update the filter date or
     * not
     *
     * It'll be called only when the advertisement filed has the default
     * unix timestamp.
     **************************************************************************/
    public void showFilterUpdateAlertDialog(){

        scanLeDevice(false);        // we should stop BLE scan first

        AlertDialog.Builder alertDialogBuilder;
        alertDialogBuilder = new AlertDialog.Builder(this);

        alertDialogBuilder.setTitle(R.string.custom_dialog_title);
        alertDialogBuilder
                .setMessage(R.string.filter_update_content)
                .setCancelable(true)
                .setPositiveButton(R.string.OK, (dialog, id) -> connectToDevice(
                        BLEDeviceItemList.get(0).getBluetoothDevice()))
                .setNegativeButton(R.string.CANCEL, (dialog, id) -> {
                    scanLeDevice(true); // we should re-start the scan
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


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            permissions = new String[]{
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
     * Warning
     *  The parameter of timestamp should be in milli-seconds
     **************************************************************************/
    private Date convertTimestampToDate(long timestamp){
        return new Date(timestamp * 1000);
    }


    private long convertBytesToLong (byte[] bytes){
        long returnValue = 0;
        if (bytes != null) {
            for (int count = 0; count < bytes.length; count++)
                returnValue += (long) (bytes[count] & 0xFF) << (8 * count);
        }

        return returnValue;

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

    private Date getCurrentDate(){
        return Calendar.getInstance().getTime();
    }


    private LocalDate convertToLocalDate(Date dateToConvert){
        return dateToConvert.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private int getElapsedDays(Date deviceDate, Date mobileDate, int type){
        int returnVal;
        Period period;

        period = Period.between(
                    convertToLocalDate(deviceDate),
                    convertToLocalDate(mobileDate));

        switch(type){
            case 0 :
            default :
                returnVal = period.getDays();
                break;
            case 1 :
                returnVal = period.getYears();
                returnVal = period.getMonths() + returnVal * 12;
                break;
        }
        return returnVal;
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

    public void UpdateContent(
                    byte[] filter_update_date,
                    byte[] filter_capacity,
                    byte[] accumulated_flow) {

        int filter_capacity_int = (int)convertBytesToLong(filter_capacity); // fixed to 50000
        int accumulated_flow_int = (int)convertBytesToLong(accumulated_flow);
        int left_flow_int = filter_capacity_int - accumulated_flow_int;
        long filter_update_unix_time_stamp = convertBytesToLong(filter_update_date);

        Date filter_update_date_fmt = convertTimestampToDate(filter_update_unix_time_stamp);
        Date current_date = getCurrentDate();

        long elapsed_days = getElapsedDays(filter_update_date_fmt, current_date, 0);
        long elapsed_months = getElapsedDays(filter_update_date_fmt, current_date, 1);


        // To prevent the zero-dividing problem, we set the elapsed days to 1 if it is 0
        long left_days = (left_flow_int <= 0)? 0: left_flow_int / ((elapsed_days == 0)? 1:elapsed_days);

        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.add(Calendar.DATE, (int)left_days);
        Date resultDate = new Date(cal.getTimeInMillis());

        long left_months = getElapsedDays(filter_update_date_fmt, resultDate, 1);
        left_days = getElapsedDays(filter_update_date_fmt, resultDate, 0);

        String formatted_string = getResources().getString(R.string.expectation_date_string);
        SimpleDateFormat s = new SimpleDateFormat(formatted_string, Locale.getDefault());

        String st = s.format(cal.getTime());


        TextView expected_date_view = findViewById(R.id.expected_date);
        expected_date_view.setText(st);

        String month = getResources().getString(R.string.months);
        String days = getResources().getString(R.string.days);
        String litters = getResources().getString(R.string.litter);
        String merged_used_days = String.format("%02d", elapsed_months) + month + " " + String.format("%02d", elapsed_days) + days;

        TextView used_days_textview = findViewById(R.id.used_days);
        used_days_textview.setText(merged_used_days);

        String merged_left_days = String.format("%02d", left_months) + month + " " + String.format("%02d", left_days) + days;
        TextView left_days_textview = findViewById(R.id.left_days);
        left_days_textview.setText(merged_left_days);

        String merged_used_litters = String.format("%04d", accumulated_flow_int) + litters;
        TextView used_litters_textview = findViewById(R.id.used_litters);
        used_litters_textview.setText(merged_used_litters);

        String merged_left_litters = String.format("%04d", left_flow_int) + litters;
        TextView left_litters_textview = findViewById(R.id.left_litters);
        left_litters_textview.setText(merged_left_litters);

        if (left_days <= 0){
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
            .into((ImageView)findViewById(R.id.waterflow_status));

        }

    }

    /***************************************************************************
     *
     **************************************************************************/
    private void initializeContent(){
        Glide
        .with(this)
        .load(R.drawable.normal_water_flow)
        .skipMemoryCache(true)
        .into((ImageView)findViewById(R.id.waterflow_status));
    }


    /***************************************************************************
     * Called when the BLE connection has been disconnected.
     *
     **************************************************************************/
    private void notifyDisconnected(boolean bDisplayMessage){
        if (bDisplayMessage)
            Toast.makeText (
                MainActivity.this,
                R.string.ble_not_connected,
                Toast.LENGTH_SHORT).show();

        scanLeDevice(true); // when all BLEs are disconnected,
                                   // we should try to rescan
    }


    /***************************************************************************
     *  This function is called only when the device is connected and
     *  the device is connected only when the firmware needs to be updated.
     **************************************************************************/
    @SuppressLint("MissingPermission")
    private void notifyConnected(
                        BluetoothGatt               gatt,
                        BluetoothGattCharacteristic characteristic,
                        boolean                     bDisplayMessage){
        if (bDisplayMessage)
            Toast.makeText (
                MainActivity.this,
                R.string.ble_connected,
                Toast.LENGTH_SHORT).show();

        writeCharacteristic(
                gatt,
                characteristic,
                0xA1,
                convertUnixTimeStamp2Bytes(getCurrentUnixTimeStamp()));
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



    @SuppressLint("MissingPermission")
    public void connectToDevice(
            BluetoothDevice     deviceToConnect){

        if (deviceToConnect == null){
            Log.e (TAG, "Device to be connected is not specified");
            return;
        }

        scanLeDevice(false);

        BluetoothGatt gatt = deviceToConnect.connectGatt(
                            this,
                            true,
                            gattCallback);

        if (gatt == null) {
            Log.e (TAG, "Fail to connect the GATT.");
            return;
        }


        for (BLEDeviceItem bleDeviceItem:BLEDeviceItemList) {

            String device_name = bleDeviceItem.getDeviceName();
            String device_name_to_connect = deviceToConnect.getName();
            if (device_name != null){
                if (device_name.equals(device_name_to_connect))
                    bleDeviceItem.setConnected(true);
            }
        }
    }

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
            BluetoothGatt               gatt,
            BluetoothGattCharacteristic characteristic,
            int                         command,
            byte[]                      data) {

        if (gatt == null) {
            Log.e (TAG, "GATT is not specified. Check it.");
            return;
        }

        if (characteristic == null){
            Log.e (TAG, "No Characteristic has been specified.");
            return;
        }

        byte[] totalBytes = new byte[1 + ((data != null && data.length != 0)? data.length:0)];
        totalBytes[0] = (byte) command;

        if (data != null && data.length != 0)
            System.arraycopy(data, 0, totalBytes, 1, data.length);

        characteristic.setValue(totalBytes);
        gatt.writeCharacteristic(characteristic);
    }



    @SuppressLint("MissingPermission")
    private void scanLeDevice(final boolean enable) {

        BluetoothLeScanner  mLEScanner = null;

        if (mBluetoothAdapter == null){
            Log.e (TAG, "Bluetooth Adapter is not specified. Check it.");
            return;
        }

        mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (mLEScanner == null){
            Log.e (TAG, "Fail to get the bluetooth scanner");
            return;
        }

        if (!enable){
            mLEScanner.stopScan(mLeScanCallback);
            mLEScanner.flushPendingScanResults(mLeScanCallback);
            Log.i(TAG, "=============> Stop scanning BLE devices");
            return;
        }

        List<ScanFilter> filters = new ArrayList<>();
        ScanSettings scanSettings = new ScanSettings.
                Builder().
                setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).
                setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).
                setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES).
                setReportDelay(0).
                build();

        mLEScanner.startScan(
                filters,
                scanSettings,
                mLeScanCallback);
        Log.i(TAG, "=============> Start Scanning BLE devices ");
    }


    /***************************************************************************
     * BLE GATT Callback
     *
     *
     **************************************************************************/
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onPhyUpdate(
                        BluetoothGatt   gatt,
                        int             txPhy,
                        int             rxPhy,
                        int             status) {
            super.onPhyUpdate(gatt, txPhy, rxPhy, status);
        }

        @Override
        public void onPhyRead(
                        BluetoothGatt   gatt,
                        int             txPhy,
                        int             rxPhy,
                        int             status) {
            super.onPhyRead(gatt, txPhy, rxPhy, status);
        }


        /***********************************************************************
         * Called when the connection status has changed
         *
         * @param gatt          The GATT
         * @param status        The previous status
         * @param newState      the new status
         **********************************************************************/
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(
                BluetoothGatt       gatt,
                int                 status,
                int                 newState) {
            super.onConnectionStateChange(gatt, status, newState);

            final String GATT_TAG = "GATT";
            Log.i (
                    GATT_TAG,
                    "Connection State changed (Previous Status : " +
                            status +
                            " New Status : " +
                            newState +
                            ")");

            switch (newState){
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i (GATT_TAG, "(STATE_CONNECTED)");
                    gatt.discoverServices();    // Now, try to find the services
                                                // Don't notify that we've connected
                                                // the BLE device yet.
                                                // It's will be notified after
                                                // all characteristics we need are
                                                // found
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.i(GATT_TAG, "(STATE_DISCONNECTED)");

                    for (BLEDeviceItem deviceItem:BLEDeviceItemList){

                        if (gatt.getDevice().getAddress().equals(deviceItem.deviceMac) &&
                            gatt.getDevice().getName().equals(deviceItem.deviceName))
                            deviceItem.setConnected(false);
                    }
                    mHandler.post(() -> notifyDisconnected(false));
                    break;

                default:
                    Log.i(GATT_TAG, "(Some other state)");
                    break;
            }
        }

        /***********************************************************************
         *  Called when a service has been discovered
         *
         * @param gatt      The GATT
         * @param status    The status when a new service has been discovered.
         **********************************************************************/
        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            final String GATT_TAG = "GATT";

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e (GATT_TAG, "Fail to discover the service");
                return;
            }

            List<BluetoothGattService> services = gatt.getServices();

            for (BluetoothGattService service: services){
                Log.d(
                    GATT_TAG,
                    "Service UUID : " + service.getUuid().toString());
                if (!service.getUuid().toString().equals(SERVICE_UUID_STRING))
                    continue;   // Try to find the valid service only

                Log.i(
                    GATT_TAG,
                    "==> Found the service we want (" + service.getUuid().toString() + ")");

                List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                for (BluetoothGattCharacteristic characteristic: characteristics){
                    Log.d(GATT_TAG, "Characteristic UUID : " + characteristic.getUuid().toString());

                    if (characteristic.getUuid().toString().equals(CHARACTERISTIC_UUID_STRING)) {
                        Log.i(
                            GATT_TAG,
                            "==> Found the characteristic we want (" + characteristic.getUuid().toString() + ")");
                        mHandler.post(() -> notifyConnected(gatt, characteristic, false));
                    }
                }
            }
        }


        /***********************************************************************
         *  Notes
         *      After receiving the ACK response from the target device,
         *      we'll disconnect the GATT and sequentially the callback for
         *      the connection state will be called since its status has
         *      changed.
         *      Therefore, we don't have to call notifyDisconnected directly.
         *
         * @param gatt              The GATT handler
         * @param characteristic    The characteristic
         * @param status            The new status
         **********************************************************************/
        @SuppressLint("MissingPermission")
        @Override
        public void onCharacteristicRead(
                BluetoothGatt               gatt,
                BluetoothGattCharacteristic characteristic,
                int                         status) {

            super.onCharacteristicRead(gatt, characteristic, status);

            Log.d("onCharacteristicRead", characteristic.toString());

            byte[] newValue = characteristic.getValue();

            if (newValue == null) {
                Log.e(TAG, "Null Data Received.... Check the H/W");
                return;
            }

            if (characteristic.getUuid().toString().equals(CHARACTERISTIC_UUID_STRING)) {
                switch(newValue[0]){
                    case (byte)0xA1 :
                        switch (newValue[1]){
                            case (byte)0x1 :
                                gatt.disconnect(); // see the comment
                                break;
                            default :
                                // Todo : Add what to do
                                //  when we've got the failure
                                break;
                        }
                        break;
                    case (byte)0xA4 :
                        switch (newValue[1]){
                            case (byte)0x01 :
                                gatt.disconnect(); // see the comment
                                break;

                            default :
                                // Todo : Add what to do
                                //  when we've got the failure
                                break;
                        }
                }
            }
        }


        /***********************************************************************
         * Called when the write process has been finished.
         * we should read the corresponding characteristic to get the real
         * device response.
         * Note that the callback for readCharacteristic will be called
         *
         * @param gatt              The GATT
         * @param characteristic    The characteristic
         * @param status            The status
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
            final String GATT_TAG = "GATT";
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e (GATT_TAG, "Fail to write data on Characteristic.");
                return;
            }

            Log.d (GATT_TAG, "Success to write data on Characteristic.");
            Log.i (GATT_TAG, "Now, we try to read the characteristic.");
            //mGatt.readCharacteristic(characteristic);
            gatt.readCharacteristic(characteristic);
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
     * Scan callback
     **************************************************************************/
    @SuppressLint("MissingPermission")
    private final ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice btDevice = result.getDevice();
            String deviceName = btDevice.getName();

            final String BLE_DEVICE_TARGET_NAME="FLOW";

            if (deviceName != null &&
                    deviceName.length() >= 5 &&
                    deviceName.substring(0, 4).equals(BLE_DEVICE_TARGET_NAME)) {

                String deviceMac = btDevice.getAddress();
                Log.d ("Scan Callback", "Found device : " + deviceName +"(Name) " + deviceMac + "(MAC)");

                byte[] scanRecord = result.getScanRecord().getBytes();

                byte[] date_bytes = new byte[4];
                byte[] flow_capacity = new byte[2];
                byte[] accumulated_flow = new byte[2];

                int advertisement_offset = 29;
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

                Log.d (
                        "Scan Callback", "(Time Stamp Byte : [" +
                                date_bytes[0] + " " +
                                date_bytes[1] + " " +
                                date_bytes[2] + " " +
                                date_bytes[3] + "], " +
                                "Flow Capacity : [" +
                                flow_capacity[0] + " " +
                                flow_capacity[1] + "], " +
                                "Accumulated Used Flow Amount : [" +
                                accumulated_flow[0] + " " +
                                accumulated_flow[1] + "])");


                if (BLEDeviceItemList.isEmpty()){
                    BLEDeviceItem newBLEDeviceItem = new BLEDeviceItem(
                            btDevice,
                            deviceName,
                            btDevice.getAddress(),
                            flow_capacity,
                            accumulated_flow,
                            date_bytes,
                            result.getRssi(),
                            true);
                    BLEDeviceItemList.add(newBLEDeviceItem);

                    if (needToUpdateFirmware(date_bytes))
                        mHandler.post(() -> showFilterUpdateAlertDialog());
                    else
                        mHandler.post(() -> UpdateContent(
                                date_bytes,
                                flow_capacity,
                                accumulated_flow));

                } else {

                    for (BLEDeviceItem item : BLEDeviceItemList) {
                        if (item.getDeviceMac().equals(deviceMac)) {    // we found the same device
                            // and we update the field values

                            item.setFilter_capacity(flow_capacity);
                            item.setFilter_date(date_bytes);
                            item.setAccumulated_flow(accumulated_flow);

                            if (needToUpdateFirmware(date_bytes))
                                mHandler.post(()->showFilterUpdateAlertDialog());
                            else
                                mHandler.post(() -> UpdateContent(
                                        date_bytes,
                                        flow_capacity,
                                        accumulated_flow));
                            return;
                        }
                    }

                    // Even though we already found the BLE, we found a new one
                    // Todo : currently, we consider one BLE.
                    //  Let's think over the additional one later

                    /*
                    BLEDeviceItem newBLEDeviceItem = new BLEDeviceItem(
                            deviceName,
                            btDevice.getAddress(),
                            flow_capacity,
                            accumulated_flow,
                            date_bytes,
                            result.getRssi(),
                            false);
                    BLEDeviceItemList.add(newBLEDeviceItem);
                    */

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



}