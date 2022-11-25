package com.waterflow.waterFlowController;

import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.RequiresApi;

public class BLEDeviceItem implements Parcelable {
    public int          imageID;
    public String       deviceName;
    public String       deviceMac;
    public int          deviceRSSI;
    public boolean      isConnected;

    public BLEDeviceItem(
                    String  deviceName,
                    String  deviceMac,
                    int     rssi,
                    boolean isConnected){
        this.imageID = imageID;
        this.deviceName = deviceName;
        this.deviceMac = deviceMac;
        this.deviceRSSI = rssi;
        this.isConnected = isConnected;
    }


    protected BLEDeviceItem(Parcel in) {
        imageID = in.readInt();
        deviceName = in.readString();
        deviceMac = in.readString();
        deviceRSSI = in.readInt();
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

    public boolean isConnected(){
        return this.isConnected;
    }

    public String getDeviceName(){
        return this.deviceName;
    }

    public String getDeviceMac(){
        return this.deviceMac;
    }


    public int getDeviceRSSI(){
        return this.deviceRSSI;
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
        parcel.writeBoolean(isConnected);
    }
}
