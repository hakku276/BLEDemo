package com.team14.bledemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.team14.blemodule.BLEScanner;
import com.team14.blemodule.WeatherService;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();
    private static final String IPVS_WEATHER = "IPVSWeather";


    private BluetoothAdapter adapter;
    private WeatherService weatherService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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

        weatherService = new WeatherService(this, adapter, null);

        //start the weather service
        weatherService.startService();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: View Started");
    }
}
