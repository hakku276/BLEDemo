package com.team18.blemodule;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.UUID;

/**
 * Created by aanal on 5/24/18.
 */

public class FanController extends BluetoothGattCallback implements BLEScanner.BLEScannerCallback {

    private static final String TARGET_DEVICE = "IPVS-LIGHT";
    private static final String IPVS_FAN_SERVICE_UUID = "00000001-0000-0000-fdfd-fdfdfdfdfdfd";
    private static final String IPVS_FAN_CHARACTERISTIC_UUID = "10000001-0000-0000-fdfd-fdfdfdfdfdfd";
    private static final String TAG = FanController.class.getName();
    private Context mContext;
    private BluetoothAdapter mAdapter;
    private FanControllerCallback mCallback;
    private BLEScanner bleScanner;
    private boolean mRunning;
    private boolean mDeviceConnected;
    private boolean mBusy;
    private BluetoothGatt mGatt;
    public FanController(Context context, BluetoothAdapter adapter, FanControllerCallback callback) {
        this.mContext = context;
        this.mAdapter = adapter;
        this.mCallback = callback;
        this.bleScanner = new BLEScanner(adapter, this);
        mRunning = false;
        mDeviceConnected = false;
        mBusy = false;
    }

    public void startService() {
        if (!mRunning) {
            bleScanner.startScan(TARGET_DEVICE);
        } else {
            Log.d(TAG, "startService: System already running");
        }
    }

    public void stopService() {
        if (mRunning && mDeviceConnected) {
            mGatt.disconnect();
        }
        mRunning = false;
        mDeviceConnected = false;
    }

    public boolean setFanSpeed(int speed) {
        if (mDeviceConnected && !mBusy) {
            if (speed >= 0 && speed < 65536) {
                BluetoothGattService srvc = mGatt.getService(UUID.fromString(IPVS_FAN_SERVICE_UUID));
                if (srvc != null) {
                    BluetoothGattCharacteristic chr = srvc.getCharacteristic(UUID.fromString(IPVS_FAN_CHARACTERISTIC_UUID));
                    if (chr != null) {

                        ByteBuffer b = ByteBuffer.allocate(4);
                        b.order(ByteOrder.LITTLE_ENDIAN); // optional, the initial order of a byte buffer is always BIG_ENDIAN.
                        b.putInt(speed);
                        byte[] value = Arrays.copyOfRange(b.array(), 0, 2);
                        chr.setValue(value);

                        StringBuilder builder = new StringBuilder();

                        for (byte bt :
                                chr.getValue()) {
                            builder.append(String.format("%02x-", bt));
                        }
                        Log.d(TAG, "setFanSpeed: value: " + builder.toString());

                        mBusy = mGatt.writeCharacteristic(chr);
                        Log.d(TAG, "setFanSpeed: Busy:" + mBusy);
                        return mBusy;
                    } else {
                        //LOG
                        Log.d(TAG, "setFanSpeed: Characteristic not found");
                    }
                } else {
                    //LOG
                    Log.d(TAG, "setFanSpeed: Service not found");
                }
            } else {
                Log.e(TAG, "setFanSpeed: Invalid Fan Speed level");
            }
        }
        return false;
    }

    public void readFanSpeed() {
        BluetoothGattService srvc = mGatt.getService(UUID.fromString(IPVS_FAN_SERVICE_UUID));
        if (srvc != null) {
            BluetoothGattCharacteristic chr = srvc.getCharacteristic(UUID.fromString(IPVS_FAN_CHARACTERISTIC_UUID));
            if (chr != null) {
                mGatt.readCharacteristic(chr);
            } else {
                //LOG
                Log.d(TAG, "setFanSpeed: Characteristic not found");
            }
        } else {
            //LOG
            Log.d(TAG, "setFanSpeed: Service not found");
        }
    }

    @Override
    public void onDeviceFound(BluetoothDevice device) {
        Log.d(TAG, "onDeviceFound: Found Target Device");
        //connect to the target device
        if (!mRunning) {
            mRunning = true;
            mDeviceConnected = false;
            bleScanner.stopScan();
            Log.d(TAG, "onDeviceFound: Connecting to target device");
            //TODO have a time out while connecting to target device
            mGatt = device.connectGatt(mContext, false, this);
        } else {
            Log.i(TAG, "onDeviceFound: Service Already running, cannot connect to device");
        }
    }

    @Override
    public void onScanFailed(BLEScanner.ScannerFailureReason reason) {

    }

    @Override
    public void onScanCompleted() {
        if (mGatt == null && mCallback != null) {
            mCallback.onFanControllerServiceFailedStart(FailureReason.OTHER);
        }
    }

    //MARK: - Bluetooth Gatt Callback
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        if (status == BluetoothGatt.GATT_FAILURE) {
            Log.e(TAG, "Failed GATT Connection");
            if (mCallback != null) {
                mCallback.onFanControllerServiceFailedStart(FailureReason.OTHER);
            }
        } else if (status == BluetoothGatt.GATT_SUCCESS) {
            switch (newState) {
                case BluetoothGatt.STATE_CONNECTING:
                    Log.d(TAG, "onConnectionStateChange: Connecting to device");
                    this.mDeviceConnected = false;
                    break;
                case BluetoothGatt.STATE_DISCONNECTING:
                    Log.d(TAG, "onConnectionStateChange: Disconnecting from device");
                    this.mDeviceConnected = false;
                    break;
                case BluetoothGatt.STATE_DISCONNECTED:
                    Log.d(TAG, "onConnectionStateChange: Disconnected from device");
                    this.mDeviceConnected = false;
                    break;
                case BluetoothGatt.STATE_CONNECTED:
                    Log.d(TAG, "onConnectionStateChange: Connected To Device");
                    this.mDeviceConnected = true;
                    //read the service list
                    if (!gatt.discoverServices()) {
                        Log.e(TAG, "onConnectionStateChange: Could not start discovery service");
                        if (mCallback != null) {
                            mCallback.onFanControllerServiceFailedStart(FailureReason.OTHER);
                        }
                    }
                    break;
            }
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.d(TAG, "onCharacteristicRead: Characteristic read");
            if (mCallback != null) {
                mCallback.onFanSpeedRead(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0));
            }
        } else {
            Log.d(TAG, "onCharacteristicRead: Cannot Read Characteristic");
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.d(TAG, "onServicesDiscovered: Services have been discovered");
            if (mCallback != null) {
                mCallback.onFanControllerServiceStarted();
            }
        } else {
            Log.e(TAG, "onServicesDiscovered: Failed to discover services");
            //serviceErrorHandler(gatt, WeatherService.InternalFailureReason.DISCOVER_SERVICES_FAILED);
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
        Log.d(TAG, "onCharacteristicWrite: Characteristic written callback");
        if (status == BluetoothGatt.GATT_SUCCESS) {
            //the value was written successfully
            Log.d(TAG, "onCharacteristicWrite: Success");
            mBusy = false;
            if (mCallback != null) {
                mCallback.onValueWritten();
            }
        } else {
            Log.d(TAG, "onCharacteristicWrite: failed");
            //TODO error handling
        }
    }

    public enum FailureReason {
        OTHER;
    }

    public interface FanControllerCallback {
        void onValueWritten();

        void onFanControllerServiceStarted();

        void onFanControllerServiceFailedStart(FailureReason reason);

        void onFanSpeedRead(int speed);
    }
}
