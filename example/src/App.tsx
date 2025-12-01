import { useCallback } from 'react';
import { StyleSheet, View, type NativeSyntheticEvent } from 'react-native';
import { AdvancedText } from 'react-native-advanced-text';

export default function App() {
  const onWordPress = useCallback((e: NativeSyntheticEvent<WordPressEvent>) => {
    console.log('Word pressed:', e.nativeEvent.word);
    console.log('Word index:', e.nativeEvent.index);
  }, []);

  const onSelection = useCallback((e: NativeSyntheticEvent<SelectionEvent>) => {
    console.log('Selected text:', e.nativeEvent.selectedText);
    console.log('Event type:', e.nativeEvent.event);
  }, []);

  const minHeight = 200;

  return (
    <View style={styles.container}>
      <AdvancedText
        text={
          'This is an example of AdvancedText component. Tap on any word to see the event in action.'
        }
        style={[styles.AdvancedText, { minHeight }]}
        indicatorWordIndex={2}
        onWordPress={onWordPress}
        menuOptions={['Highlight', 'Copy', 'Translate']}
        onSelection={onSelection}
        highlightedWords={[
          {
            index: 4,
            highlightColor: '#6baeffb5',
          },
        ]}
        fontSize={24}
        color={'#FFFFFF'}
        fontWeight="normal"
        fontFamily={'monospace'}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  AdvancedText: {
    width: '100%',
  },
});

type WordPressEvent = {
  word: string;
  index: number;
};

type SelectionEvent = {
  selectedText: string;
  event: string;
};
