package com.team18.bledemo;

import android.annotation.SuppressLint;
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
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.team18.blemodule.FanController;

import static android.content.Context.BLUETOOTH_SERVICE;

public class FanFragment extends Fragment implements FanController.FanControllerCallback{
    private static final String TAG = FanFragment.class.getSimpleName();

    private TextView mTextStatus;

    private TextView mTextSpeed;

    private SeekBar mSeekbar;

    private int value;

    private FanController mFanController;

    private Button btnConnect;

    private Button btnUpdate;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_fan,container,false);
        mSeekbar = (SeekBar) view.findViewById(R.id.seekbar);
        mTextSpeed = (TextView) view.findViewById(R.id.text_speed);
        mTextStatus = (TextView) view.findViewById(R.id.text_status);
        btnConnect = (Button) view.findViewById(R.id.button_connect);
        btnUpdate = (Button) view.findViewById(R.id.button_update);

        mSeekbar.setMax(65535);

        mSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mTextSpeed.setText(Integer.toString(progress));
                value = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

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

        mFanController = new FanController(getActivity(), adapter, this);

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "onClick: Starting connect");
                mTextStatus.setText("Connecting to Device");
                mFanController.startService();
                btnConnect.setEnabled(false);
            }
        });

        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTextStatus.setText("Updating value");
                mFanController.setFanSpeed(value);
            }
        });

        return view;
    }

    @Override
    public void onValueWritten() {
        Log.i(TAG, "onValueWritten: The value was written");
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTextStatus.setText("Value Updated");
            }
        });
    }

    @Override
    public void onFanControllerServiceStarted() {
        Log.i(TAG, "onFanControllerServiceStarted: Fan service started");
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTextStatus.setText("Connected to Service");
            }
        });
    }

    @Override
    public void onFanControllerServiceFailedStart(FanController.FailureReason reason) {
        Log.i(TAG, "onFanControllerServiceFailedStart: Failed");
        btnConnect.setEnabled(true);
    }

    @Override
    public void onFanSpeedRead(int speed) {

    }
}
