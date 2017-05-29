package com.team14.bledemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.team14.blemodule.BLEScanner;
import com.team14.blemodule.FanController;
import com.team14.blemodule.WeatherService;

public class MainActivity extends AppCompatActivity implements FanController.FanControllerCallback{

    private static final String TAG = MainActivity.class.getName();

    private BluetoothAdapter adapter;
    //private WeatherService weatherService;
    private FanController fanController;

    private Handler mHandler;

    private int speed;

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

        //weatherService = new WeatherService(this, adapter, this);

        fanController = new FanController(this, adapter, this);
        fanController.startService();
        //start the weather service
        //weatherService.startService();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: View Started");
    }

    //MARK: - Weather Service

//    @Override
//    public void onTemperatureChanged(float value) {
//
//    }
//
//    @Override
//    public void onHumidityChanged(int value) {
//
//    }
//
//    @Override
//    public void onFanControllerServiceStarted() {
//        Log.d(TAG, "onWeatherServiceStarted: Service Has Started");
//        weatherService.registerForTemperature();
//    }
//
//    @Override
//    public void onTemperatureServiceRegistered() {
//        Log.d(TAG, "onTemperatureServiceRegistered: Registered for temperature");
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                if(!weatherService.registerForHumidity()){
//                    Log.d(TAG, "run: Not writing Descriptor");
//                }
//            }
//        });
//    }
//
//    @Override
//    public void onHumidityServiceRegistered() {
//        Log.d(TAG, "onHumidityServiceRegistered: Registered for humidity");
//    }
//
//    @Override
//    public void onServiceFailedStart(WeatherService.FailureReason reason) {
//
//    }


    @Override
    public void onFanControllerServiceStarted() {
        Log.d(TAG, "onFanControllerServiceStarted: Connected to FAN SERVICE");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                speed = 65000;
                fanController.setFanSpeed(36000);
            }
        });
    }

    @Override
    public void onValueWritten() {
        Log.d(TAG, "onValueWritten: Value Written");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (speed != 65000) {
                    speed = 65000;
                    fanController.setFanSpeed(36000);
                } else {
                    Log.d(TAG, "run: FINISHED");
                    fanController.readFanSpeed();
                }
            }
        });
    }

    @Override
    public void onFanControllerServiceFailedStart(FanController.FailureReason reason) {

    }

    @Override
    public void onFanSpeedRead(int speed) {
        Log.d(TAG, "onFanSpeedRead: Fan Speed: " + speed);
        fanController.stopService();
    }
}
