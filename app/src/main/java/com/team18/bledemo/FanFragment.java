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
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.team18.blemodule.FanController;

import static android.content.Context.BLUETOOTH_SERVICE;

public class FanFragment extends Fragment implements FanController.FanControllerCallback {
    private static final String TAG = FanFragment.class.getSimpleName();

    private TextView mTextStatus;

    private TextView mTextSpeed;

    private SeekBar mSeekbar;

    private int value;

    private FanController mFanController;

    private Button btnConnect;

    private Button btnUpdate;

    private boolean serviceStarted = false;

    private ImageView mImgFan;

    private TextView mTextRawSpeed;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_fan, container, false);
        mSeekbar = (SeekBar) view.findViewById(R.id.seekbar);
        mTextSpeed = (TextView) view.findViewById(R.id.text_speed);
        mTextStatus = (TextView) view.findViewById(R.id.text_status);
        btnConnect = (Button) view.findViewById(R.id.button_connect);
        btnUpdate = (Button) view.findViewById(R.id.button_update);
        mImgFan = (ImageView) view.findViewById(R.id.img_fan);
        mTextRawSpeed = (TextView) view.findViewById(R.id.text_raw_speed);

        mSeekbar.setMax(65535);

        mSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                float percent = ((float) progress / seekBar.getMax()) * 100;

                mTextSpeed.setText(String.format("%.02f %%", percent));
                mTextRawSpeed.setText(Integer.toString(progress));
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
        if (adapter == null) {
            Log.e(TAG, "Bluetooth Adapter not available");
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
                if(!serviceStarted) {
                    mTextStatus.setText("Connecting to Device");
                    mFanController.startService();
                    btnConnect.setEnabled(false);
                } else {
                    serviceStarted = false;
                    mTextStatus.setText("Disconnected from Device");
                    btnConnect.setEnabled(true);
                    btnConnect.setText("Connect");
                    mFanController.stopService();
                }
            }
        });

        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int speed = (value * 100) / mSeekbar.getMax();
                Log.i(TAG, "onClick: Updating speed to: " + speed);
                Animation animation = getRotationAnimation(speed);
                if (animation != null) {
                    mImgFan.startAnimation(animation);
                } else {
                    mImgFan.setAnimation(animation);
                }
                if (serviceStarted) {
                    mTextStatus.setText("Updating value");

                    mFanController.setFanSpeed(value);
                }
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
                serviceStarted = true;
                mTextStatus.setText("Connected to Service");
                btnConnect.setText("Disconnect");
                btnConnect.setEnabled(true);
            }
        });
    }

    @Override
    public void onFanControllerServiceFailedStart(FanController.FailureReason reason) {
        Log.i(TAG, "onFanControllerServiceFailedStart: Failed");
        serviceStarted = false;
        btnConnect.setEnabled(true);
        btnConnect.setText("Connect");
        mTextStatus.setText("Failed to connect");
    }

    @Override
    public void onFanSpeedRead(int speed) {

    }

    /**
     * Get animation speed relative to the speed
     *
     * @param speed the rounded down speed percentage
     * @return the rotate animation
     */
    private RotateAnimation getRotationAnimation(int speed) {
        if (speed <= 0) {
            return null;
        } else if (speed >= 100) {
            speed = 100;
        }
        RotateAnimation anim = new RotateAnimation(0f, -360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        anim.setInterpolator(new LinearInterpolator());
        anim.setRepeatCount(Animation.INFINITE);
        anim.setFillAfter(true);
        Log.i(TAG, "getRotationAnimation: setting animation duration: " + (2000 - 10 * speed));
        anim.setDuration(1500 - 10 * speed);

        return anim;
    }
}
