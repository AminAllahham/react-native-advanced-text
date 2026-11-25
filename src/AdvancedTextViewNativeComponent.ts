/* eslint-disable prettier/prettier */
import type { ViewProps } from 'react-native';
import { codegenNativeComponent } from 'react-native';
// @ts-ignore
import type { DirectEventHandler, Int32 , Float  } from 'react-native/Libraries/Types/CodegenTypes';

interface HighlightedWord {
  index: Int32;
  highlightColor: string;
  borderRadius?: Float;
  // Underlines the word in highlightColor instead of filling its background.
  // A word can have both: list it once with and once without.
  underline?: boolean;
}

export interface NativeProps extends ViewProps {
  text: string;
  highlightedWords?: ReadonlyArray<HighlightedWord>;
  menuOptions?: ReadonlyArray<string>;
  onWordPress?: DirectEventHandler<{ word: string; index: Int32 }>;
  // A double tap on a word. It never selects text.
  onWordDoublePress?: DirectEventHandler<{ word: string; index: Int32 }>;
  // A long press on a word. Text selection still starts as usual.
  onWordLongPress?: DirectEventHandler<{ word: string; index: Int32 }>;
  // startWordIndex/endWordIndex: the first and last word the selection
  // touches, or -1 when it touches none.
  onSelection?: DirectEventHandler<{
    selectedText: string;
    event: string;
    startWordIndex: Int32;
    endWordIndex: Int32;
  }>;
  // Set by AdvancedText when onWordDoublePress is given. While on, a single
  // tap waits out the double-tap delay before firing onWordPress.
  wordDoublePressEnabled?: boolean;
  fontSize?: Int32;
  fontWeight?: string;
  color?: string;
  textAlign?: string;
  fontFamily?: string;
  lineHeight?: Float;
  letterSpacing?: Float;
  // Extra space between words, in points, added to every space character on
  // top of letterSpacing. The text itself is unchanged, so word indexes and
  // selections stay the same.
  wordSpacing?: Float;
  indicatorWordIndex?: Int32;
  indicatorColor?: string;
  indicatorMode?: string;
}

export default codegenNativeComponent<NativeProps>('AdvancedTextView');
