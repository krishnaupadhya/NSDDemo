package com.example.krishnaupadhya.nsddemo.ui.nsd;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.example.krishnaupadhya.nsddemo.R;
import com.example.krishnaupadhya.nsddemo.utils.AppConstants;
import com.example.krishnaupadhya.nsddemo.utils.AppUtility;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class NsdSlaveActivity extends AppCompatActivity {

    private String mClientDeviceName = AppConstants.CUSTOMERS_DEVICE;
    private String mNsdServerType = AppConstants.SERVICE_TYPE;

    private InetAddress hostAddress;
    private int hostPort;
    private NsdManager mNsdManager;
    boolean isDiscovered = false;
    private int SocketServerPort = 6000;
    private static final String REQUEST_CONNECT_CLIENT = "request-connect-client";

    private static final String TAG = "NSDClient";
    EditText msgEditText;
    TextView slaveStatusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nsd_slave);
        mClientDeviceName = AppUtility.getLocalBluetoothName();
        mNsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
        if (!isDiscovered) {
            mNsdManager.discoverServices(mNsdServerType,
                    NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
            isDiscovered = true;
        }

        Button sendBtn = findViewById(R.id.send_msg_btn);
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectToHost();
            }
        });
        msgEditText = findViewById(R.id.msg_text_slave);
        slaveStatusText = findViewById(R.id.connection_status);
    }

    NsdManager.DiscoveryListener mDiscoveryListener = new NsdManager.DiscoveryListener() {

        // Called as soon as service discovery begins.
        @Override
        public void onDiscoveryStarted(String regType) {
            Log.d(TAG, "Service discovery started");
        }

        @Override
        public void onServiceFound(NsdServiceInfo service) {
            // A service was found! Do something with it.
            Log.d(TAG, "Service discovery success : " + service);
            Log.d(TAG, "Host = " + service.getServiceName());
            Log.d(TAG, "port = " + String.valueOf(service.getPort()));

            if (!service.getServiceType().equals(mNsdServerType)) {
                // Service type is the string containing the protocol and
                // transport layer for this service.
                Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
            } else if (service.getServiceName().equals(mClientDeviceName)) {
                // The name of the service tells the user what they'd be
                // connecting to. It could be "Bob's Chat App".
                Log.d(TAG, "Same machine: " + mClientDeviceName);
            } else {
                Log.d(TAG, "Diff Machine : " + service.getServiceName());
                // connect to the service and obtain serviceInfo
                mNsdManager.resolveService(service, createResolveListener());
            }
        }

        @Override
        public void onServiceLost(NsdServiceInfo service) {
            // When the network service is no longer available.
            // Internal bookkeeping code goes here.
            Log.e(TAG, "service lost" + service);
        }

        @Override
        public void onDiscoveryStopped(String serviceType) {
            Log.i(TAG, "Discovery stopped: " + serviceType);
        }

        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, "Discovery failed: Error code:" + errorCode);
            mNsdManager.stopServiceDiscovery(this);
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, "Discovery failed: Error code:" + errorCode);
            mNsdManager.stopServiceDiscovery(this);
        }
    };

    private NsdManager.ResolveListener createResolveListener() {
        return new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Called when the resolve fails. Use the error code to debug.
                Log.e(TAG, "Resolve failed " + errorCode);
                Log.e(TAG, "serivce = " + serviceInfo);
            }

            @Override
            public void onServiceResolved(final NsdServiceInfo serviceInfo) {
                Log.d(TAG, "Resolve Succeeded. " + serviceInfo);

                if (serviceInfo.getServiceName().equals(mClientDeviceName)) {
                    Log.d(TAG, "Same IP.");
                    return;
                }

                // Obtain port and IP
                hostPort = serviceInfo.getPort();
                hostAddress = serviceInfo.getHost();
                NsdSlaveActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        slaveStatusText.setText(serviceInfo.getServiceName());
                    }
                });

                 /* Once the client device resolves the service and obtains
                 * server's ip address, connect to the server and send data
                 */

                connectToHost();
            }
            //Rest of the code here
        };
    }

//    NsdManager.ResolveListener mResolveListener = new NsdManager.ResolveListener() {
//
//        @Override
//        public void onServiceResolved(final NsdServiceInfo serviceInfo) {
//            Log.d(TAG, "Resolve Succeeded. " + serviceInfo);
//
//            if (serviceInfo.getServiceName().equals(mClientDeviceName)) {
//                Log.d(TAG, "Same IP.");
//                return;
//            }
//
//            // Obtain port and IP
//            hostPort = serviceInfo.getPort();
//            hostAddress = serviceInfo.getHost();
//            NsdSlaveActivity.this.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    slaveStatusText.setText(serviceInfo.getServiceName());
//                }
//            });
//
// /* Once the client device resolves the service and obtains
// * server's ip address, connect to the server and send data
// */
//
//            connectToHost();
//        }
//
//
//        @Override
//        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
//            // Called when the resolve fails. Use the error code to debug.
//            Log.e(TAG, "Resolve failed " + errorCode);
//            Log.e(TAG, "serivce = " + serviceInfo);
//        }
//    };

    private void connectToHost() {

        if (hostAddress == null) {
            Log.e(TAG, "Host Address is null");
            return;
        }

        String ipAddress = getLocalIpAddress();
        JSONObject jsonData = new JSONObject();

        try {
            jsonData.put("request", REQUEST_CONNECT_CLIENT);
            jsonData.put("ipAddress", ipAddress);
            if (!TextUtils.isEmpty(msgEditText.getText())) {
                jsonData.put("message", msgEditText.getText());
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "can't put request");
            return;
        }

        new SocketServerTask().execute(jsonData);
    }

    private String getLocalIpAddress() {
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        return ip;
    }

    private class SocketServerTask extends AsyncTask<JSONObject, Void, Void> {
        private JSONObject jsonData;
        private boolean success;

        @Override
        protected Void doInBackground(JSONObject... params) {
            Socket socket = null;
            DataInputStream dataInputStream = null;
            DataOutputStream dataOutputStream = null;
            jsonData = params[0];

            try {
                // Create a new Socket instance and connect to host
                socket = new Socket(hostAddress, SocketServerPort);

                dataOutputStream = new DataOutputStream(
                        socket.getOutputStream());
                dataInputStream = new DataInputStream(socket.getInputStream());

                // transfer JSONObject as String to the server
                dataOutputStream.writeUTF(jsonData.toString());
                Log.i(TAG, "waiting for response from host");

                // Thread will wait till server replies
                String response = dataInputStream.readUTF();
                if (response != null && response.equals("Connection Accepted")) {
                    success = true;
                } else {
                    success = false;
                }

            } catch (IOException e) {
                e.printStackTrace();
                success = false;
            } finally {

                // close socket
                if (socket != null) {
                    try {
                        Log.i(TAG, "closing the socket");
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                // close input stream
                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                // close output stream
                if (dataOutputStream != null) {
                    try {
                        dataOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (success) {
                Toast.makeText(NsdSlaveActivity.this, "Connection Done", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(NsdSlaveActivity.this, "Unable to connect", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onPause() {
        if (mNsdManager != null && isDiscovered) {
            mNsdManager.stopServiceDiscovery(mDiscoveryListener);
            isDiscovered = false;
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mNsdManager != null
                && !isDiscovered) {
            mNsdManager.discoverServices(
                    mNsdServerType, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
            isDiscovered = true;
        }

    }

    @Override
    protected void onDestroy() {
        if (mNsdManager != null && isDiscovered) {
            mNsdManager.stopServiceDiscovery(mDiscoveryListener);
        }
        super.onDestroy();
    }

}