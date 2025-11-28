import type { NativeSyntheticEvent, ViewProps } from 'react-native';
import AdvancedTextViewNativeComponent from './AdvancedTextViewNativeComponent';

interface HighlightedWord {
  index: number;
  highlightColor: string;
}

interface NativeProps extends ViewProps {
  text: string;
  highlightedWords?: ReadonlyArray<HighlightedWord>;
  fontSize?: number;
  color?: string;
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
  fontSize,
  color,
}) => {
  return (
    <AdvancedTextViewNativeComponent
      text={text}
      highlightedWords={highlightedWords}
      menuOptions={menuOptions}
      onWordPress={onWordPress}
      onSelection={onSelection}
      indicatorWordIndex={indicatorWordIndex}
      fontSize={fontSize}
      color={color}
    />
  );
};
