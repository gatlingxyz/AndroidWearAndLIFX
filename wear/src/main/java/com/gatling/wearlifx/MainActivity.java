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

        //  Is needed for communication between the wearable and the device.
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

        //  This, or at least getNodes() has to be done in the background. Explanation there.
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

        //  Just a regular activity from here. Views and such.
        progress = findViewById(R.id.progress);
        onOffView = findViewById(R.id.on_off_layout);

        findViewById(R.id.light_off).setOnClickListener(this);
        findViewById(R.id.light_on).setOnClickListener(this);

        mShortAnimationDuration = getResources().getInteger(
                android.R.integer.config_shortAnimTime);

    }

    /**
     * This method will generate all the nodes that are attached to a Google Api Client.
     * Now, theoretically, only one should be: the phone. However, they return us more
     * than one. In the case where the phone happens to not be the first/only, I decided to
     * make a List of all the nodes and we'll loop through them and send each of them
     * a message.
     * @return  The List of Nodes
     */
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

    /**
     * This simply sends a message to the phone with the path "/lights/all/on" or "lights/all/off".
     * This is set up to be expandable, so you can target specific lights, but will probably never
     * become that right now. {@code setResultCallback} can be used in place of {@code await}; the
     * former will make the call asynchronously and provide a callback for when it's completed.
     * @param path
     */
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

    /**
     * This method receives messages from the connected device.
     * For some reason, trying to alter views in this method threw thread errors.
     * To solve this, I simple use {@code runOnUiThread}.
     * @param messageEvent
     */
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

    /**
     * This is just a simple animation I got from Google documentation.
     * @param from The starting view
     * @param to The view being transitioned to.
     */
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