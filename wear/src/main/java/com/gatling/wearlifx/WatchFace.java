package com.gatling.wearlifx;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

public class WatchFace extends WatchFaceActivity {

    private final static IntentFilter intentFilter;
    private boolean isDimmed = false;

    static {
        intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_TIME_TICK);
        intentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        intentFilter.addAction(Intent.ACTION_TIME_CHANGED);
    }

    TextView time, battery;
    private final String TIME_FORMAT_DISPLAYED = "KK:mm a";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.watchface);

        time = (TextView) findViewById(R.id.time);
        battery = (TextView) findViewById(R.id.battery);

        registerReceiver(mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        mTimeInfoReceiver.onReceive(this, registerReceiver(null, intentFilter));
        registerReceiver(mTimeInfoReceiver, intentFilter);

    }

    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context arg0, Intent intent) {
            battery.setText(String.valueOf(intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) + "%"));
        }
    };

    private BroadcastReceiver mTimeInfoReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context arg0, Intent intent) {
            Date date = new Date();
            time.setText(new SimpleDateFormat(TIME_FORMAT_DISPLAYED).format(date));
            setColorOfText();
        }
    };

    private void setColorOfText(){
        time.setTextColor(isDimmed ? Color.GRAY : Color.RED);
        battery.setTextColor(isDimmed ? Color.GRAY : Color.RED);
    }

    @Override
    public void onScreenDim() {
        isDimmed = true;
        ((LinearLayout)findViewById(R.id.watchface)).setGravity(Gravity.CENTER);
        setColorOfText();
    }

    @Override
    public void onScreenAwake() {
        isDimmed = false;
        ((LinearLayout)findViewById(R.id.watchface)).setGravity(Gravity.TOP);
        setColorOfText();
    }
}
