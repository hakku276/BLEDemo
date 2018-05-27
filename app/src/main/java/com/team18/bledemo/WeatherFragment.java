package com.team18.bledemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.team18.blemodule.WeatherService;

import java.util.Timer;
import java.util.TimerTask;

import static android.content.Context.BLUETOOTH_SERVICE;

public class WeatherFragment extends Fragment implements WeatherService.WeatherServiceCallback{

    private static final String TAG = WeatherFragment.class.getSimpleName();

    private TextView mTextStatus;
    private TextView mTextTemperature;
    private TextView mTextHumidity;

    private WeatherService mWeatherService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_weather, container,false);
        mTextStatus = (TextView) view.findViewById(R.id.text_state);
        mTextTemperature = (TextView) view.findViewById(R.id.text_temperature);
        mTextHumidity = (TextView) view.findViewById(R.id.text_humidity);
        BluetoothManager manager = (BluetoothManager) this.getActivity().getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager.getAdapter();
        if (adapter == null){
            Log.e(TAG, "Bluetooth Adapter not available");
            //TODO later show a notice
            mTextStatus.setText("The Bluetooth Adapter is not available");
            return view;
        }
        if (!adapter.isEnabled()) {
            Log.d(TAG, "onCreate: Bluetooth is not turned on, turning on bluetooth");
            adapter.enable();
        }

        mWeatherService = new WeatherService(getActivity(), adapter,this);

        //mWeatherService.startService();
        return view;
    }

    @Override
    public void onTemperatureChanged(final float value) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTextTemperature.setText(String.format("%.02f °C", value));
            }
        });
    }

    @Override
    public void onHumidityChanged(final int value) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTextHumidity.setText(String.format("%.02f %%", value/100.0));
            }
        });
    }

    @Override
    public void onWeatherServiceStarted() {
        Log.i(TAG, "onWeatherServiceStarted: Weather service started");
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTextStatus.setText("Weather Service Started");

                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Log.i(TAG, "startTimer: Registering for Temperature");
                        mWeatherService.registerForTemperature();
                    }
                }, 1000);

            }
        });
    }

    @Override
    public void onTemperatureServiceRegistered() {
        Log.i(TAG, "onTemperatureServiceRegistered: Registration success");

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTextStatus.setText("Registered for temperature");

                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if(!mWeatherService.registerForHumidity()){
                            Log.d(TAG, "run: Not writing Descriptor");
                        }
                    }
                },1000);
            }
        });
    }

    @Override
    public void onHumidityServiceRegistered() {
        Log.i(TAG, "onHumidityServiceRegistered: Registered for humidity");
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTextStatus.setText("Status: Ready");
            }
        });
    }

    @Override
    public void onWeatherServiceFailedStart(final WeatherService.FailureReason reason) {
        Log.i(TAG, "onWeatherServiceFailedStart: Failed to start: " + reason);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTextStatus.setText("Status: " + reason);
            }
        });
    }
}
