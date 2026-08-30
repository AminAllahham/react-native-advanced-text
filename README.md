# react-native-advanced-text

 react-native-advanced-text is a powerful cross-platform text component for React Native that enables word-level interaction, dynamic highlighting, and custom selection actions.

## Installation


```sh
npm install react-native-advanced-text
```

> Requires the New Architecture (Fabric). React Native 0.80+ recommended.

## Usage


```js
import { AdvancedText } from "react-native-advanced-text";

<AdvancedText
  text={'This is an example of AdvancedText component. Tap on any word to see the event in action.'}
  style={styles.AdvancedText}
  onWordPress={(event) => {
    console.log({event})
  }}
  menuOptions={['Highlight', 'Copy', 'Translate']}
  onSelection={(event) => {
    console.log({event})
  }}
  highlightedWords={[
    {
      index: 2,
      highlightColor: '#FFD60A',
      borderRadius: 10,
    },
    {
      index: 4,
      highlightColor: '#6baeffb5',
      borderRadius: 6,
    },
  ]}
  fontSize={24}
  color={'#FFFFFF'}
  fontWeight="normal"
  fontFamily={'monospace'}
  letterSpacing={1}
/>
```

## Sizing

`AdvancedText` measures its own height, like React Native's built‑in `<Text>`.
Give it a width (via `style`, `flex`, or a constraining parent) and it derives
its height from the text content and that width — **no explicit `height` is
required**.

Under the hood the Fabric ShadowNode implements `measureContent()` using React
Native's own `TextLayoutManager` (the same engine `<Text>` uses), so wrapping,
font metrics, `letterSpacing` and `lineHeight` are accounted for on both iOS and
Android without any JS‑side text measurement.

```jsx
// Before: an explicit height had to be estimated and passed in.
<AdvancedText text={text} style={{ height: estimatedHeight }} />

// Now:
<AdvancedText text={text} style={{ width: '100%' }} />
```

### Props that affect measurement

| Prop            | Notes                                                                 |
| --------------- | ------------------------------------------------------------------- |
| `fontSize`      | points (defaults to 16)                                            |
| `fontFamily`    |                                                                   |
| `fontWeight`    | `"bold"` or `"italic"`                                             |
| `letterSpacing` | points; applied to both rendering and measurement                  |
| `lineHeight`    | multiple of the font size (unchanged behavior)                     |

## Contributing

- [Development workflow](CONTRIBUTING.md#development-workflow)
- [Sending a pull request](CONTRIBUTING.md#sending-a-pull-request)
- [Code of conduct](CODE_OF_CONDUCT.md)

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
