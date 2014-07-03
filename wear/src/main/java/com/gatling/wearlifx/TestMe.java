package com.gatling.wearlifx;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

/**
 * Created by gimmiepepsi on 7/2/14.
 */
public class TestMe extends Activity {

    ImageView watchface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.watchface);
        Log.v("Tavonwear", "the watchface is showing");

        watchface = (ImageView) findViewById(R.id.watchface);

    }

    @Override
    protected void onPause() {
        super.onPause();
        watchface.setImageResource(R.drawable.ic_launcher);
    }

    @Override
    protected void onResume() {
        super.onResume();
        watchface.setImageResource(R.drawable.tensecondslater);
    }
}
