import { StyleSheet } from "react-native";

export const styles = StyleSheet.create({
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