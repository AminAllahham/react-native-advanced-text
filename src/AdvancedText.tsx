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
