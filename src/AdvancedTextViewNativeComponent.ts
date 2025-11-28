import { codegenNativeComponent } from 'react-native';
import type { TextProps } from 'react-native';
// @ts-ignore
import type { DirectEventHandler, Int32 } from 'react-native/Libraries/Types/CodegenTypes';

interface HighlightedWord {
  index: Int32;
  highlightColor: string;
}

interface NativeProps extends TextProps {
  text: string;
  highlightedWords?: ReadonlyArray<HighlightedWord>;
  menuOptions?: ReadonlyArray<string>;
  onWordPress?: DirectEventHandler<{ word: string }>;
  onSelection?: DirectEventHandler<{ selectedText: string; eventType: string }>;
  indicatorWordIndex?: Int32;
}

export default codegenNativeComponent<NativeProps>('AdvancedTextView');
