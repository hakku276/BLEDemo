package com.team14.blemodule;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import java.util.UUID;

/**
 * The weather service that scans for IPVSWeather and connects, subscribes to the device listens to changes in the values
 * Created by aanal on 5/24/17.
 */

public class WeatherService extends BluetoothGattCallback implements BLEScanner.BLEScannerCallback{

    private static final String TAG = WeatherService.class.getName();
    private static final String TARGET_DEVICE_NAME = "IPVSWeather";
    private static final String IPVS_WEATHER_UUID = "00000002-0000-0000-fdfd-fdfdfdfdfdfd";
    private static final String IPVS_WEATHER_TEMP_UUID = "00002a1c-0000-1000-8000-00805f9b34fb";

    public interface WeatherServiceCallback{
        void onTemperatureChanged(float value);
        void onHumidityChanged(int value);
        void onServiceFailedStart();
    }

    private BLEScanner scanner;
    private BluetoothAdapter adapter;
    private WeatherServiceCallback mCallback;
    private boolean mRunning;
    private Context mContext;
    private BluetoothDevice target;

    public WeatherService(Context context, BluetoothAdapter adapter, WeatherServiceCallback callback){
        this.adapter = adapter;
        this.scanner = new BLEScanner(adapter, this);
        this.mCallback = callback;
        mRunning = false;
        this.mContext = context;
    }

    public void startService(){
        if(!mRunning){
            Log.d(TAG, "startService: Booting up service");
            Log.d(TAG, "startService: Initiating Scan for target named: " + TARGET_DEVICE_NAME);
            scanner.startScan(TARGET_DEVICE_NAME);

            //TODO have a handler here to timeout
        }
    }

    @Override
    public void onDeviceFound(BluetoothDevice device) {
        Log.d(TAG, "onDeviceFound: Found Target Device");
        //connect to the target device
        if (!mRunning){
            mRunning = true;
            target = device;
            Log.d(TAG, "onDeviceFound: Connecting to target device");
            device.connectGatt(mContext, false, this);
        }
    }

    @Override
    public void onScanCompleted() {
        Log.d(TAG, "onScanCompleted: Scan Completed");
        if (!mRunning){
            Log.d(TAG, "onScanCompleted: Scan completed but the system is not running");
            if (mCallback!= null){
                mCallback.onServiceFailedStart();
            }
        }
    }

    //MARK: - Bluetooth Gatt Callback methods
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        if (status == BluetoothGatt.GATT_FAILURE){
            Log.e(TAG, "Failed GATT Connection");
            if (mCallback != null){
                mCallback.onServiceFailedStart();
            }
        }
        if (status == BluetoothGatt.GATT_SUCCESS) {
            switch (newState) {
                case BluetoothGatt.STATE_CONNECTING:
                    Log.d(TAG, "onConnectionStateChange: Connecting to device");
                    break;
                case BluetoothGatt.STATE_DISCONNECTING:
                    Log.d(TAG, "onConnectionStateChange: Disconnecting from device");
                    break;
                case BluetoothGatt.STATE_DISCONNECTED:
                    Log.d(TAG, "onConnectionStateChange: Disconnected from device");
                    break;
                case BluetoothGatt.STATE_CONNECTED:
                    Log.d(TAG, "onConnectionStateChange: Connected To Device");
                    //read the service list
                    if(!gatt.discoverServices()){
                        Log.e(TAG, "onConnectionStateChange: COuld not start discovery service");
                        if(mCallback!= null){
                            mCallback.onServiceFailedStart();
                        }
                    }
                    break;
            }
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.d(TAG, "onServicesDiscovered: Services have been discovered");
            BluetoothGattService weatherService = gatt.getService(UUID.fromString(IPVS_WEATHER_UUID));
            if (weatherService != null){
                Log.d(TAG, "onServicesDiscovered: Service found");
                BluetoothGattCharacteristic temperatureCharacteristic = weatherService.getCharacteristic(UUID.fromString(IPVS_WEATHER_TEMP_UUID));
                if(temperatureCharacteristic != null){
                    if(!gatt.setCharacteristicNotification(temperatureCharacteristic, true)){
                        Log.e(TAG, "onServicesDiscovered: Could not Register for notification");
                        if (mCallback!= null){
                            mCallback.onServiceFailedStart();
                        }
                    } else {
                        BluetoothGattDescriptor descriptor = temperatureCharacteristic.getDescriptors().get(0);
                        if(descriptor != null) {
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            gatt.writeDescriptor(descriptor);
                        }
                    }
                } else {
                    if (mCallback!= null){
                        mCallback.onServiceFailedStart();
                    }
                }
            } else {
                Log.e(TAG, "onServicesDiscovered: Service not found");
                if (mCallback!= null){
                    mCallback.onServiceFailedStart();
                }
            }
        } else {
            Log.e(TAG, "onServicesDiscovered: Failed to discover services");
            if (mCallback!= null){
                mCallback.onServiceFailedStart();
            }
        }

    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

        StringBuilder builder = new StringBuilder();
        for (byte b :
                characteristic.getValue()) {
            builder.append(String.format("%02x", b));
        }

        Log.d(TAG, "onCharacteristicChanged: " + builder.toString());

        gatt.disconnect();
    }
}
