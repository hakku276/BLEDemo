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
    private static final String IPVS_WEATHER_HUMID_UUID = "00002a6f-0000-1000-8000-00805f9b34fb";

    public interface WeatherServiceCallback{
        void onTemperatureChanged(float value);
        void onHumidityChanged(int value);
        void onServiceStarted();
        void onServiceFailedStart(FailureReason reason);
    }

    public enum FailureReason{
        DEVICE_NOT_FOUND,
        CONNECTION_FAILED,
        UNKNOWN
    }

    private enum InternalFailureReason{
        SCANNER_FAILED,
        CHARACTERISTIC_NOT_FOUND,
        CHARACTERISTIC_NOTI_FAILED,
        DESCRIPTOR_WRITE_FAILED,
        DESCRIPTOR_NOT_FOUND,
        SERVICE_NOT_FOUND,
        DISCOVER_SERVICES_FAILED
    }

    private BLEScanner scanner;
    private WeatherServiceCallback mCallback;
    private boolean mRunning;
    private boolean mDeviceConnected;
    private Context mContext;

    public WeatherService(Context context, BluetoothAdapter adapter, WeatherServiceCallback callback){
        this.scanner = new BLEScanner(adapter, this);
        this.mCallback = callback;
        mRunning = false;
        this.mContext = context;
        this.mDeviceConnected = false;
    }

    public void startService(){
        if(!mRunning){
            Log.d(TAG, "startService: Booting up service");
            Log.d(TAG, "startService: Initiating Scan for target named: " + TARGET_DEVICE_NAME);
            scanner.startScan(TARGET_DEVICE_NAME);
        } else {
            Log.i(TAG, "startService: Service already running");
        }
    }

    public void updateValues(){
        //TODO update the values in the device

    }

    @Override
    public void onDeviceFound(BluetoothDevice device) {
        Log.d(TAG, "onDeviceFound: Found Target Device");
        //connect to the target device
        if (!mRunning){
            mRunning = true;
            mDeviceConnected = false;
            Log.d(TAG, "onDeviceFound: Connecting to target device");
            device.connectGatt(mContext, false, this);
        } else {
            Log.i(TAG, "onDeviceFound: Service Already running, cannot connect to device");
        }
    }

    @Override
    public void onScanCompleted() {
        Log.d(TAG, "onScanCompleted: Scan Completed");
        if (!mRunning){
            Log.d(TAG, "onScanCompleted: Scan completed but the system is not running");
            if (mCallback!= null){
                mCallback.onServiceFailedStart(FailureReason.DEVICE_NOT_FOUND);
            }
        }
    }

    @Override
    public void onScanFailed(BLEScanner.ScannerFailureReason reason) {
        serviceErrorHandler(null,InternalFailureReason.SCANNER_FAILED);
    }

    //MARK: - Bluetooth Gatt Callback methods
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        if (status == BluetoothGatt.GATT_FAILURE){
            Log.e(TAG, "Failed GATT Connection");
            if (mCallback != null){
                mCallback.onServiceFailedStart(FailureReason.CONNECTION_FAILED);
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
                    if(!gatt.discoverServices()){
                        Log.e(TAG, "onConnectionStateChange: Could not start discovery service");
                        if(mCallback!= null){
                            mCallback.onServiceFailedStart(FailureReason.UNKNOWN);
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
                BluetoothGattCharacteristic humidityCharacteristic = weatherService.getCharacteristic(UUID.fromString(IPVS_WEATHER_HUMID_UUID));
                if (temperatureCharacteristic == null || humidityCharacteristic == null){
                    Log.e(TAG, "onServicesDiscovered: Complete Set of Characteristics not found");
                    serviceErrorHandler(gatt, InternalFailureReason.CHARACTERISTIC_NOT_FOUND);
                } else {
                    //apply for notification request on temperature changes
                    if(!gatt.setCharacteristicNotification(temperatureCharacteristic, true) || gatt.setCharacteristicNotification(humidityCharacteristic, true)){
                        Log.e(TAG, "onServicesDiscovered: Could not Register for Temperature or Humidity notification");
                        serviceErrorHandler(gatt, InternalFailureReason.CHARACTERISTIC_NOTI_FAILED);
                    } else {
                        BluetoothGattDescriptor tempDescriptor = temperatureCharacteristic.getDescriptors().get(0);
                        BluetoothGattDescriptor humidDescriptor = humidityCharacteristic.getDescriptors().get(0);
                        if (tempDescriptor != null && humidDescriptor != null) {
                            //descriptors found
                            tempDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            humidDescriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                            if(!gatt.writeDescriptor(tempDescriptor) || !gatt.writeDescriptor(humidDescriptor)){
                                //descriptor writing okay
                                Log.d(TAG, "onServicesDiscovered: Notification Writing has been done.");
                                if (mCallback != null){
                                    mCallback.onServiceStarted();
                                }
                            } else {
                                Log.e(TAG, "onServicesDiscovered: Could not write notification request to descriptor");
                                serviceErrorHandler(gatt, InternalFailureReason.DESCRIPTOR_WRITE_FAILED);
                            }
                        } else {
                            Log.e(TAG, "onServicesDiscovered: Did not find descriptor for Temperature");
                            serviceErrorHandler(gatt, InternalFailureReason.DESCRIPTOR_NOT_FOUND);
                        }
                    }
                }
            } else {
                Log.e(TAG, "onServicesDiscovered: Service not found");
                serviceErrorHandler(gatt,InternalFailureReason.SERVICE_NOT_FOUND);
            }
        } else {
            Log.e(TAG, "onServicesDiscovered: Failed to discover services");
            serviceErrorHandler(gatt, InternalFailureReason.DISCOVER_SERVICES_FAILED);
        }

    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if(characteristic.getUuid() == UUID.fromString(IPVS_WEATHER_TEMP_UUID)){
            //temperature changed
            float temp = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_FLOAT, 0);
            Log.d(TAG, "onCharacteristicChanged: Temperature: " + temp);
            if (mCallback != null){
                mCallback.onTemperatureChanged(temp);
            }
        } else if (characteristic.getUuid() == UUID.fromString(IPVS_WEATHER_HUMID_UUID)){
            int humid = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
            Log.d(TAG, "onCharacteristicChanged: Humidity: " + humid);
            if (mCallback != null){
                mCallback.onHumidityChanged(humid);
            }
        }
    }

    /**
     * Handles error related to Weather Service
     * @param gatt the gatt connection, null if not connected
     */
    private void serviceErrorHandler(BluetoothGatt gatt, InternalFailureReason reason){
        if (mCallback!= null){
            mCallback.onServiceFailedStart(FailureReason.UNKNOWN);
        }

        if(mDeviceConnected && gatt != null){
            //disconnect from device
            gatt.disconnect();
        } else {
            mDeviceConnected = false;
        }

        //the system is not running
        mRunning = false;
    }
}
