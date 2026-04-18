package com.cooper.signalssimulator

import android.Manifest
import android.app.ComponentCaller
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.IOException

// UI DOES NOT HANDLE LOGIC IT HANDLES STATE - KEEP IN MIND (NOTE FOR ME)

data class UiState(
    val devices: Set<BluetoothDevice>? = emptySet(),
    var activeDevice: BluetoothDevice? = null,
    val isConnected: Boolean = false,
    val bluetoothThread: BluetoothThread? = null,
    var dataFileName: String = "",
    var dataFileUri: Uri? = null,
    var ch1: FloatArray = floatArrayOf(),
    var ch2: FloatArray = floatArrayOf(),
    var ch3: FloatArray = floatArrayOf(),
    var isRunning: Boolean = true
)

class UiStateManager(private val adapter: BluetoothAdapter): ViewModel(){
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun setDevices(){
        _uiState.update { devices ->
            devices.copy(
                devices = adapter.bondedDevices?.toSet()
            )
        }
    }

    fun selectDevice(device: BluetoothDevice){
        _uiState.update { activeDevice ->
            activeDevice.copy(
                activeDevice = device
            )
        }
    }

    fun updateConnectionStatus(isConnected_: Boolean){
        _uiState.update { isConnected ->
            isConnected.copy(
                isConnected = isConnected_
            )
        }
    }

    fun runBluetoothThread(){
        Log.d("CONN", _uiState.value.isConnected.toString())
        Log.d("AD", _uiState.value.activeDevice.toString())
        Log.d("URI", _uiState.value.dataFileUri.toString())
        if(!_uiState.value.isConnected && _uiState.value.activeDevice != null && _uiState.value.dataFileName != ""){
            val bThread = BluetoothThread(
                _uiState.value.activeDevice!!,
                adapter,
                this
            )
            _uiState.update { bluetoothThread ->
                bluetoothThread.copy(
                    bluetoothThread = bThread
                )
            }
            bThread.start()
            Log.d("BL", "Starting thread")
        }
    }

    fun stopBluetoothThread(bluetoothThread: BluetoothThread){
        bluetoothThread.cancel()
    }

    fun setDataFileName(name: String){
        _uiState.update { dataFileName ->
            dataFileName.copy(
                dataFileName = name
            )
        }
    }

    fun writeData(data: List<MutableList<Float>>){
        _uiState.update {
            it.copy(
                ch1 = data[0].toFloatArray(),
                ch2 = data[1].toFloatArray(),
                ch3 = data[2].toFloatArray()
            )
        }

        Log.d("DATA", "Channel data written to arrays.")
    }
}

class MainActivity : ComponentActivity() {

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val manager = getSystemService(BluetoothManager::class.java)
        manager.adapter
    }

    private var hasPermission by mutableStateOf(false)
    val stateManager: UiStateManager by lazy {
        UiStateManager(bluetoothAdapter)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        checkPermission()

        setContent {
            AppScreen(stateManager, hasPermission, { loadCsvFile() })
        }


    }

    // ✅ Permission Handling
    @RequiresApi(Build.VERSION_CODES.S)
    private fun checkPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            hasPermission = true
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN),
                100
            )
        }
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)} passing\n      in a {@link RequestMultiplePermissions} object for the {@link ActivityResultContract} and\n      handling the result in the {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 100 && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            hasPermission = true
            stateManager.setDevices()
        } else {
            Log.e("BL", "Permission denied")
        }
    }

    fun loadCsvFile(){
        Log.i("CSV", "Opening Dialog")
        val reqCode = 2
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        startActivityForResult(Intent.createChooser(intent, "Select Data File"), reqCode)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        caller: ComponentCaller
    ) {
        super.onActivityResult(requestCode, resultCode, data, caller)
        try {
            if(requestCode == 2 && resultCode == RESULT_OK){
                val fileUri = data!!.data
                if(fileUri != null){
                    val docFile: DocumentFile? = DocumentFile.fromSingleUri(applicationContext, fileUri)
                    if(docFile != null){
                        stateManager.setDataFileName(docFile.name.toString())
                        Log.i("CSV", docFile.name.toString())
                        val data = readCsvFile(fileUri)
                        stateManager.writeData(data)
                    }
                }
            }
        }

        // if operation is cancelled
        catch (_: RuntimeException){
            Log.d("CSV", "Operation was cancelled.")
        }
    }

    // we have a csv file
    // format is as follows:
    // index
    // ignore first row
    // split (delimiter  ",")
    // write to buffer
    fun readCsvFile(fileUri: Uri): List<MutableList<Float>>{
        val ch1 = mutableListOf<Float>()
        val ch2 = mutableListOf<Float>()
        val ch3 = mutableListOf<Float>()

        try{
            val inputStream = contentResolver.openInputStream(fileUri)
            inputStream?.bufferedReader()?.useLines { lines ->
                lines.forEach { line ->
                    val parts = line.split(",")

                    ch1.add(parts[1].trim().toFloat())
                    ch2.add(parts[2].trim().toFloat())
                    ch3.add(parts[3].trim().toFloat())
                }
            }
        }

        catch(_: IOException){
            Log.e("CSV", "Error reading csv file")
        }

        val arr = listOf(ch1, ch2, ch3)
        return arr
    }
}
