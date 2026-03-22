import { Alert, PermissionsAndroid } from 'react-native';
import RNBluetoothClassic from 'react-native-bluetooth-classic'; 

export async function isBtEnabled() {
  const isEnabled = await RNBluetoothClassic.isBluetoothEnabled();
  if (isEnabled != null) {
    if (!isEnabled) {
      Alert.alert(
            "Enable Bluetooth",
            "Please Enable Your Bluetooth To Use This App.",
            [{
                text: "Ok",
                style: "default",
                onPress: () => console.log("")
            }]
        );
    }
  }
}

export async function requestPerm() {
  const isGranted = await PermissionsAndroid.request(PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT, {
    title: "Allow Bluetooth",
    message: "Please enable bluetooth for data transmission",
    buttonPositive: 'Allow',
    buttonNegative: 'Deny'
  })
  if (isGranted) {
    return isGranted === PermissionsAndroid.RESULTS.GRANTED;
  }
}

// export async function requestPerm() {
//   const perms = [
//       PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT, 
//       PermissionsAndroid.PERMISSIONS.READ_EXTERNAL_STORAGE
//     ]

//   const isGranted = await PermissionsAndroid.requestMultiple(perms);
//   if (isGranted[perms[0]] == PermissionsAndroid.RESULTS.GRANTED && isGranted[perms[1]] == PermissionsAndroid.RESULTS.GRANTED) {
//     return true;
//   }
// }

export async function checkPermission(){
  const hasPermissionBle = await PermissionsAndroid.check('android.permission.BLUETOOTH_CONNECT');
  const hasStoragePerm = await PermissionsAndroid.check('android.permission.READ_EXTERNAL_STORAGE');
  if(hasPermissionBle && hasStoragePerm) return;
  await requestPerm();
}
