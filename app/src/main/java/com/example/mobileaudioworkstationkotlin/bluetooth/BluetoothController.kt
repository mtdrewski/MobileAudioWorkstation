package com.example.mobileaudioworkstationkotlin.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import com.example.mobileaudioworkstationkotlin.bluetooth.domain.ConnectionResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

@SuppressLint("MissingPermission")
class BluetoothController (
    private val context: Context
){

    private val scanPermission by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Manifest.permission.BLUETOOTH_SCAN
        } else {
            Manifest.permission.BLUETOOTH_ADMIN
        }
    }

    private val connectPermission by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Manifest.permission.BLUETOOTH_CONNECT
        } else {
            Manifest.permission.BLUETOOTH_ADMIN
        }
    }

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }

    private val bluetoothAdapter by lazy {
        bluetoothManager.adapter
    }
    init {
        updatePairedDevices()
    }

    var pairedDevices = MutableStateFlow<List<LocalBluetoothDevice>>(emptyList())

    fun startBluetoothServer(): Flow<ConnectionResult> {

    }

    fun connectToDevice(device: BluetoothDevice): Flow<ConnectionResult>{

    }

    

    private fun updatePairedDevices(){
        if (!hasPermission(connectPermission)) {
            return
        }
        bluetoothAdapter?.bondedDevices
            ?.map{ bluetoothDevice -> LocalBluetoothDevice(
                name = bluetoothDevice.name,
                address = bluetoothDevice.address)
            }?.also { devices ->
                pairedDevices.update { devices }
            }
    }

    private fun hasPermission(permission: String): Boolean{
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }



    /*
    var scannedDevices = MutableStateFlow<List<LocalBluetoothDevice>>(emptyList())

    private val foundDeviceReceiver = FoundDeviceReceiver { device ->
        scannedDevices.update { devices ->
            val newDevice = LocalBluetoothDevice(
                name = device.name,
                address = device.address)
            if(newDevice in devices) devices else devices + newDevice
        }
    }


    fun startDiscovery() {
        if(!hasPermission(scanPermission)) {
            return
        }

        context.registerReceiver(
            foundDeviceReceiver,
            IntentFilter(BluetoothDevice.ACTION_FOUND)
        )

        //updatePairedDevices()

        //bluetoothAdapter?.startDiscovery()
    }

    fun stopDiscovery() {
        if(!hasPermission(scanPermission)) {
            return
        }

        bluetoothAdapter?.cancelDiscovery()
    }

    fun release(){
        context.unregisterReceiver(foundDeviceReceiver)
    }
*/


}