import React from 'react';
import AdvancedTextViewNativeComponent, {
  type NativeProps,
} from './AdvancedTextViewNativeComponent';

export const AdvancedText: React.FC<NativeProps> = ({
  text,
  style,
  highlightedWords,
  menuOptions,
  onWordPress,
  onWordDoublePress,
  onWordLongPress,
  onSelection,
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
      onWordDoublePress={onWordDoublePress}
      onWordLongPress={onWordLongPress}
      onSelection={onSelection}
      // Only apps that handle double taps pay the single-tap delay.
      wordDoublePressEnabled={!!onWordDoublePress}
    />
  );
};
