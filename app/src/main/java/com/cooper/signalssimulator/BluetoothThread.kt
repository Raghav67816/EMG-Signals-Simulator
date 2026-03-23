package com.cooper.signalssimulator

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.PackageManagerCompat
import java.io.IOException
import java.util.UUID

class BluetoothThread(
    device: BluetoothDevice,
    adapter: BluetoothAdapter,
    viewModel: UiStateManager,
): Thread() {
    private val _adapter: BluetoothAdapter = adapter
    private val _uiState = viewModel
    private val bluetoothSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
        device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun run() {
        try{
            _adapter.cancelDiscovery();
            bluetoothSocket?.connect();
            _uiState.updateConnectionStatus(if(bluetoothSocket != null) bluetoothSocket!!.isConnected else false)
        }

        catch (e: SecurityException){
            Log.e("BL", "Security exception occurred")
            Log.e("BL", e.toString())
        }
    }

    fun cancel(){
        try {
            bluetoothSocket?.close()
            _uiState.updateConnectionStatus(false)
        }

        catch (e: IOException){
            Log.e("ERROR", e.toString())
        }
    }
}