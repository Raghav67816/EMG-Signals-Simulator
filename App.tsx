import { useEffect, useState } from 'react';
import { StyleSheet, PermissionsAndroid, View, SafeAreaView, StatusBar, ToastAndroid } from 'react-native';
import { PaperProvider, Text, Button, Card, MD3DarkTheme } from 'react-native-paper';
import RNBluetoothClassic, { BluetoothDevice } from 'react-native-bluetooth-classic';
import { Dropdown } from 'react-native-paper-dropdown';


async function isBtEnabled() {
  const isEnabled = await RNBluetoothClassic.isBluetoothEnabled();
  if (isEnabled != null) {
    if (!isEnabled) {
      requestPerm();
    }
  }
}

async function checkPermission(){
  const hasPermission = await PermissionsAndroid.check('android.permission.BLUETOOTH_CONNECT');
  if(hasPermission) return;
  await requestPerm();
}

async function requestPerm() {
  const isGranted = await PermissionsAndroid.request(PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT, {
    title: "Enabled Bluetooth",
    message: "Please enable bluetooth for data transmission",
    buttonPositive: 'Allow',
    buttonNegative: 'Deny'
  })
  if (isGranted) {
    return isGranted === PermissionsAndroid.RESULTS.GRANTED;
  }
}

async function connectToDevice(device: BluetoothDevice) {
  const isConnected = await device.isConnected();
  if (!isConnected) {
    try {
      let connection = await device.connect({ connectorType: "rfcomm" });
      if (connection) { ToastAndroid.show("Connected", ToastAndroid.LONG); streamData(device); }
    } catch (e) {
      console.log(e);
      ToastAndroid.show("Failed to connect", ToastAndroid.LONG);
    }
  }
}

async function streamData(device: BluetoothDevice) {
  setInterval(() => {
    device.write(Math.random().toString());
    console.log("writing data")
  }, 5) // 1kHz
}


const theme = {
  ...MD3DarkTheme,
  colors: {
    ...MD3DarkTheme.colors,
    primary: '#BB86FC',
    background: '#121212',
    surface: '#1E1E1E',
    onSurface: '#FFFFFF',
  },
};

function App() {
  const [isConnected, setIsConnected] = useState<boolean>(false);
  const [focusedDevice, setFocusedDevice] = useState<string>();
  const [connectedDevices, setConnectedDevices] = useState<BluetoothDevice[]>([]);

  useEffect(() => {
    const getDevices = async () => {
      const devices: BluetoothDevice[] = await RNBluetoothClassic.getBondedDevices();
      setConnectedDevices(devices);
    }

    const initApp = async () => {
      checkPermission().then(
        out => {
          isBtEnabled().then(
            out2 => {
              getDevices();
            }
          )
        }
      )
    }

    const connectSub = RNBluetoothClassic.onDeviceConnected((event) => {
        setIsConnected(true);
      })

      const disconnectSub = RNBluetoothClassic.onDeviceDisconnected((event) => {
        setIsConnected(false);
      })

      initApp();
      return () => {
        if (connectSub) connectSub.remove();
        if (disconnectSub) disconnectSub.remove();
      }

  }, [])

  return (
    <PaperProvider theme={theme}>
      <StatusBar barStyle="light-content" backgroundColor="#121212" />
      <SafeAreaView style={styles.screen}>
        <View style={styles.container}>

          <View>
            <Text variant="headlineMedium" style={styles.headerText}>
              Bluetooth Control
            </Text>

            <View style={styles.section}>
              <Text variant="labelLarge" style={styles.label}>
                Select Your Device:
              </Text>
              <View style={styles.comboWrapper}>
                <Dropdown 
                  label={"Bluetooth Device"}
                  options={connectedDevices.map((device) => {
                    return { label: device.name, value: device.name }
                  })}
                  value={focusedDevice}
                  onSelect={setFocusedDevice}
                />
              </View>
            </View>

            <Card style={styles.infoCard}>
              <Card.Content>
                <View style={styles.infoRow}>
                  <Text variant="bodyLarge" style={styles.boldLabel}>Device Name:</Text>
                  <Text variant="bodyLarge" style={styles.infoText}>{focusedDevice ? focusedDevice : "None Selected"}</Text>
                </View>
                <View style={styles.infoRow}>
                  <Text variant="bodyLarge" style={styles.boldLabel}>Status:</Text>
                  <Text
                    variant="bodyLarge"
                    style={[
                      styles.infoText,
                      { color: isConnected ? '#03DAC6' : '#CF6679' }
                    ]}
                  >
                    {isConnected ? "CONNECTED" : "DISCONNECTED"}
                  </Text>
                </View>
              </Card.Content>
            </Card>
          </View>

          <Button
            mode="contained"
            style={styles.button}
            buttonColor={theme.colors.primary}
            textColor="#000"
            contentStyle={styles.buttonInner}
            onPress={async () => {
              for(const device in connectedDevices){
                if(connectedDevices[device].name == focusedDevice){
                  connectToDevice(connectedDevices[device]);
                }
              }
            }}>
            CONNECT TO DEVICE
          </Button>

        </View>
      </SafeAreaView>
    </PaperProvider>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: '#121212',
  },
  container: {
    flex: 1,
    padding: 24,
    justifyContent: 'space-between',
  },
  headerText: {
    marginBottom: 30,
    fontWeight: 'bold',
    color: '#FFFFFF', // Pure white for header
    textAlign: 'center',
  },
  section: {
    marginBottom: 30,
    zIndex: 1000,
  },
  label: {
    marginBottom: 10,
    color: '#E0E0E0', // Light gray label
    letterSpacing: 1,
  },
  comboWrapper: {
    backgroundColor: '#2C2C2C', // Dark background for combo box
    borderRadius: 8
  },
  infoCard: {
    marginTop: 20,
    backgroundColor: '#1E1E1E',
    borderWidth: 1,
    borderColor: '#333333',
  },
  infoRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginVertical: 8,
  },
  boldLabel: {
    fontWeight: 'bold',
    color: '#BDBDBD',
  },
  infoText: {
    color: '#FFFFFF',
    fontWeight: '600',
  },
  button: {
    borderRadius: 4,
    marginBottom: 10,
    elevation: 4,
  },
  buttonInner: {
    paddingVertical: 12,
  }
});

export default App;