package com.team14.blemodule;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * The BLE Scanner which scans for the respective BLE Devices
 * Created by aanal on 5/24/17.
 */

public class BLEScanner extends ScanCallback{

    interface BLEScannerCallback{
        void onDeviceFound(BluetoothDevice device);
        void onScanCompleted();
    }

    private static final String TAG = BLEScanner.class.getName();
    private static final long SCAN_TIME = 60000;
    private Handler mHandler;
    private boolean mScanning;
    private BluetoothAdapter adapter;
    private BLEScannerCallback mCallback;

    public BLEScanner(BluetoothAdapter adapter, BLEScannerCallback callback){
        this.adapter = adapter;
        mHandler = new Handler();
        mScanning = false;
        this.mCallback = callback;
    }

    public void startScan() {
        if (!mScanning) {
            Log.d(TAG, "startScan: Scan started for : " + SCAN_TIME);
            mScanning = true;
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "run: Stopping Scanner");
                    stopScan();
                }
            }, SCAN_TIME);
            adapter.getBluetoothLeScanner().startScan(this);
        } else {
            Log.d(TAG, "startScan: Scanner already running");
        }
    }

    public void startScan(String deviceName){
        if(!mScanning){
            mScanning = true;
            Log.d(TAG, "startScan: Searching for Device: " + deviceName);

            //ScanFilter
            List<ScanFilter> filters = new ArrayList<>(1);
            ScanFilter.Builder builder = new ScanFilter.Builder();
            builder.setDeviceName("IPVSWeather");
            filters.add(builder.build());

            //ScanSetting
            ScanSettings.Builder scanSettingBuilder = new ScanSettings.Builder();
            scanSettingBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "run: Stopping Scanner");
                    stopScan();
                }
            }, SCAN_TIME);

            adapter.getBluetoothLeScanner().startScan(filters,scanSettingBuilder.build(),this);
        } else {
            Log.d(TAG, "startScan: Scanner already running");
        }
    }

    public void stopScan(){
        if(mScanning) {
            Log.d(TAG, "stopScan: Stopping LE Scanner");
            adapter.getBluetoothLeScanner().stopScan(this);
            mScanning = false;
        } else {
            Log.d(TAG, "stopScan: Scanner not running");
        }
    }

    @Override
    public void onScanResult(int callbackType, ScanResult result) {
        Log.d(TAG, "onScanResult: Device Found: " + result.toString());
        if (mCallback != null){
            mCallback.onDeviceFound(result.getDevice());
        }
    }

    @Override
    public void onBatchScanResults(List<ScanResult> results) {

    }

    @Override
    public void onScanFailed(int errorCode) {
        Log.e(TAG, "onScanFailed: The Scan Failed with error code: " + errorCode);
    }
}
