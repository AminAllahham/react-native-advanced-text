import React from 'react';
import type {
  NativeSyntheticEvent,
  StyleProp,
  ViewProps,
  ViewStyle,
} from 'react-native';
import AdvancedTextViewNativeComponent from './AdvancedTextViewNativeComponent';

interface HighlightedWord {
  index: number;
  highlightColor: string;
}

interface WordPressEvent {
  word: string;
  index: number;
}

interface SelectionEvent {
  selectedText: string;
  event: string;
}

interface NativeProps extends ViewProps {
  text: string;
  style?: StyleProp<ViewStyle>;
  highlightedWords?: ReadonlyArray<HighlightedWord>;
  menuOptions?: ReadonlyArray<string>;
  onWordPress?: (event: NativeSyntheticEvent<WordPressEvent>) => void;
  onSelection?: (event: NativeSyntheticEvent<SelectionEvent>) => void;
  indicatorWordIndex?: number;
}

export const AdvancedText: React.FC<NativeProps> = ({
  text,
  style,
  highlightedWords,
  menuOptions,
  onWordPress,
  onSelection,
  indicatorWordIndex,
  ...restProps
}) => {
  return (
    <AdvancedTextViewNativeComponent
      {...restProps}
      style={style}
      text={text}
      highlightedWords={highlightedWords}
      menuOptions={menuOptions}
      onWordPress={onWordPress}
      onSelection={onSelection}
      indicatorWordIndex={indicatorWordIndex}
    />
  );
};
