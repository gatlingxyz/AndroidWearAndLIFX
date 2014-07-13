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
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

public class MainActivity extends InsetActivity implements
        View.OnClickListener,
        MessageApi.MessageListener,
        GoogleApiClient.ConnectionCallbacks{

    private final String TAG = "TavonWear";
    private GoogleApiClient mGoogleApiClient;
    private int mShortAnimationDuration;
    private View progress;
    private View onOffView;
    private Node peerNode;  //  There's normally only one.

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

        //  Just a regular activity from here. Views and such.
        progress = findViewById(R.id.progress);
        onOffView = findViewById(R.id.on_off_layout);

        findViewById(R.id.light_off).setOnClickListener(this);
        findViewById(R.id.light_on).setOnClickListener(this);

        mShortAnimationDuration = getResources().getInteger(
                android.R.integer.config_shortAnimTime);

    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.v(TAG, "connected to Google Play Services on Wear!");
        Wearable.MessageApi.addListener(mGoogleApiClient, this).setResultCallback(resultCallback);
    }

    /**
     * Not needed, but here to show capabilities. This callback occurs after the MessageApi
     * listener is added to the Google API Client.
     */
    private ResultCallback<Status> resultCallback =  new ResultCallback<Status>() {
        @Override
        public void onResult(Status status) {
            Log.v(TAG, "Status: " + status.getStatus().isSuccess());
            new AsyncTask<Void, Void, Void>(){
                @Override
                protected Void doInBackground(Void... params) {
                    sendStartMessage();
                    return null;
                }
            }.execute();
        }
    };

    /**
     * This method will generate all the nodes that are attached to a Google Api Client.
     * Now, theoretically, only one should be: the phone. However, they return us more
     * a list. In the case where the phone happens to not be the first/only, I decided to
     * make a List of all the nodes and we'll loop through them and send each of them
     * a message. After getting the list of nodes, it sends a message to each of them telling
     * it to start. One the last successful node, it saves it as our one peerNode.
     */
    private void sendStartMessage(){

        NodeApi.GetConnectedNodesResult rawNodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();

        for (final Node node : rawNodes.getNodes()) {
            Log.v(TAG, "Node: " + node.getId());
            PendingResult<MessageApi.SendMessageResult> result = Wearable.MessageApi.sendMessage(
                    mGoogleApiClient,
                    node.getId(),
                    "/start",
                    null
            );

            result.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                @Override
                public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                    //  The message is done sending.
                    //  This doesn't mean it worked, though.
                    Log.v(TAG, "Our callback is done.");
                    peerNode = node;    //  Save the node that worked so we don't have to loop again.
                }
            });
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

        PendingResult<MessageApi.SendMessageResult> result = Wearable.MessageApi.sendMessage(
                mGoogleApiClient,
                peerNode.getId(),
                togglePath,
                null
        );

        result.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
            @Override
            public void onResult(MessageApi.SendMessageResult sendMessageResult) {
            }
        });
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

    @Override
    protected void onStop() {
        super.onStop();
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
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
                break;
        }
    }

    /**
     * This is just a simple animation I got from Google documentation.
     * Just for kicks.
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

}