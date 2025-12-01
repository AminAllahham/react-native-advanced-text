# react-native-advanced-text

 react-native-advanced-text is a powerful cross-platform text component for React Native that enables word-level interaction, dynamic highlighting, and custom selection actions.

## Installation


```sh
npm install react-native-advanced-text
```


## Usage


```js
import { AdvancedTextView } from "react-native-advanced-text";

<AdvancedText
  text={'This is an example of AdvancedText component. Tap on any word to see the event in action.'}
  style={[styles.AdvancedText, { minHeight }]}
  indicatorWordIndex={2}
  onWordPress={(event) => {
    console.log({event})
  }}
  menuOptions={['Highlight', 'Copy', 'Translate']}
  onSelection={(event) => {
    console.log({event})
  }}
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
```


## Contributing

- [Development workflow](CONTRIBUTING.md#development-workflow)
- [Sending a pull request](CONTRIBUTING.md#sending-a-pull-request)
- [Code of conduct](CODE_OF_CONDUCT.md)

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
