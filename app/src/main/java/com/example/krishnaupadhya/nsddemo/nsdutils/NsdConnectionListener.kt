package com.example.krishnaupadhya.nsddemo.nsdutils

import android.net.nsd.NsdServiceInfo

/**
 * Created by Krishna.Upadhya on 27-02-2018.
 */

interface NsdConnectionListener {

    fun onNsdServiceRegistered(service: NsdServiceInfo)
    fun onNsdServiceDiscovered(service: NsdServiceInfo?)
    fun onNsdServiceDiscoveryFailed(service: NsdServiceInfo?)
    fun onNsdServiceRegistrationFailed(service: NsdServiceInfo?)
}
