import type { NativeSyntheticEvent, TextProps } from 'react-native';
import AdvancedTextViewNativeComponent from './AdvancedTextViewNativeComponent';

interface HighlightedWord {
  index: number;
  highlightColor: string;
}

interface NativeProps extends TextProps {
  text: string;
  highlightedWords?: ReadonlyArray<HighlightedWord>;
  menuOptions?: ReadonlyArray<string>;
  onWordPress?: (event: NativeSyntheticEvent<{ word: string }>) => void;
  onSelection?: (
    event: NativeSyntheticEvent<{ selectedText: string; eventType: string }>
  ) => void;
  indicatorWordIndex?: number;
}

export const AdvancedText: React.FC<NativeProps> = ({
  text,
  highlightedWords,
  menuOptions,
  onWordPress,
  onSelection,
  indicatorWordIndex,
  ...restProps
}) => {
  return (
    <AdvancedTextViewNativeComponent
      text={text}
      highlightedWords={highlightedWords}
      menuOptions={menuOptions}
      onWordPress={onWordPress}
      onSelection={onSelection}
      indicatorWordIndex={indicatorWordIndex}
      {...restProps}
    />
  );
};
