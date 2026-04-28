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
        device.createInsecureRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
    }

    private val startByte: Int = 0xAA;
    private val stopByte: Int = 0xBB;

//    Run transmitting data to pc
    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN])
    override fun run() {
        try {
            var index = 0
            var packetIndex = 0

            val buffer = ByteBuffer.allocate(18)
            buffer.order(ByteOrder.LITTLE_ENDIAN)

            val ch1 = _uiState.uiState.value.ch1
            val ch2 = _uiState.uiState.value.ch2
            val ch3 = _uiState.uiState.value.ch3

            _adapter.cancelDiscovery()
            bluetoothSocket?.connect()

            if (bluetoothSocket?.isConnected != true) {
                Log.e("BT", "Connection failed")
                return
            }

            _uiState.updateConnectionStatus(true)
            Log.d("BT", "Connected")

            val output = bluetoothSocket!!.outputStream

            while (_uiState.uiState.value.isRunning) {

                if (index >= ch1.size) index = 0

                buffer.clear()
                buffer.put(startByte.toByte())
                buffer.putFloat(ch1[index])
                buffer.putFloat(ch2[index])
                buffer.putFloat(ch3[index])
                buffer.putInt(packetIndex)
                buffer.put(stopByte.toByte())

                packetIndex++
                index++

                try {
                    output.write(buffer.array())
                } catch (e: IOException) {
                    Log.e("BT", "Write failed - connection lost", e)
                    break
                }

                sleep(1)
            }

        } catch (e: Exception) {
            Log.e("BT", "Error in thread", e)
        } finally {
            cancel()
        }
    }


    // Stop running
    // update state to disconnected
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