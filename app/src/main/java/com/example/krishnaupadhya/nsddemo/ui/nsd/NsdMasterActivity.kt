package com.example.krishnaupadhya.nsddemo.ui.nsd

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import com.example.krishnaupadhya.nsddemo.R
import com.example.krishnaupadhya.nsddemo.utils.AppConstants
import com.example.krishnaupadhya.nsddemo.utils.AppUtility
import kotlinx.android.synthetic.main.activity_nsd_register.*
import org.json.JSONException
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.util.*

class NsdMasterActivity : AppCompatActivity() {

    private var mServerDeviceName = AppConstants.SERVERS_DEVICE
    private val mNsdServerType = AppConstants.SERVICE_TYPE
    private var socketServerThread: SocketServerThread? = null
    private var mNsdManager: NsdManager? = null
    internal var isRegistered = false
    private val SocketServerPort = 6000

    private var clientIPs: MutableList<String>? = null


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nsd_register)
        mServerDeviceName = AppUtility.getLocalBluetoothName()
        mNsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager
        if (!isRegistered) {
            registerService(9000)
            isRegistered = true
        }

        clientIPs = ArrayList()
        socketServerThread = SocketServerThread()
        socketServerThread!!.start()
    }

    internal var mRegistrationListener: NsdManager.RegistrationListener = object : NsdManager.RegistrationListener {

        override fun onServiceRegistered(nsdServiceInfo: NsdServiceInfo) {
            val mServiceName = nsdServiceInfo.serviceName
            mServerDeviceName = mServiceName
            Log.d(TAG, "Registered name : " + mServiceName)

            this@NsdMasterActivity.runOnUiThread { status_master_device.text = nsdServiceInfo.serviceName + " " + getString(R.string.master_connection_status) }
        }


        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo,
                                          errorCode: Int) {
            // Registration failed! Put debugging code here to determine
            // why.
        }

        override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
            // Service has been unregistered. This only happens when you
            // call
            // NsdManager.unregisterService() and pass in this listener.
            Log.d(TAG,
                    "Service Unregistered : " + serviceInfo.serviceName)
        }

        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo,
                                            errorCode: Int) {
            // Unregistration failed. Put debugging code here to determine
            // why.
        }
    }

    fun showToast(toast: String) {
        this@NsdMasterActivity.runOnUiThread { Toast.makeText(this@NsdMasterActivity, toast, Toast.LENGTH_LONG).show() }
    }


    fun registerService(port: Int) {
        val serviceInfo = NsdServiceInfo()
        serviceInfo.serviceName = mServerDeviceName
        serviceInfo.serviceType = mNsdServerType
        serviceInfo.port = port

        mNsdManager!!.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener)
    }

    private inner class SocketServerThread : Thread() {

        override fun run() {

            var socket: Socket? = null
            var serverSocket: ServerSocket? = null
            var dataInputStream: DataInputStream? = null
            var dataOutputStream: DataOutputStream? = null

            try {
                Log.i(TAG, "Creating server socket")
                serverSocket = ServerSocket(SocketServerPort)

                while (true) {
                    socket = serverSocket.accept()
                    dataInputStream = DataInputStream(
                            socket!!.getInputStream())
                    dataOutputStream = DataOutputStream(
                            socket.getOutputStream())

                    val messageFromClient: String
                    val messageToClient: String
                    val request: String

                    //If no message sent from client, this code will block the Thread
                    messageFromClient = dataInputStream.readUTF()

                    val jsondata: JSONObject

                    try {
                        jsondata = JSONObject(messageFromClient)
                        request = jsondata.getString("request")

                        if (request == REQUEST_CONNECT_CLIENT) {
                            val clientIPAddress = jsondata.getString("ipAddress")
                            if (jsondata.has("message")) {
                                val msg = jsondata.getString("message")
                                if (!TextUtils.isEmpty(msg)) {
                                    this@NsdMasterActivity.runOnUiThread { msg_text_view.text = getString(R.string.customer_mobile_number) + " " + msg }
                                }
                            }
                            // Add client IP to a list
                            clientIPs!!.add(clientIPAddress)
                            showToast("Accepted")

                            messageToClient = "Connection Accepted"


                            // Important command makes client able to send message
                            dataOutputStream.writeUTF(messageToClient)
                            // ****** Paste here Bonus 1

                            // ****** Paste here Bonus 1
                        } else {
                            // There might be other queries, but as of now nothing.
                            dataOutputStream.flush()
                        }

                    } catch (e: JSONException) {
                        e.printStackTrace()
                        Log.e(TAG, "Unable to get request")
                        dataOutputStream.flush()
                    }

                }

            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                if (socket != null) {
                    try {
                        socket.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                }

                if (dataInputStream != null) {
                    try {
                        dataInputStream.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                }

                if (dataOutputStream != null) {
                    try {
                        dataOutputStream.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                }
            }

        }

    }

    override fun onPause() {
        if (mNsdManager != null && isRegistered) {
            mNsdManager!!.unregisterService(mRegistrationListener)
            isRegistered = false
        }
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (mNsdManager != null && !isRegistered) {
            registerService(9000)
            isRegistered = true
        }

    }

    override fun onDestroy() {
        if (mNsdManager != null && isRegistered) {
            mNsdManager!!.unregisterService(mRegistrationListener)
            isRegistered = false;
        }
        super.onDestroy()
    }

    companion object {
        private val REQUEST_CONNECT_CLIENT = "request-connect-client"

        private val TAG = "NSDServer"
    }
}