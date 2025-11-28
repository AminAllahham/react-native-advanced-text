import type { NativeSyntheticEvent, ViewProps } from 'react-native';
import AdvancedTextViewNativeComponent from './AdvancedTextViewNativeComponent';

interface HighlightedWord {
  index: number;
  highlightColor: string;
}

interface NativeProps extends ViewProps {
  text: string;
  highlightedWords?: ReadonlyArray<HighlightedWord>;
  color: string;
  fontSize: number;

  menuOptions?: ReadonlyArray<string>;
  onWordPress?: (
    event: NativeSyntheticEvent<{ word: string; index: string }>
  ) => void;
  onSelection?: (
    event: NativeSyntheticEvent<{ selectedText: string; event: string }>
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
  color,
  fontSize,
}) => {
  return (
    <AdvancedTextViewNativeComponent
      text={text}
      highlightedWords={highlightedWords}
      menuOptions={menuOptions}
      onWordPress={onWordPress}
      onSelection={onSelection}
      indicatorWordIndex={indicatorWordIndex}
      color={color}
      fontSize={fontSize}
    />
  );
};
