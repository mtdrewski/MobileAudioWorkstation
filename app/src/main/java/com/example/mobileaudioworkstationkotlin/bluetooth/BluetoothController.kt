package com.example.mobileaudioworkstationkotlin.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import com.example.mobileaudioworkstationkotlin.bluetooth.domain.ConnectionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID

@SuppressLint("MissingPermission")
class BluetoothController (
    private val context: Context
){

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

    private var currentServerSocket: BluetoothServerSocket? = null
    private var currentClientSocket: BluetoothSocket? = null

    var pairedDevices = MutableStateFlow<List<LocalBluetoothDevice>>(emptyList())
    var isConnected = MutableStateFlow(false)
    var errors = MutableStateFlow("")
    private val bluetoothStateReceiver = BluetoothStateReceiver { isConnectedLocal, bluetoothDevice ->
        if(bluetoothAdapter?.bondedDevices?.contains(bluetoothDevice) == true) {
            Toast.makeText(context, "okay, okay okay", Toast.LENGTH_LONG)
            isConnected.update { isConnectedLocal }
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                errors.emit("Can't connect to a non-paired device.")
            }
        }
    }

    init {
        updatePairedDevices()
        context.registerReceiver(
            bluetoothStateReceiver,
            IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            }
        )

    }

    fun startBluetoothServer(): Flow<ConnectionResult> {
        return flow {
            if(!hasPermission(connectPermission)) {
                throw SecurityException("No BLUETOOTH_CONNECT permission")
            }
            currentServerSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord("piano_service", UUID.fromString(SERVICE_UUID))
            var shouldLoop = true
            while(shouldLoop) {
                currentClientSocket = try {
                    currentServerSocket?.accept()
                } catch(e: IOException) {
                    shouldLoop = false
                    null
                }
                emit(ConnectionResult.ConnectionEstablished)
                currentClientSocket?.let {
                    currentServerSocket?.close()
                }
            }
        }
    }

    fun connectToDevice(device: LocalBluetoothDevice): Flow<ConnectionResult>{
        return flow {
            if(!hasPermission(connectPermission)) {
                throw SecurityException("No BLUETOOTH_CONNECT permission")
            }
            currentClientSocket = bluetoothAdapter
                ?.getRemoteDevice(device.address)
                ?.createRfcommSocketToServiceRecord(
                    UUID.fromString(SERVICE_UUID)
                )

            currentClientSocket?.let { socket ->
                try {
                    socket.connect()
                    emit(ConnectionResult.ConnectionEstablished)
                } catch(e: IOException) {
                    socket.close()
                    currentClientSocket = null
                    emit(ConnectionResult.Error("Connection was interrupted"))
                }
            }
        }.onCompletion {
            closeConnection()
        }.flowOn(Dispatchers.IO)
    }

    fun release(){
        closeConnection()
        context.unregisterReceiver(bluetoothStateReceiver)
    }
    private fun closeConnection(){
        currentClientSocket?.close()
        currentServerSocket?.close()
        currentClientSocket = null
        currentServerSocket = null
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

    companion object {
        const val SERVICE_UUID = "8ed3c9f6-f97b-4d41-916d-0ee10a3554ad"
    }

}