import { useEffect } from 'react';
import { StyleSheet, PermissionsAndroid, View, Text, Button } from 'react-native';
import RNBluetoothClassic, { BluetoothDevice } from 'react-native-bluetooth-classic';
import ComboBox from 'react-native-combobox';
import { useState } from 'react';

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

async function requestPerm() {
  const isGranted = await PermissionsAndroid.request(PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT, {
    title: "Enabled Bluetooth",
    message: "Please enable bluetooth for data transmission",
    buttonPositive: 'Allow',
    buttonNegative: 'Deny'
  })

  if (isGranted) {
    console.log("allowed")
  }
}

async function connectToDevice(device: BluetoothDevice){
  let connection = await  device.connect();
  if (connection){
    console.log("Connected successfully");
  }
  else{
    console.error("failed to connect");
  }
}

function App() {
  const _normalDevices: string[] = [];
  const [focusedDevice, setFocusedDevice] = useState('');
  const [bdevices, setBDevices] = useState<BluetoothDevice[]>([]);
  const [connectedDevices, setConnectedDevices] = useState<string[]>([]);

  useEffect(() => {
    const btInit = async () => {
      isBtEnabled();
    };

    // get connected devices
    const getDevices = async () => {
      const devices: BluetoothDevice[] = await RNBluetoothClassic.getBondedDevices();
      setBDevices(devices);
      for (const device in devices){
        _normalDevices.push(devices[device].name);
      }
      setConnectedDevices(_normalDevices);
    }

    btInit();
    getDevices();

  }, [])
  return (
    <View style={{ flex: 1, paddingVertical: 80, paddingHorizontal: 40, justifyContent: 'space-between' }}>
      <ComboBox values={connectedDevices} onValueSelect={(index) => {
        setFocusedDevice(connectedDevices[index]);
      }}></ComboBox>
      <Text>Selected Device: {focusedDevice}</Text>
      <Button title={'Connect'} onPress={() => {
      }}></Button>
    </View>
  );
}


const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: 'black'
  },
});

export default App;
