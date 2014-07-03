package com.gatling.wearlifx;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by gimmiepepsi on 7/2/14.
 */
public class TestMe extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.watchface);
        Log.v("Tavonwear", "the watchface is showing");
    }
}
