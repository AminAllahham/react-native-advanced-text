/* eslint-disable prettier/prettier */
import type { ViewProps } from 'react-native';
import { codegenNativeComponent } from 'react-native';
// @ts-ignore
import type { DirectEventHandler, Int32 } from 'react-native/Libraries/Types/CodegenTypes';

interface HighlightedWord {
  index: Int32;
  highlightColor: string;
}

export interface NativeProps extends ViewProps {
  text: string;
  highlightedWords?: ReadonlyArray<HighlightedWord>;
  menuOptions?: ReadonlyArray<string>;
  onWordPress?: DirectEventHandler<{ word: string; index: Int32 }>;
  onSelection?: DirectEventHandler<{ selectedText: string; event: string }>;
  indicatorWordIndex?: Int32;
  fontSize?: Int32;
  fontWeight?: string;
  color?: string;
  textAlign?: string;
  fontFamily?: string;
  lineHeight?: number;
}

export default codegenNativeComponent<NativeProps>('AdvancedTextView');
