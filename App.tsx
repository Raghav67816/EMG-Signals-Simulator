import { useEffect } from 'react';
import { StyleSheet, PermissionsAndroid, View, Text, Button } from 'react-native';
import RNBluetoothClassic, { BluetoothDevice } from 'react-native-bluetooth-classic';
import ComboBox from 'react-native-combobox';
import { useState } from 'react';


// Check if bluetooth is enabled
// if not request permission
async function isBtEnabled() {
  const isEnabled = await RNBluetoothClassic.isBluetoothEnabled();
  if (isEnabled != null) {
    if (!isEnabled) {
      requestPerm();
    }
    else {
      console.info("Bluetooth is already enabled")
    }
  }
}

// request permission for bluetooth
async function requestPerm() {
  const isGranted = await PermissionsAndroid.request(PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT, {
    title: "Enabled Bluetooth",
    message: "Please enable bluetooth for data transmission",
    buttonPositive: 'Allow',
    buttonNegative: 'Deny'
  })

  if (isGranted) {
    console.log("allowed");
    return isGranted === PermissionsAndroid.RESULTS.GRANTED;
  }
}

// connect to paired device
async function connectToDevice(device: BluetoothDevice) {
  const isConnected = await device.isConnected();
  console.log(`is connected: ${isConnected}`)
  if (!isConnected) {
    try {
      let connection = await device.connect({
        connectorType: "rfcomm"
      });
      console.log(connection ? true : false);
      if (connection) {
        streamData(device);
      }
    }

    catch (e) {
      console.log(e);
    }
  }

  else {
    console.log("Already connected");
  }
}

async function streamData(device: BluetoothDevice) {
  while (true) {
    console.log("Streaming Data");
    setInterval(() => {
      device.write(Math.random().toString());
    }, 1 / 200)
  }
}

function App() {
  const [deviceIndex, setDeviceIndex] = useState(0);
  const [focusedDevice, setFocusedDevice] = useState<string>("");
  const [connectedDevices, setConnectedDevices] = useState<BluetoothDevice[]>([]);

  useEffect(() => {
    const getDevices = async () => {
      const devices: BluetoothDevice[] = await RNBluetoothClassic.getBondedDevices();
      setConnectedDevices(devices);
    }

    const btInit = async () => {
      isBtEnabled();
    }

    btInit();
    getDevices();

  }, [])

  return (
    <View style={{ flex: 1, paddingVertical: 80, paddingHorizontal: 40, justifyContent: 'space-between' }}>
      <ComboBox values={connectedDevices.map((device) => {
        return device.name;
      })} onValueSelect={(index) => {
        setDeviceIndex(index);
        setFocusedDevice(connectedDevices[index].name);
      }} />
      <Text>Selected Device: {focusedDevice}</Text>
      <Button title="Connect" onPress={async () => {
        connectToDevice(connectedDevices[deviceIndex]);
      }}></Button>
    </View>
  );
}

export default App;
