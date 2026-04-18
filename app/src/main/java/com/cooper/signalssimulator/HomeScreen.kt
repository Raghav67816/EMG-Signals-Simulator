package com.cooper.signalssimulator


import android.Manifest
import android.annotation.SuppressLint
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle


@SuppressLint("SupportAnnotationUsage")
@Composable
@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
fun AppScreen(viewModel: UiStateManager, hasPermission: Boolean, onSelectFileClicked: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var expanded by remember { mutableStateOf(false) }


    LaunchedEffect(hasPermission) {
        if(hasPermission){
            viewModel.setDevices()
        }
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