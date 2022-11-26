package com.waterflow.waterFlowController;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;


public class BLEDeviceAdapter extends BaseAdapter {

    private ArrayList<BLEDeviceItem> listViewItemList = new ArrayList<>();

    public BLEDeviceAdapter(){

    }

    @Override
    public int getCount() {
        return listViewItemList.size();
    }

    @Override
    public Object getItem(int position) {
        return listViewItemList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView (
                    int         position,
                    View        convertView,
                    ViewGroup   parent) {


        final Context context = parent.getContext();



        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(
                                R.layout.device_row_item,
                                parent,
                                false);
        }

        BLEDeviceItem deviceItem = listViewItemList.get(position);

        TextView    deviceName = convertView.findViewById(R.id.textViewDevice);
        TextView    deviceMAC = convertView.findViewById(R.id.textViewMac);
        ImageView   iconImage = convertView.findViewById(R.id.iconImage);
        TextView    deviceRSSI = convertView.findViewById(R.id.textViewRSSI);
        TextView    RSSILabel = convertView.findViewById(R.id.RSSI_label);

        if (deviceItem.deviceName == null || deviceItem.deviceName.trim().equals(""))
            deviceName.setText (R.string.noname);
        else
            deviceName.setText (deviceItem.deviceName);

        deviceName.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        deviceMAC.setText(deviceItem.deviceMac);
        deviceMAC.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);

        deviceRSSI.setText(String.valueOf(deviceItem.deviceRSSI));
        deviceRSSI.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        RSSILabel.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        iconImage.setImageResource(R.drawable.bluetooth);

        return convertView;
    };



    public void addItem(
                    String      deviceName,
                    String      deviceMAC,
                    byte[]      flow_capacity,
                    byte[]      accumulated_flow,
                    int         deviceRSSI,
                    byte[]      filter_date,
                    boolean     bConnected){

        BLEDeviceItem   deviceItem = new BLEDeviceItem(
                                            deviceName,
                                            deviceMAC,
                                            flow_capacity,
                accumulated_flow,
                                            filter_date,
                                            deviceRSSI, bConnected);
        listViewItemList.add(deviceItem);
    }
}
