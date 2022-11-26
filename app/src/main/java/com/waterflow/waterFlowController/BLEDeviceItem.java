package com.waterflow.waterFlowController;

import android.bluetooth.BluetoothDevice;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.RequiresApi;

import java.util.Date;

public class BLEDeviceItem implements Parcelable {
    public int              imageID;
    public String           deviceName;
    public String           deviceMac;
    public byte[]           filter_capacity;
    public byte[]           accumulated_flow;
    public byte[]           filter_date;

    public int              deviceRSSI;
    public boolean          isConnected;

    public BLEDeviceItem(
                    String              deviceName,
                    String              deviceMac,
                    byte[]              filterCapacity,
                    byte[]              waterCapacity,
                    byte[]              filter_date,
                    int     rssi,
                    boolean isConnected){
        this.imageID = imageID;
        this.deviceName = deviceName;
        this.deviceMac = deviceMac;
        this.filter_capacity = filterCapacity;
        this.accumulated_flow = waterCapacity;
        this.filter_date = filter_date;
        this.deviceRSSI = rssi;
        this.isConnected = isConnected;
    }


    protected BLEDeviceItem(Parcel in) {

        imageID = in.readInt();
        deviceName = in.readString();
        deviceMac = in.readString();
        deviceRSSI = in.readInt();
        in.readByteArray(filter_capacity);
        in.readByteArray(accumulated_flow);
        in.readByteArray(filter_date);
        isConnected = in.readByte() != 0;
    }

    public static final Creator<BLEDeviceItem> CREATOR = new Creator<BLEDeviceItem>() {
        @Override
        public BLEDeviceItem createFromParcel(Parcel in) {
            return new BLEDeviceItem(in);
        }

        @Override
        public BLEDeviceItem[] newArray(int size) {
            return new BLEDeviceItem[size];
        }
    };

    public void setConnected(boolean bConnected){
        this.isConnected = bConnected;
    }

    public void setFilter_date(byte[] new_filter_date){
        this.filter_date = new_filter_date;
    }

    public void setAccumulated_flow(byte[] new_accumulated_flow){
        this.accumulated_flow = new_accumulated_flow;
    }

    public void setFilter_capacity(byte[] new_filter_capacity){
        this.filter_capacity = new_filter_capacity;
    }

    public boolean isConnected(){
        return this.isConnected;
    }

    public String getDeviceName(){
        return this.deviceName;
    }

    public String getDeviceMac(){
        return this.deviceMac;
    }

    public byte[]  getFilterCapacity(){return this.filter_capacity;}
    public byte[]  getWaterCapacity(){return this.accumulated_flow;}

    public int getDeviceRSSI(){
        return this.deviceRSSI;
    }

    public byte[] getFilterDateBytes() {
        return this.filter_date;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(imageID);
        parcel.writeString(deviceName);
        parcel.writeString(deviceMac);
        parcel.writeInt(deviceRSSI);
        parcel.writeByteArray(filter_capacity);
        parcel.writeByteArray(accumulated_flow);
        parcel.writeByteArray(filter_date);
        parcel.writeBoolean(isConnected);
    }
}
