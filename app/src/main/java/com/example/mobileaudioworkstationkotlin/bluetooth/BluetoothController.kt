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
import android.util.Log
import android.widget.Toast
import com.example.mobileaudioworkstationkotlin.piano.NotesPlayer
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
    var localConnectingThread = ConnectingThread()
    init {
        updatePairedDevices()
    }


    inner class StartBluetoothServerThread: Thread() {
        override fun run() {
            if(!hasPermission(connectPermission)) {
                throw SecurityException("No BLUETOOTH_CONNECT permission")
            }
            currentServerSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord("piano_service", UUID.fromString(SERVICE_UUID))
            while(true) {
                try {
                    currentClientSocket = currentServerSocket?.accept()
                    localConnectingThread.start()
                    Log.i("BLUETOOTH", "CONNECTION ESTABLISHED - SERVER")
                } catch(e: IOException) {
                    break
                }
            }
        }
    }


    inner class ConnectToDeviceThread(private val device: LocalBluetoothDevice): Thread(){
        override fun run() {
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
                    Log.i("BLUETOOTH", "CONNECTION ESTABLISHED - CLIENT")
                    localConnectingThread.start()
                } catch(e: IOException) {
                    socket.close()
                    currentClientSocket = null
                }
            }
        }
    }

    inner class ConnectingThread() : Thread() {

        val localNotesPlayer = NotesPlayer(context)
        override fun run() {
            while(true) {
                try {
                    val inputStream = currentClientSocket!!.inputStream
                    val note: Int = inputStream.read()
                    Log.i("BLUETOOTH", "connected thread received $note")
                    localNotesPlayer.playNote(note)
                } catch(e: IOException) {
                    Log.e("BLUETOOTH", "disconnected", e)
                }
            }
        }

        fun write(note: Int) {
            try {
                currentClientSocket!!.outputStream.write(note)
                Log.i("BLUETOOTH", "connected thread wrote to outputStream: $note")
            } catch(e: IOException) {
                Log.e("BLUETOOTH", "Exception during write", e)
            }
        }
    }

    fun release() {
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