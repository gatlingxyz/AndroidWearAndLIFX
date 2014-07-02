package com.gatling.wearlifx;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.wearable.activity.InsetActivity;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends InsetActivity implements View.OnClickListener, MessageApi.MessageListener, GoogleApiClient.ConnectionCallbacks{

    private GoogleApiClient mGoogleApiClient;
    private List<Node> nodes;
    private int mShortAnimationDuration;

    private final String TAG = "TavonWear";

    private View progress;
    private View onOffView;

    @Override
    public void onReadyForContent() {
        setContentView(R.layout.activity_main);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.d(TAG, "onConnectionFailed: " + result);
                    }
                })
                .addApi(Wearable.API)
                .build();

        mGoogleApiClient.connect();

        new AsyncTask<Void, Void, List<Node>>(){

            @Override
            protected List<Node> doInBackground(Void... params) {
                return getNodes();
            }

            @Override
            protected void onPostExecute(List<Node> nodeList) {
                nodes = nodeList;
                for(Node node : nodeList) {
                    PendingResult<MessageApi.SendMessageResult> result = Wearable.MessageApi.sendMessage(
                            mGoogleApiClient,
                            node.getId(),
                            "/start",
                            null
                            );

                    result.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            Log.v(TAG, "we got something back");
                        }
                    });
                }

            }
        }.execute();

        progress = findViewById(R.id.progress);
        onOffView = findViewById(R.id.on_off_layout);

        findViewById(R.id.light_off).setOnClickListener(this);
        findViewById(R.id.light_on).setOnClickListener(this);

        mShortAnimationDuration = getResources().getInteger(
                android.R.integer.config_shortAnimTime);

    }

    private List<Node> getNodes() {
        List<Node> nodes = new ArrayList<Node>();
        NodeApi.GetConnectedNodesResult rawNodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        for (Node node : rawNodes.getNodes()) {
            nodes.add(node);
        }
        return nodes;
    }

    @Override
    public void onClick(View v) {

        switch(v.getId()){
            case R.id.light_on:
                toggleLights("on");
                break;
            case R.id.light_off:
                toggleLights("off");
                break;
            default:
                throw new IllegalArgumentException();
        }

    }

    public void toggleLights(String path){
        String togglePath = "/lights/all/" + path;

        for(Node node : nodes) {
            PendingResult<MessageApi.SendMessageResult> result = Wearable.MessageApi.sendMessage(
                    mGoogleApiClient,
                    node.getId(),
                    togglePath,
                    null
            );

            result.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                @Override
                public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                }
            });
        }
    }

    @Override
    public void onMessageReceived(final MessageEvent messageEvent) {

        /*
        This method apparently runs in a background thread.
         */

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(messageEvent.getPath().endsWith("connected")){
                    crossfade(progress, onOffView);
                }
                else{
                    crossfade(onOffView, progress);
                }
            }
        });

        Log.v(TAG, "Message received on wear: " + messageEvent.getPath());

    }

    private void crossfade(final View from, View to) {
        to.setAlpha(0f);
        to.setVisibility(View.VISIBLE);
        to.animate()
                .alpha(1f)
                .setDuration(mShortAnimationDuration)
                .setListener(null);

        from.animate()
                .alpha(0f)
                .setDuration(mShortAnimationDuration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        from.setVisibility(View.GONE);
                    }
                });
    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);

    }

}