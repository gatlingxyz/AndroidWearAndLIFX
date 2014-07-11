package com.gatling.wearlifx;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import lifx.java.android.client.LFXClient;
import lifx.java.android.entities.LFXTypes;
import lifx.java.android.light.LFXTaggedLightCollection;
import lifx.java.android.network_context.LFXNetworkContext;

/**
 * Most of this sample code is for communicating with LIFX and the watch.
 */
public class LifxService extends WearableListenerService {

    private String TAG = "TAVONWear";
    private LFXNetworkContext networkContext;
    private WifiManager.MulticastLock ml = null;
    private GoogleApiClient mGoogleApiClient;
    private Node peerNode;

    @Override
    public void onCreate() {
        super.onCreate();

        //  As noted in the LIFX samples, this may be needed.
        WifiManager wifi;
        wifi = (WifiManager) getSystemService( Context.WIFI_SERVICE);
        ml = wifi.createMulticastLock("lifx_samples_tag");
        ml.acquire();

        //  Needed for communication between watch and device.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d(TAG, "onConnected: " + connectionHint);
                    }
                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.d(TAG, "onConnectionSuspended: " + cause);
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.d(TAG, "onConnectionFailed: " + result);
                    }
                })
                .addApi(Wearable.API)
                .build();

        mGoogleApiClient.connect();
    }

    @Override
    public void onPeerConnected(Node peer) {
        peerNode = peer;
        connectToLifx();
    }

    private void connectToLifx(){
        /**
         * The following is very important for LIFX.
         * After calling {@code connect()} it is not instantly connected.
         * While testing this, I called things immediately after connect and
         * it threw errors. Only when my phone accidentally rotated did I realize
         * what was going on. Wait for a connection with LIFX before proceeding.
         */
        LFXClient client = LFXClient.getSharedInstance( getApplicationContext());
        networkContext = client.getLocalNetworkContext();
        networkContext.addNetworkContextListener(new LFXNetworkContext.LFXNetworkContextListener() {
            @Override
            public void networkContextDidConnect(LFXNetworkContext networkContext) {
                tellWatchConnectedState("connected");
            }

            @Override
            public void networkContextDidDisconnect(LFXNetworkContext networkContext) {
                tellWatchConnectedState("disconnected");
            }

            @Override
            public void networkContextDidAddTaggedLightCollection(LFXNetworkContext networkContext, LFXTaggedLightCollection collection) {

            }

            @Override
            public void networkContextDidRemoveTaggedLightCollection(LFXNetworkContext networkContext, LFXTaggedLightCollection collection) {

            }
        });
        networkContext.connect();
    }

    /**
     * Here, the device actually receives the message that the phone sent, as a path.
     * We simply check that path's last segment and act accordingly.
     * @param messageEvent
     */
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        Log.v(TAG, "msg rcvd");
        Log.v(TAG, messageEvent.getPath());

        if(networkContext != null) {
            if (messageEvent.getPath().endsWith("on")) {
                networkContext.getAllLightsCollection().setPowerState(LFXTypes.LFXPowerState.ON);
            } else if (messageEvent.getPath().endsWith("off")) {
                networkContext.getAllLightsCollection().setPowerState(LFXTypes.LFXPowerState.OFF);
            }
        }
    }

    private void tellWatchConnectedState(final String state){

        Log.v(TAG, "telling " + peerNode.getId() + " i am " + state);

        PendingResult<MessageApi.SendMessageResult> result = Wearable.MessageApi.sendMessage(
                mGoogleApiClient,
                peerNode.getId(),
                "/listener/lights/" + state,
                null
        );

        result.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
            @Override
            public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                Log.v(TAG, "Phone: " + sendMessageResult.getStatus().getStatusMessage());
            }
        });

    }


}
