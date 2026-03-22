import { styles } from './style'
import DataPicker from './DataPicker';
import { useEffect, useState } from 'react';
import { View, SafeAreaView, StatusBar, ToastAndroid } from 'react-native';
import { PaperProvider, Text, Button, Card, MD3DarkTheme } from 'react-native-paper';
import RNBluetoothClassic, { BluetoothDevice } from 'react-native-bluetooth-classic';
import { Dropdown } from 'react-native-paper-dropdown';
import { checkPermission, isBtEnabled, requestPerm } from './PermManager';


async function connectToDevice(device: BluetoothDevice) {
  const isConnected = await device.isConnected();
  if (!isConnected) {
    try {
      let connection = await device.connect({ connectorType: "rfcomm", delimiter: "\n" });
      if (connection) { ToastAndroid.show("Connected", ToastAndroid.LONG); streamData(device); }
    } catch (e) {
      console.log(e);
      ToastAndroid.show("Failed to connect", ToastAndroid.LONG);
    }
  }
}

async function streamData(device: BluetoothDevice) {
  setInterval(() => {
    device.write(Math.random().toFixed(2) + "\n", "utf-8")
    console.log("writing data")
  }, 5) // 200Hz
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
        out => 
          isBtEnabled().then(
            out2 => 
              getDevices()
          )
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
              ADS1293 Simulator
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
              <Card.Content>
                 <DataPicker />
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


export default App;