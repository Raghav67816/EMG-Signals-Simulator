package com.cooper.signalssimulator

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import androidx.annotation.RequiresPermission
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
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

    val interval = 1000000L
    var nextTime = System.nanoTime()

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun run() {
        try{
            var index = 0 // track of position
            val buffer: ByteBuffer = ByteBuffer.allocate(12) // 4-bytes (ch1) + 4-bytes (ch2) + 4 bytes (ch3)
            buffer.order(ByteOrder.LITTLE_ENDIAN)

            val ch1 = _uiState.uiState.value.ch1
            val ch2 = _uiState.uiState.value.ch2
            val ch3 = _uiState.uiState.value.ch3

            _adapter.cancelDiscovery()
            bluetoothSocket?.connect()
            _uiState.updateConnectionStatus(if(bluetoothSocket != null) bluetoothSocket!!.isConnected else false)

            while(_uiState.uiState.value.isRunning){
                if(index >= ch1.size){index=0}

                buffer.putFloat(ch1[index])
                buffer.putFloat(ch2[index])
                buffer.putFloat(ch3[index])
                index++

                bluetoothSocket!!.outputStream.write(buffer.array())
                buffer.clear()

                // add 1 ms to current time
                nextTime += interval
                while(System.nanoTime() < nextTime){}
            }
        }

        catch (e: SecurityException){
            Log.e("BL", "Security exception occurred")
            Log.e("BL", e.toString())
        }

        catch (e: IOException){
            Log.e("SOCK", "Server not available.")
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