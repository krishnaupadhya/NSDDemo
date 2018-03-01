package com.example.krishnaupadhya.nsddemo.ui.nsd

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.text.format.Formatter
import android.util.Log
import android.view.View
import android.widget.Toast
import com.example.krishnaupadhya.nsddemo.utils.AppConstants
import com.example.krishnaupadhya.nsddemo.R
import kotlinx.android.synthetic.main.activity_nsd_discover.*
import org.json.JSONException
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetAddress
import java.net.Socket

class NsdDiscoverActivity : AppCompatActivity(), View.OnClickListener  {

  private val SERVICE_NAME = "Client Device"

    private var hostAddress: InetAddress? = null
    private var hostPort: Int = 0
    private var mNsdManager: NsdManager? = null
    private var isServiceDiscovered = false

    private val SocketServerPort = 6000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nsd_discover)
        mNsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager
        if (!isServiceDiscovered) {
            mNsdManager!!.discoverServices(AppConstants.SERVICE_TYPE,
                    NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener)
            isServiceDiscovered = true
        }
        send_msg_btn.setOnClickListener { connectToHost() }
    }

    override fun onClick(view: View?) {
        when(view?.id){
            R.id.send_msg_btn->{
                connectToHost()
            }
        }

    }

    internal var mDiscoveryListener: NsdManager.DiscoveryListener = object : NsdManager.DiscoveryListener {

        // Called as soon as service discovery begins.
        override fun onDiscoveryStarted(regType: String) {
            Log.d(TAG, "Service discovery started")
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            // A service was found! Do something with it.
            Log.d(TAG, "Service discovery success : " + service)
            Log.d(TAG, "Host = " + service.serviceName)
            Log.d(TAG, "port = " + service.port.toString())

            if (service.serviceType != AppConstants.SERVICE_TYPE) {
                // Service type is the string containing the protocol and
                // transport layer for this service.
                Log.d(TAG, "Unknown Service Type: " + service.serviceType)
            } else if (service.serviceName == SERVICE_NAME) {
                // The name of the service tells the user what they'd be
                // connecting to. It could be "Bob's Chat App".
                Log.d(TAG, "Same machine: " + SERVICE_NAME)
                Toast.makeText(this@NsdDiscoverActivity, "Same machine: " + service.serviceName, Toast.LENGTH_LONG).show()
            } else {
                Log.d(TAG, "Diff Machine : " + service.serviceName)
                Toast.makeText(this@NsdDiscoverActivity, "Diff Machine : " + service.serviceName, Toast.LENGTH_LONG).show()
                // connect to the service and obtain serviceInfo
                mNsdManager!!.resolveService(service, mResolveListener)
            }
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            // When the network service is no longer available.
            // Internal bookkeeping code goes here.
            Log.e(TAG, "service lost" + service)
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Log.i(TAG, "Discovery stopped: " + serviceType)
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "Discovery failed: Error code:" + errorCode)
            mNsdManager!!.stopServiceDiscovery(this)
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "Discovery failed: Error code:" + errorCode)
            mNsdManager!!.stopServiceDiscovery(this)
        }
    }

    internal var mResolveListener: NsdManager.ResolveListener = object : NsdManager.ResolveListener {

        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            Log.d(TAG, "Resolve Succeeded. " + serviceInfo)

            if (serviceInfo.serviceName == SERVICE_NAME) {
                Log.d(TAG, "Same IP.")
                return
            }

            // Obtain port and IP
            hostPort = serviceInfo.port
            hostAddress = serviceInfo.host
            this@NsdDiscoverActivity.runOnUiThread { connection_status.text = "Service Connected to " + serviceInfo.serviceName }


            /* Once the client device resolves the service and obtains
 * server's ip address, connect to the server and send data
 */

            connectToHost()
        }


        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            // Called when the resolve fails. Use the error code to debug.
            Log.e(TAG, "Resolve failed " + errorCode)
            Log.e(TAG, "serivce = " + serviceInfo)
        }
    }

    private val localIpAddress: String
        get() {
            val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            return Formatter.formatIpAddress(wm.connectionInfo.ipAddress)
        }



    private fun connectToHost() {

        if (hostAddress == null) {
            Log.e(TAG, "Host Address is null")
            return
        }

        val ipAddress = localIpAddress
        val jsonData = JSONObject()

        try {
            jsonData.put("request", REQUEST_CONNECT_CLIENT)
            jsonData.put("ipAddress", ipAddress)
            if (!TextUtils.isEmpty(msg_text.text))
                jsonData.put("message", msg_text.text)
        } catch (e: JSONException) {
            e.printStackTrace()
            Log.e(TAG, "can't put request")
            return
        }

        SocketServerTask().execute(jsonData)
    }

    private inner class SocketServerTask : AsyncTask<JSONObject, Void, Void>() {
        private var jsonData: JSONObject? = null
        private var success: Boolean = false

        override fun doInBackground(vararg params: JSONObject): Void? {
            var socket: Socket? = null
            var dataInputStream: DataInputStream? = null
            var dataOutputStream: DataOutputStream? = null
            jsonData = params[0]

            try {
                // Create a new Socket instance and connect to host
                socket = Socket(hostAddress, SocketServerPort)

                dataOutputStream = DataOutputStream(
                        socket.getOutputStream())
                dataInputStream = DataInputStream(socket.getInputStream())

                // transfer JSONObject as String to the server
                dataOutputStream.writeUTF(jsonData!!.toString())
                Log.i(TAG, "waiting for response from host")

                // Thread will wait till server replies
                val response = dataInputStream.readUTF()
                if (response != null && response == "Connection Accepted") {
                    success = true
                } else {
                    success = false
                }

            } catch (e: IOException) {
                e.printStackTrace()
                success = false
            } finally {

                // close socket
                if (socket != null) {
                    try {
                        Log.i(TAG, "closing the socket")
                        socket.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                }

                // close input stream
                if (dataInputStream != null) {
                    try {
                        dataInputStream.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                }

                // close output stream
                if (dataOutputStream != null) {
                    try {
                        dataOutputStream.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                }
            }
            return null
        }

        override fun onPostExecute(result: Void) {
            if (success) {
                Toast.makeText(this@NsdDiscoverActivity, "Connection Done", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@NsdDiscoverActivity, "Unable to connect", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onPause() {
        if (mNsdManager != null) {
            mNsdManager!!.stopServiceDiscovery(mDiscoveryListener)
            isServiceDiscovered = false
        }
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (!isServiceDiscovered && mNsdManager != null) {
            mNsdManager!!.discoverServices(
                    AppConstants.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener)
        }

    }

    override fun onDestroy() {
        if (mNsdManager != null) {
            mNsdManager!!.stopServiceDiscovery(mDiscoveryListener)
        }
        super.onDestroy()
    }

    companion object {
        private val REQUEST_CONNECT_CLIENT = "request-connect-client"

        private val TAG = "NSDClient"
    }
}
