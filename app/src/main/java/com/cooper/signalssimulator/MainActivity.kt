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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.IOException
import java.nio.ByteBuffer

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
        if(!_uiState.value.isConnected){
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

    fun writeData(data: Pair<FloatArray, FloatArray>){
        _uiState.update {
            it.copy(
                ch1 = data.first,
                ch2 = data.second
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
            AppScreen(stateManager, { loadCsvFile() })
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

    // ✅ Permission Result
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
        deviceId: Int
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 100 && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            hasPermission = true
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
        catch (e: RuntimeException){
            Log.d("CSV", "Operation was cancelled.")
        }
    }

    // we have a csv file
    // format is as follows:
    // index
    // ignore first row
    // split (delimiter  ",")
    // write to buffer
    fun readCsvFile(fileUri: Uri): Pair<FloatArray, FloatArray>{
        val ch1 = mutableListOf<Float>()
        val ch2 = mutableListOf<Float>()

        try{
            val inputStream = contentResolver.openInputStream(fileUri)
            inputStream?.bufferedReader()?.useLines { lines ->
                lines.forEach { line ->
                    val parts = line.split(",")

                    ch1.add(parts[1].trim().toFloat())
                    ch2.add(parts[2].trim().toFloat())
                }
            }
        }

        catch(e: IOException){
            Log.e("CSV", "Error reading csv file")
        }

        return Pair(ch1.toFloatArray(), ch2.toFloatArray())
    }
}

@Composable
@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
fun AppScreen(viewModel: UiStateManager, onSelectFileClicked: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var expanded by remember { mutableStateOf(false) }


    LaunchedEffect(Unit) {
        viewModel.setDevices()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {


        Text(
            text = "ADS1293 Simulator",
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 20.dp),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "Select Your Device:",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clickable { expanded = true },
            shape = RoundedCornerShape(8.dp),
            tonalElevation = 3.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {

                Text(
                    text = "Bluetooth Device",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Text(
                    text = state.activeDevice?.name ?: "Select Device",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                state.devices?.forEach { device ->
                    DropdownMenuItem(
                        text = { Text(device.name ?: device.address) },
                        onClick = {
                            viewModel.selectDevice(device)
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(25.dp))


        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Device Name:")
                    Text(
                        text = state.activeDevice?.name ?: "None",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Status Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Status:")

                    Text(
                        text = if(state.isConnected) "CONNECTED" else "DISCONNECTED",
                        color = if(state.isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Data File: ")
                    Button(
                        onClick = {
                            onSelectFileClicked()
                        },
                        modifier = Modifier,
                        enabled = !state.isConnected,
                    ){
                        Text(if(state.dataFileName == "") "Select File" else state.dataFileName)
                    }
                }
            }
        }

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            onClick = {
                if(state.isConnected){
                    viewModel.stopBluetoothThread(state.bluetoothThread!!)
                }
                else{
                    viewModel.runBluetoothThread()
                }
            },
        ){
            Text(text=if(state.isConnected) "Disconnect" else "Connect")
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}