package com.team18.blemodule;

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
 * Created by aanal on 5/24/18.
 */

public class WeatherService extends BluetoothGattCallback implements BLEScanner.BLEScannerCallback {

    private static final String TAG = WeatherService.class.getName();
    private static final String TARGET_DEVICE_NAME = "IPVSWeather";
    private static final String IPVS_WEATHER_UUID = "00000002-0000-0000-fdfd-fdfdfdfdfdfd";
    private static final String IPVS_WEATHER_TEMP_UUID = "00002a1c-0000-1000-8000-00805f9b34fb";
    private static final String DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb";
    private static final String IPVS_WEATHER_HUMID_UUID = "00002a6f-0000-1000-8000-00805f9b34fb";
    private BLEScanner scanner;
    private WeatherServiceCallback mCallback;
    private boolean mRunning;
    private boolean mDeviceConnected;
    private boolean mBusy;
    private Context mContext;
    private BluetoothGatt mGatt;
    public WeatherService(Context context, BluetoothAdapter adapter, WeatherServiceCallback callback) {
        this.scanner = new BLEScanner(adapter, this);
        this.mCallback = callback;
        mRunning = false;
        this.mContext = context;
        this.mDeviceConnected = false;
        this.mRunning = false;
        this.mBusy = false;
    }

    public void startService() {
        if (!mRunning) {
            mRunning = true;
            Log.d(TAG, "startService: Booting up service");
            Log.d(TAG, "startService: Initiating Scan for target named: " + TARGET_DEVICE_NAME);
            scanner.startScan(TARGET_DEVICE_NAME);
        } else {
            Log.i(TAG, "startService: Service already running");
        }
    }

    public void stopService() {
        mRunning = false;
        if (mDeviceConnected) {
            mGatt.disconnect();
        }
    }

    public boolean registerForTemperature() {
        if (mBusy) {
            Log.d(TAG, "registerForTemperature: Gatt Busy");
            serviceErrorHandler(InternalFailureReason.GATT_BUSY);
            return false;
        }
        if (mGatt != null) {
            BluetoothGattService weatherService = mGatt.getService(UUID.fromString(IPVS_WEATHER_UUID));
            if (weatherService != null) {
                Log.d(TAG, "onServicesDiscovered: Service found");
                BluetoothGattCharacteristic temperatureCharacteristic = weatherService.getCharacteristic(UUID.fromString(IPVS_WEATHER_TEMP_UUID));
                if (temperatureCharacteristic == null) {
                    Log.e(TAG, "onServicesDiscovered: Complete Set of Characteristics not found");
                    serviceErrorHandler(InternalFailureReason.CHARACTERISTIC_NOT_FOUND);
                } else {
                    //apply for notification request on temperature changes
                    if (!mGatt.setCharacteristicNotification(temperatureCharacteristic, true)) {
                        Log.e(TAG, "onServicesDiscovered: Could not Register for Temperature or Humidity notification");
                        serviceErrorHandler(InternalFailureReason.CHARACTERISTIC_NOTI_FAILED);
                    } else {
                        BluetoothGattDescriptor tempDescriptor = temperatureCharacteristic.getDescriptor(UUID.fromString(DESCRIPTOR_UUID));
                        if (tempDescriptor != null) {
                            mBusy = true;
                            //descriptors found
                            tempDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            return mGatt.writeDescriptor(tempDescriptor);
                        } else {
                            Log.e(TAG, "onServicesDiscovered: Did not find descriptor for Temperature");
                            serviceErrorHandler(InternalFailureReason.DESCRIPTOR_NOT_FOUND);
                        }
                    }
                }
            } else {
                Log.e(TAG, "onServicesDiscovered: Service not found");
                serviceErrorHandler(InternalFailureReason.SERVICE_NOT_FOUND);
            }
        } else {
            Log.e(TAG, "registerForTemperature: Gatt Not Ready");
        }
        return false;
    }

    public boolean registerForHumidity() {
        if (mBusy) {
            Log.d(TAG, "registerForTemperature: Gatt Busy");
            serviceErrorHandler(InternalFailureReason.GATT_BUSY);
            return false;
        }
        //write the humidity descriptor
        if (mGatt != null) {
            BluetoothGattService weatherService = mGatt.getService(UUID.fromString(IPVS_WEATHER_UUID));
            if (weatherService != null) {
                BluetoothGattCharacteristic humidityCharacteristic = weatherService.getCharacteristic(UUID.fromString(IPVS_WEATHER_HUMID_UUID));
                if (humidityCharacteristic != null) {
                    mGatt.setCharacteristicNotification(humidityCharacteristic, true);
                    BluetoothGattDescriptor humidDescriptor = humidityCharacteristic.getDescriptor(UUID.fromString(DESCRIPTOR_UUID));
                    if (humidDescriptor != null) {
                        Log.d(TAG, "onDescriptorWrite: Writing Humidity Descriptor");
                        mBusy = true;
                        humidDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        return mGatt.writeDescriptor(humidDescriptor);
                    } else {
                        Log.e(TAG, "onDescriptorWrite: Could not find humidity descriptor");
                    }
                } else {
                    Log.e(TAG, "onDescriptorWrite: Could not find Humidity Characteristic");
                }
            } else {
                Log.e(TAG, "onDescriptorWrite: Could not find weather service on second run");
            }
        } else {
            Log.e(TAG, "registerForHumidity: Gatt Not Ready");
        }
        return false;
    }

    public void updateTemperatureValue() {

    }

    @Override
    public void onDeviceFound(BluetoothDevice device) {
        Log.d(TAG, "onDeviceFound: Found Target Device");
        //connect to the target device
        mDeviceConnected = false;
        Log.d(TAG, "onDeviceFound: Connecting to target device");
        //TODO have a time out while connecting to target device
        mGatt = device.connectGatt(mContext, false, this);
        scanner.stopScan();
    }

    @Override
    public void onScanCompleted() {
        Log.d(TAG, "onScanCompleted: Scan Completed");
        Log.d(TAG, "onScanCompleted: Scan completed but the system is not running");
        if (mGatt == null && mCallback != null) {
            mCallback.onWeatherServiceFailedStart(FailureReason.DEVICE_NOT_FOUND);
        }
    }

    @Override
    public void onScanFailed(BLEScanner.ScannerFailureReason reason) {
        serviceErrorHandler(InternalFailureReason.SCANNER_FAILED);
    }

    //MARK: - Bluetooth Gatt Callback methods
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        if (status == BluetoothGatt.GATT_FAILURE) {
            Log.e(TAG, "Failed GATT Connection");
            if (mCallback != null) {
                mCallback.onWeatherServiceFailedStart(FailureReason.CONNECTION_FAILED);
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
                            mCallback.onWeatherServiceFailedStart(FailureReason.UNKNOWN);
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
            if (mCallback != null) {
                mCallback.onWeatherServiceStarted();
            }
        } else {
            Log.e(TAG, "onServicesDiscovered: Failed to discover services");
            serviceErrorHandler(InternalFailureReason.DISCOVER_SERVICES_FAILED);
        }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorWrite(gatt, descriptor, status);
        mBusy = false;
        Log.d(TAG, "onDescriptorWrite: " + descriptor.getUuid().toString());
        if (status == BluetoothGatt.GATT_SUCCESS) {
            if (descriptor.getCharacteristic().getUuid().toString().equals(IPVS_WEATHER_TEMP_UUID)) {
                Log.d(TAG, "onDescriptorWrite: Temperature Descriptor successfully written");
                if (mCallback != null) {
                    mCallback.onTemperatureServiceRegistered();
                }
            } else if (descriptor.getCharacteristic().getUuid().toString().equals(IPVS_WEATHER_HUMID_UUID)) {
                Log.d(TAG, "onDescriptorWrite: Humidity Descriptor successfully written");
                if (mCallback != null) {
                    mCallback.onHumidityServiceRegistered();
                }
            } else {
                Log.e(TAG, "onDescriptorWrite: Unknown Descriptor written");
            }
        } else {
            Log.d(TAG, "onDescriptorWrite: Error Code: " + status);
            Log.e(TAG, "onDescriptorWrite: Gatt Descriptor write failed " + descriptor.getUuid().toString());
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        Log.d(TAG, "onCharacteristicChanged: Characteristic value updated");
        Log.d(TAG, "onCharacteristicChanged: " + characteristic.getUuid().toString());
        if (characteristic.getUuid().toString().equals(IPVS_WEATHER_TEMP_UUID)) {
            //temperature changed
            float temp = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_FLOAT, 1);
            Log.d(TAG, "onCharacteristicChanged: Temperature: " + temp);
            if (mCallback != null) {
                mCallback.onTemperatureChanged(temp);
            }
        } else if (characteristic.getUuid().toString().equals(IPVS_WEATHER_HUMID_UUID)) {
            int humid = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
            Log.d(TAG, "onCharacteristicChanged: Humidity: " + humid);
            if (mCallback != null) {
                mCallback.onHumidityChanged(humid);
            }
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);
        mBusy = false;
        if (status == BluetoothGatt.GATT_SUCCESS) {
            if (characteristic.getUuid().toString().equals(IPVS_WEATHER_TEMP_UUID)) {
                Log.d(TAG, "onCharacteristicRead: Temperature characteristic read");
                float temp = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_FLOAT, 1);
                Log.d(TAG, "onCharacteristicChanged: Temperature: " + temp);
                if (mCallback != null) {
                    mCallback.onTemperatureChanged(temp);
                }
            } else if (characteristic.getUuid().toString().equals(IPVS_WEATHER_HUMID_UUID)) {
                int humid = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                Log.d(TAG, "onCharacteristicChanged: Humidity: " + humid);
                if (mCallback != null) {
                    mCallback.onHumidityChanged(humid);
                }
            }
        } else {
            Log.e(TAG, "onCharacteristicRead: Gatt could not read characteristic");
            serviceErrorHandler(InternalFailureReason.CHARACTERISTIC_READ_FAILED);
        }
    }

    private void serviceErrorHandler(InternalFailureReason reason) {
        if (mCallback != null) {
            mCallback.onWeatherServiceFailedStart(FailureReason.UNKNOWN);
        }

        if (mDeviceConnected && mGatt != null) {
            //disconnect from device
            mGatt.disconnect();
        } else {
            mDeviceConnected = false;
        }

        //the system is not running
        mRunning = false;
    }

    public enum FailureReason {
        DEVICE_NOT_FOUND,
        CONNECTION_FAILED,
        UNKNOWN
    }

    private enum InternalFailureReason {
        SCANNER_FAILED,
        CHARACTERISTIC_NOT_FOUND,
        CHARACTERISTIC_NOTI_FAILED,
        CHARACTERISTIC_READ_FAILED,
        DESCRIPTOR_WRITE_FAILED,
        DESCRIPTOR_NOT_FOUND,
        SERVICE_NOT_FOUND,
        DISCOVER_SERVICES_FAILED,
        GATT_BUSY
    }


    public interface WeatherServiceCallback {
        void onTemperatureChanged(float value);

        void onHumidityChanged(int value);

        void onWeatherServiceStarted();

        void onTemperatureServiceRegistered();

        void onHumidityServiceRegistered();

        void onWeatherServiceFailedStart(FailureReason reason);
    }
}
