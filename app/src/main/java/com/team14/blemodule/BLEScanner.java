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

public class BLEScanner extends ScanCallback {

    /**
     * The BLE Scanner Callback which can be used to get feedback from the scanner
     */
    interface BLEScannerCallback {
        /**
         * Called Whenever a device with the scan criteria has been found
         *
         * @param device the BluetoothDevice representation of the device found
         */
        void onDeviceFound(BluetoothDevice device);

        /**
         * Called when the scan fails
         *
         * @param reason The reason of the failure
         */
        void onScanFailed(ScannerFailureReason reason);

        /**
         * Called when the scan completes after the defined amount of time, regardless of device found or not
         */
        void onScanCompleted();
    }

    /**
     * The reasons for Scanner Failure
     */
    enum ScannerFailureReason {
        SCAN_ALREADY_STARTED,
        APPLICATION_REGISTRATION_FAILED,
        SCAN_NOT_SUPPORTED,
        INTERNAL_ERROR
    }

    private static final String TAG = BLEScanner.class.getName();
    /**
     * The BLE Scan Time in Millis
     */
    private static final long SCAN_TIME = 20000;

    /**
     * The handler used to post delayed runnable tasks, usually to stop the scanner after a certain time
     */
    private Handler mHandler;
    /**
     * defines whether the device is currently BLE Scanning or not
     */
    private boolean mScanning;
    /**
     * The bluetooth adapter, to start start the LE Scans
     */
    private BluetoothAdapter adapter;

    /**
     * Callback that is waiting for scanner results
     */
    private BLEScannerCallback mCallback;

    BLEScanner(BluetoothAdapter adapter, BLEScannerCallback callback) {
        this.adapter = adapter;
        mHandler = new Handler();
        mScanning = false;
        this.mCallback = callback;
    }

    /**
     * Starts a scan for any possible device
     */
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

    /**
     * Starts a scan for the specified device name
     *
     * @param deviceName the name of the device to filter
     */
    void startScan(String deviceName) {
        if (!mScanning) {
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

            adapter.getBluetoothLeScanner().startScan(filters, scanSettingBuilder.build(), this);
        } else {
            Log.d(TAG, "startScan: Scanner already running");
        }
    }

    /**
     * Stops the scan
     */
    void stopScan() {
        if (mScanning) {
            Log.d(TAG, "stopScan: Stopping LE Scanner");
            adapter.getBluetoothLeScanner().stopScan(this);
            mScanning = false;
            if (mCallback != null) {
                mCallback.onScanCompleted();
            }
        } else {
            Log.d(TAG, "stopScan: Scanner not running");
        }
    }

    @Override
    public void onScanResult(int callbackType, ScanResult result) {
        Log.d(TAG, "onScanResult: Device Found: " + result.toString());
        if (mCallback != null) {
            mCallback.onDeviceFound(result.getDevice());
        }
    }

    @Override
    public void onBatchScanResults(List<ScanResult> results) {
        //nothing to do here
    }

    @Override
    public void onScanFailed(int errorCode) {
        Log.e(TAG, "onScanFailed: The Scan Failed with error code: " + errorCode);
        ScannerFailureReason reason;
        switch (errorCode) {
            case SCAN_FAILED_ALREADY_STARTED:
                reason = ScannerFailureReason.SCAN_ALREADY_STARTED;
                break;
            case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                reason = ScannerFailureReason.APPLICATION_REGISTRATION_FAILED;
                break;
            case SCAN_FAILED_FEATURE_UNSUPPORTED:
                reason = ScannerFailureReason.SCAN_NOT_SUPPORTED;
                break;
            case SCAN_FAILED_INTERNAL_ERROR:
                reason = ScannerFailureReason.INTERNAL_ERROR;
                break;
            default:
                reason = ScannerFailureReason.INTERNAL_ERROR;
        }
        if (reason == ScannerFailureReason.SCAN_ALREADY_STARTED) {
            // if scan already started, it is not a failure
            mScanning = true;
        } else if (mCallback != null) {
            mCallback.onScanFailed(reason);
        }
    }
}
