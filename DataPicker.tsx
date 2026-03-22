import { Button, Text } from 'react-native-paper'
import { styles } from './style'
import { ToastAndroid, View } from "react-native"
import { useState } from 'react'
import { readFile } from 'react-native-fs'
import { DirectoryPickerResponse, DirectoryPickerResponseLongTerm, DocumentPickerResponse, pick, types } from '@react-native-documents/picker';

// one file is of around 30 mb each contains 10,000 samples
// we will load 1/10th of these samples at once.

export default function DataPicker() {
    const [results, _setResults] = useState<
    Array<DocumentPickerResponse[] | DirectoryPickerResponse | DirectoryPickerResponseLongTerm[]>
  >([]);

    async function selectFiles() {
        try {
            const [results] = await pick({
                type: [types.csv],
                mode: 'open',
                allowMultiSelection: true
            })

            console.log("file opened")
            addResult([results])

            console.log(results.uri);
        }

        catch (err) {
            ToastAndroid.show("Failed to open file", ToastAndroid.SHORT);
            console.error(err);
        }
    }

    const addResult = (
        newResult:
            | DocumentPickerResponse[]
            | DirectoryPickerResponse
            | DirectoryPickerResponseLongTerm[],
    ) => {
        _setResults((prevResult) => {
            if (prevResult) {
                return [newResult, ...prevResult].slice(0, 4)
            } else {
                return [newResult]
            }
        })
        return newResult
    }

    return (
        <View style={styles.infoRow}>
            <Text style={styles.boldLabel} variant='bodyLarge' >Data Files: </Text>
            <Button mode='contained-tonal' onPress={selectFiles} >Select Files</Button>
        </View>
    )
}