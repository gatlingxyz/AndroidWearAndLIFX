package com.gatling.wearlifx;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.ArrayList;
import java.util.List;

import lifx.java.android.client.LFXClient;
import lifx.java.android.entities.LFXTypes;
import lifx.java.android.light.LFXTaggedLightCollection;
import lifx.java.android.network_context.LFXNetworkContext;

/**
 * Created by gimmiepepsi on 6/30/14.
 */
public class LifxService extends WearableListenerService {

    private String TAG = "TAVONWear";
    private LFXNetworkContext networkContext;
    private WifiManager.MulticastLock ml = null;
    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate() {
        super.onCreate();

        WifiManager wifi;
        wifi = (WifiManager) getSystemService( Context.WIFI_SERVICE);
        ml = wifi.createMulticastLock("lifx_samples_tag");
        ml.acquire();

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
    public void onMessageReceived(MessageEvent messageEvent) {

        Log.v(TAG, "msg rcvd");
        Log.v(TAG, messageEvent.getPath());

        if(messageEvent.getPath().endsWith("on")){
            networkContext.getAllLightsCollection().setPowerState(LFXTypes.LFXPowerState.ON);
        }
        else if(messageEvent.getPath().endsWith("off")){
            networkContext.getAllLightsCollection().setPowerState(LFXTypes.LFXPowerState.OFF);
        }
        else{

        }

    }

    private void tellWatchConnectedState(final String state){

        new AsyncTask<Void, Void, List<Node>>(){

            @Override
            protected List<Node> doInBackground(Void... params) {
                return getNodes();
            }

            @Override
            protected void onPostExecute(List<Node> nodeList) {
                for(Node node : nodeList) {
                    Log.v(TAG, "telling " + node.getId() + " i am " + state);

                    PendingResult<MessageApi.SendMessageResult> result = Wearable.MessageApi.sendMessage(
                            mGoogleApiClient,
                            node.getId(),
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
        }.execute();

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

}
