import { useEffect } from 'react';
import { StyleSheet, PermissionsAndroid, View, Text } from 'react-native';
import RNBluetoothClassic, {
  BluetoothDevice
} from 'react-native-bluetooth-classic';

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

function App() {

  useEffect(() => {
    const btInit = async () => {
      isBtEnabled();
    };

    btInit();
  }, [])
  return (
    <View style={styles.container}>

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
