package com.team14.bledemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.team14.blemodule.BLEScanner;
import com.team14.blemodule.WeatherService;

public class MainActivity extends AppCompatActivity implements WeatherService.WeatherServiceCallback{

    private static final String TAG = MainActivity.class.getName();

    private BluetoothAdapter adapter;
    private WeatherService weatherService;

    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new Handler();

        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        adapter = manager.getAdapter();
        if (adapter == null){
            Log.e(TAG, "Bluetooth Adapter not available");
            //TODO later show a notice
            return;
        }
        if (!adapter.isEnabled()) {
            Log.d(TAG, "onCreate: Bluetooth is not turned on, turning on bluetooth");
            adapter.enable();
        }

        weatherService = new WeatherService(this, adapter, this);

        //start the weather service
        weatherService.startService();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: View Started");
    }

    //MARK: - Weather Service

    @Override
    public void onTemperatureChanged(float value) {

    }

    @Override
    public void onHumidityChanged(int value) {

    }

    @Override
    public void onServiceStarted() {
        Log.d(TAG, "onServiceStarted: Service Has Started");
        weatherService.registerForTemperature();
    }

    @Override
    public void onTemperatureServiceRegistered() {
        Log.d(TAG, "onTemperatureServiceRegistered: Registered for temperature");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(!weatherService.registerForHumidity()){
                    Log.d(TAG, "run: Not writing Descriptor");
                }
            }
        });
    }

    @Override
    public void onHumidityServiceRegistered() {
        Log.d(TAG, "onHumidityServiceRegistered: Registered for humidity");
    }

    @Override
    public void onServiceFailedStart(WeatherService.FailureReason reason) {

    }
}
