#import "AdvancedTextView.h"

#import <react/renderer/components/AdvancedTextViewSpec/ComponentDescriptors.h>
#import <react/renderer/components/AdvancedTextViewSpec/EventEmitters.h>
#import <react/renderer/components/AdvancedTextViewSpec/Props.h>
#import <react/renderer/components/AdvancedTextViewSpec/RCTComponentViewHelpers.h>

#import "RCTFabricComponentsPlugins.h"

using namespace facebook::react;

@interface AdvancedTextView () <RCTAdvancedTextViewViewProtocol, UIGestureRecognizerDelegate, UITextViewDelegate>

@property (nonatomic, strong) UITextView *textView;
@property (nonatomic, strong) NSMutableArray<NSDictionary *> *wordRanges;
@property (nonatomic, strong) NSMutableDictionary<NSNumber *, UIColor *> *highlightColors;
@property (nonatomic, strong) NSArray<NSString *> *menuOptions;
@property (nonatomic, assign) NSInteger indicatorWordIndex;

@end

@implementation AdvancedTextView

+ (ComponentDescriptorProvider)componentDescriptorProvider
{
    NSLog(@"[AdvancedTextView] componentDescriptorProvider called");
    return concreteComponentDescriptorProvider<AdvancedTextViewComponentDescriptor>();
}

- (instancetype)initWithFrame:(CGRect)frame
{
    NSLog(@"[AdvancedTextView] initWithFrame called");
    if (self = [super initWithFrame:frame]) {
        @try {
            static const auto defaultProps = std::make_shared<const AdvancedTextViewProps>();
            _props = defaultProps;

            _wordRanges = [NSMutableArray array];
            _highlightColors = [NSMutableDictionary dictionary];
            _indicatorWordIndex = -1;

            [self setupTextView];
            [self setupGestureRecognizers];

            NSLog(@"[AdvancedTextView] Initialization successful");
        } @catch (NSException *exception) {
            NSLog(@"[AdvancedTextView] Exception in init: %@", exception);
            @throw;
        }
    }

    return self;
}

- (void)setupTextView
{
    NSLog(@"[AdvancedTextView] setupTextView called");
    @try {
        _textView = [[UITextView alloc] initWithFrame:self.bounds];
        _textView.editable = NO;
        _textView.selectable = YES; // Allow text selection
        _textView.scrollEnabled = YES;
        _textView.backgroundColor = [UIColor clearColor];
        _textView.textContainerInset = UIEdgeInsetsMake(8, 8, 8, 8);
        _textView.font = [UIFont systemFontOfSize:16];
        _textView.textColor = [UIColor labelColor];
        _textView.delegate = self;

        self.contentView = _textView;
        NSLog(@"[AdvancedTextView] TextView setup successful");
    } @catch (NSException *exception) {
        NSLog(@"[AdvancedTextView] Exception in setupTextView: %@", exception);
        @throw;
    }
}

- (void)setupGestureRecognizers
{
    NSLog(@"[AdvancedTextView] setupGestureRecognizers called");
    @try {
        // Single tap for word selection
        UITapGestureRecognizer *tapGesture = [[UITapGestureRecognizer alloc]
                                              initWithTarget:self
                                              action:@selector(handleTap:)];
        tapGesture.delegate = self;
        [_textView addGestureRecognizer:tapGesture];

        NSLog(@"[AdvancedTextView] Gesture recognizers setup successful");
    } @catch (NSException *exception) {
        NSLog(@"[AdvancedTextView] Exception in setupGestureRecognizers: %@", exception);
        @throw;
    }
}

- (void)updateProps:(Props::Shared const &)props oldProps:(Props::Shared const &)oldProps
{
    NSLog(@"[AdvancedTextView] updateProps called");
    @try {
        const auto &oldViewProps = *std::static_pointer_cast<AdvancedTextViewProps const>(_props);
        const auto &newViewProps = *std::static_pointer_cast<AdvancedTextViewProps const>(props);

        BOOL textChanged = NO;
        BOOL highlightsChanged = NO;
        BOOL menuChanged = NO;
        BOOL indicatorChanged = NO;

        // Check text change
        if (oldViewProps.text != newViewProps.text) {
            textChanged = YES;
            NSLog(@"[AdvancedTextView] Text changed: %s", newViewProps.text.c_str());
        }

        // Check highlighted words change
        if (oldViewProps.highlightedWords.size() != newViewProps.highlightedWords.size()) {
            highlightsChanged = YES;
            NSLog(@"[AdvancedTextView] Highlights size changed: %zu -> %zu",
                  oldViewProps.highlightedWords.size(), newViewProps.highlightedWords.size());
        } else {
            for (size_t i = 0; i < oldViewProps.highlightedWords.size(); i++) {
                const auto &oldHW = oldViewProps.highlightedWords[i];
                const auto &newHW = newViewProps.highlightedWords[i];
                if (oldHW.index != newHW.index || oldHW.highlightColor != newHW.highlightColor) {
                    highlightsChanged = YES;
                    NSLog(@"[AdvancedTextView] Highlight changed at index %zu", i);
                    break;
                }
            }
        }

        // Check menu options change
        if (oldViewProps.menuOptions.size() != newViewProps.menuOptions.size()) {
            menuChanged = YES;
            NSLog(@"[AdvancedTextView] Menu options size changed: %zu -> %zu",
                  oldViewProps.menuOptions.size(), newViewProps.menuOptions.size());
        } else {
            for (size_t i = 0; i < oldViewProps.menuOptions.size(); i++) {
                if (oldViewProps.menuOptions[i] != newViewProps.menuOptions[i]) {
                    menuChanged = YES;
                    NSLog(@"[AdvancedTextView] Menu option changed at index %zu", i);
                    break;
                }
            }
        }

        // Check indicator change
        if (oldViewProps.indicatorWordIndex != newViewProps.indicatorWordIndex) {
            indicatorChanged = YES;
            NSLog(@"[AdvancedTextView] Indicator changed: %d -> %d",
                  oldViewProps.indicatorWordIndex, newViewProps.indicatorWordIndex);
        }

        // Apply updates
        if (textChanged) {
            NSString *text = [NSString stringWithUTF8String:newViewProps.text.c_str()];
            [self updateTextContent:text];
        }

        if (highlightsChanged) {
            [self updateHighlightedWords:newViewProps.highlightedWords];
        }

        if (menuChanged) {
            [self updateMenuOptions:newViewProps.menuOptions];
        }

        if (indicatorChanged) {
            _indicatorWordIndex = newViewProps.indicatorWordIndex;
            [self updateTextAppearance];
        }

        [super updateProps:props oldProps:oldProps];
        NSLog(@"[AdvancedTextView] updateProps completed successfully");
    } @catch (NSException *exception) {
        NSLog(@"[AdvancedTextView] Exception in updateProps: %@, reason: %@",
              exception.name, exception.reason);
        @throw;
    }
}

- (void)updateTextContent:(NSString *)text
{
    NSLog(@"[AdvancedTextView] updateTextContent called with text length: %lu",
          (unsigned long)text.length);
    @try {
        if (!text) {
            NSLog(@"[AdvancedTextView] Text is nil, skipping update");
            return;
        }

        // IMPORTANT: Set the text first!
        _textView.text = text;
        NSLog(@"[AdvancedTextView] Set textView.text with length: %lu", (unsigned long)_textView.text.length);

        // Parse text into words and their ranges
        [_wordRanges removeAllObjects];

        NSRange searchRange = NSMakeRange(0, text.length);
        NSCharacterSet *whitespaceSet = [NSCharacterSet whitespaceAndNewlineCharacterSet];

        NSInteger wordIndex = 0;
        while (searchRange.location < text.length) {
            // Skip whitespace
            while (searchRange.location < text.length &&
                   [whitespaceSet characterIsMember:[text characterAtIndex:searchRange.location]]) {
                searchRange.location++;
                searchRange.length = text.length - searchRange.location;
            }

            if (searchRange.location >= text.length) break;

            // Find word end
            NSUInteger wordStart = searchRange.location;
            while (searchRange.location < text.length &&
                   ![whitespaceSet characterIsMember:[text characterAtIndex:searchRange.location]]) {
                searchRange.location++;
            }

            NSRange wordRange = NSMakeRange(wordStart, searchRange.location - wordStart);
            NSString *word = [text substringWithRange:wordRange];

            [_wordRanges addObject:@{
                @"word": word,
                @"range": [NSValue valueWithRange:wordRange],
                @"index": @(wordIndex)
            }];

            wordIndex++;
            searchRange.length = text.length - searchRange.location;
        }

        NSLog(@"[AdvancedTextView] Parsed %ld words", (long)_wordRanges.count);
        [self updateTextAppearance];
    } @catch (NSException *exception) {
        NSLog(@"[AdvancedTextView] Exception in updateTextContent: %@", exception);
        @throw;
    }
}

- (void)updateHighlightedWords:(const std::vector<AdvancedTextViewHighlightedWordsStruct> &)highlightedWords
{
    NSLog(@"[AdvancedTextView] updateHighlightedWords called with %zu highlights",
          highlightedWords.size());
    @try {
        [_highlightColors removeAllObjects];

        for (const auto &hw : highlightedWords) {
            NSInteger index = hw.index;
            NSString *colorString = [NSString stringWithUTF8String:hw.highlightColor.c_str()];
            UIColor *color = [self hexStringToColor:colorString];

            if (color) {
                _highlightColors[@(index)] = color;
                NSLog(@"[AdvancedTextView] Added highlight for word %ld with color %@",
                      (long)index, colorString);
            } else {
                NSLog(@"[AdvancedTextView] Failed to parse color: %@", colorString);
            }
        }

        [self updateTextAppearance];
    } @catch (NSException *exception) {
        NSLog(@"[AdvancedTextView] Exception in updateHighlightedWords: %@", exception);
        @throw;
    }
}

- (void)updateMenuOptions:(const std::vector<std::string> &)options
{
    NSLog(@"[AdvancedTextView] updateMenuOptions called with %zu options", options.size());
    @try {
        NSMutableArray *menuArray = [NSMutableArray array];
        for (const auto &option : options) {
            NSString *optionStr = [NSString stringWithUTF8String:option.c_str()];
            [menuArray addObject:optionStr];
            NSLog(@"[AdvancedTextView] Added menu option: %@", optionStr);
        }
        _menuOptions = [menuArray copy];
    } @catch (NSException *exception) {
        NSLog(@"[AdvancedTextView] Exception in updateMenuOptions: %@", exception);
        @throw;
    }
}

- (void)updateTextAppearance
{
    NSLog(@"[AdvancedTextView] updateTextAppearance called");
    @try {
        if (_wordRanges.count == 0) {
            NSLog(@"[AdvancedTextView] No word ranges, skipping appearance update");
            return;
        }

        if (!_textView.text || _textView.text.length == 0) {
            NSLog(@"[AdvancedTextView] TextView has no text");
            return;
        }

        NSMutableAttributedString *attributedString = [[NSMutableAttributedString alloc]
                                                        initWithString:_textView.text];

        // Apply default attributes
        [attributedString addAttribute:NSFontAttributeName
                                 value:[UIFont systemFontOfSize:16]
                                 range:NSMakeRange(0, attributedString.length)];

        [attributedString addAttribute:NSForegroundColorAttributeName
                                 value:[UIColor labelColor]
                                 range:NSMakeRange(0, attributedString.length)];

        // Apply highlights
        for (NSDictionary *wordInfo in _wordRanges) {
            NSNumber *index = wordInfo[@"index"];
            NSValue *rangeValue = wordInfo[@"range"];
            NSRange range = [rangeValue rangeValue];

            // Validate range
            if (range.location + range.length > attributedString.length) {
                NSLog(@"[AdvancedTextView] Invalid range at index %@: {%lu, %lu} for string length %lu",
                      index, (unsigned long)range.location, (unsigned long)range.length,
                      (unsigned long)attributedString.length);
                continue;
            }

            UIColor *highlightColor = _highlightColors[index];
            if (highlightColor) {
                [attributedString addAttribute:NSBackgroundColorAttributeName
                                         value:highlightColor
                                         range:range];
            }

            // Add background color indicator for indicated word
            if (_indicatorWordIndex >= 0 && [index integerValue] == _indicatorWordIndex) {
                // Use a semi-transparent blue background for the current word
                UIColor *indicatorColor = [[UIColor systemBlueColor] colorWithAlphaComponent:0.3];
                [attributedString addAttribute:NSBackgroundColorAttributeName
                                         value:indicatorColor
                                         range:range];
            }
        }

        _textView.attributedText = attributedString;
        NSLog(@"[AdvancedTextView] Text appearance updated successfully");
    } @catch (NSException *exception) {
        NSLog(@"[AdvancedTextView] Exception in updateTextAppearance: %@, reason: %@",
              exception.name, exception.reason);
        @throw;
    }
}

- (void)handleTap:(UITapGestureRecognizer *)gesture
{
    NSLog(@"[AdvancedTextView] handleTap called");
    @try {
        if (gesture.state != UIGestureRecognizerStateEnded) return;

        CGPoint location = [gesture locationInView:_textView];
        NSInteger wordIndex = [self wordIndexAtPoint:location];

        NSLog(@"[AdvancedTextView] Tap at point: {%.2f, %.2f}, word index: %ld",
              location.x, location.y, (long)wordIndex);

        // Dismiss any existing selection
        _textView.selectedTextRange = nil;

        if (wordIndex >= 0 && wordIndex < _wordRanges.count) {
            NSDictionary *wordInfo = _wordRanges[wordIndex];
            NSString *word = wordInfo[@"word"];

            NSLog(@"[AdvancedTextView] Word pressed: %@", word);
            [self emitWordPressEvent:word index:wordIndex];
        }
    } @catch (NSException *exception) {
        NSLog(@"[AdvancedTextView] Exception in handleTap: %@", exception);
    }
}

#pragma mark - UITextViewDelegate

- (BOOL)textView:(UITextView *)textView shouldInteractWithURL:(NSURL *)URL inRange:(NSRange)characterRange interaction:(UITextItemInteraction)interaction
{
    return YES;
}

- (void)textViewDidChangeSelection:(UITextView *)textView
{
    NSLog(@"[AdvancedTextView] Selection changed");

    // Get selected text
    NSString *selectedText = [textView.text substringWithRange:textView.selectedRange];

    if (selectedText.length > 0) {
        NSLog(@"[AdvancedTextView] Selected text: %@", selectedText);

        // Add custom menu items
        if (_menuOptions && _menuOptions.count > 0) {
            [self setupCustomMenuItems];
        }
    }
}

- (void)setupCustomMenuItems
{
    NSLog(@"[AdvancedTextView] Setting up custom menu items");

    UIMenuController *menuController = [UIMenuController sharedMenuController];
    NSMutableArray *customItems = [NSMutableArray array];

    for (NSString *option in _menuOptions) {
        UIMenuItem *item = [[UIMenuItem alloc] initWithTitle:option
                                                      action:@selector(handleCustomMenuAction:)];
        [customItems addObject:item];
    }

    menuController.menuItems = customItems;
}

- (void)handleCustomMenuAction:(UIMenuItem *)sender
{
    NSLog(@"[AdvancedTextView] Custom menu action: %@", sender.title);

    NSString *selectedText = [_textView.text substringWithRange:_textView.selectedRange];
    [self emitSelectionEvent:selectedText menuOption:sender.title];

    // Clear selection after action
    _textView.selectedTextRange = nil;
}

- (BOOL)canPerformAction:(SEL)action withSender:(id)sender
{
    // Allow custom menu items
    if (action == @selector(handleCustomMenuAction:)) {
        return YES;
    }

    // Allow default actions (copy, etc.)
    if (action == @selector(copy:) ||
        action == @selector(selectAll:) ||
        action == @selector(select:)) {
        return [super canPerformAction:action withSender:sender];
    }

    return NO;
}

- (void)handleLongPress:(UILongPressGestureRecognizer *)gesture
{
    // Removed - using native iOS text selection menu instead
}

- (NSInteger)wordIndexAtPoint:(CGPoint)point
{
    @try {
        if (!_textView.layoutManager || !_textView.textContainer) {
            NSLog(@"[AdvancedTextView] Layout manager or text container is nil");
            return -1;
        }

        // Adjust point for text container insets
        point.x -= _textView.textContainerInset.left;
        point.y -= _textView.textContainerInset.top;

        NSLayoutManager *layoutManager = _textView.layoutManager;
        NSTextContainer *textContainer = _textView.textContainer;

        NSUInteger characterIndex = [layoutManager characterIndexForPoint:point
                                                           inTextContainer:textContainer
                                  fractionOfDistanceBetweenInsertionPoints:nil];

        NSLog(@"[AdvancedTextView] Character index at point: %lu", (unsigned long)characterIndex);

        // Find which word this character belongs to
        for (NSDictionary *wordInfo in _wordRanges) {
            NSValue *rangeValue = wordInfo[@"range"];
            NSRange range = [rangeValue rangeValue];

            if (NSLocationInRange(characterIndex, range)) {
                return [wordInfo[@"index"] integerValue];
            }
        }

        return -1;
    } @catch (NSException *exception) {
        NSLog(@"[AdvancedTextView] Exception in wordIndexAtPoint: %@", exception);
        return -1;
    }
}

- (void)showContextMenuForWord:(NSString *)word atIndex:(NSInteger)index location:(CGPoint)location
{
    // Removed - using native iOS text selection menu instead
}

- (UIViewController *)findViewController
{
    // Removed - no longer needed
    return nil;
}

- (void)emitWordPressEvent:(NSString *)word index:(NSInteger)index
{
    NSLog(@"[AdvancedTextView] emitWordPressEvent: %@ at index: %ld", word, (long)index);
    @try {
        if (_eventEmitter) {
            auto emitter = std::static_pointer_cast<const AdvancedTextViewEventEmitter>(_eventEmitter);

            AdvancedTextViewEventEmitter::OnWordPress event;
            event.word = [word UTF8String];
            event.index = static_cast<int>(index);

            emitter->onWordPress(event);
            NSLog(@"[AdvancedTextView] Word press event emitted successfully");
        } else {
            NSLog(@"[AdvancedTextView] Event emitter is null");
        }
    } @catch (NSException *exception) {
        NSLog(@"[AdvancedTextView] Exception in emitWordPressEvent: %@", exception);
    }
}

- (void)emitSelectionEvent:(NSString *)selectedText menuOption:(NSString *)option
{
    NSLog(@"[AdvancedTextView] emitSelectionEvent: %@ with option: %@", selectedText, option);
    @try {
        if (_eventEmitter) {
            auto emitter = std::static_pointer_cast<const AdvancedTextViewEventEmitter>(_eventEmitter);

            AdvancedTextViewEventEmitter::OnSelection event;
            event.selectedText = [selectedText UTF8String];
            event.event = [option UTF8String];

            emitter->onSelection(event);
            NSLog(@"[AdvancedTextView] Selection event emitted successfully");
        } else {
            NSLog(@"[AdvancedTextView] Event emitter is null");
        }
    } @catch (NSException *exception) {
        NSLog(@"[AdvancedTextView] Exception in emitSelectionEvent: %@", exception);
    }
}

- (void)layoutSubviews
{
    NSLog(@"[AdvancedTextView] layoutSubviews called");
    @try {
        [super layoutSubviews];
        _textView.frame = self.bounds;
    } @catch (NSException *exception) {
        NSLog(@"[AdvancedTextView] Exception in layoutSubviews: %@", exception);
    }
}

- (UIColor *)hexStringToColor:(NSString *)stringToConvert
{
    @try {
        if (!stringToConvert || [stringToConvert length] == 0) {
            NSLog(@"[AdvancedTextView] Empty color string");
            return nil;
        }

        NSString *noHashString = [stringToConvert stringByReplacingOccurrencesOfString:@"#" withString:@""];
        NSScanner *stringScanner = [NSScanner scannerWithString:noHashString];

        unsigned hex;
        if (![stringScanner scanHexInt:&hex]) {
            NSLog(@"[AdvancedTextView] Failed to parse hex color: %@", stringToConvert);
            return nil;
        }

        int r = (hex >> 16) & 0xFF;
        int g = (hex >> 8) & 0xFF;
        int b = (hex) & 0xFF;

        return [UIColor colorWithRed:r / 255.0f green:g / 255.0f blue:b / 255.0f alpha:1.0f];
    } @catch (NSException *exception) {
        NSLog(@"[AdvancedTextView] Exception in hexStringToColor: %@", exception);
        return nil;
    }
}

Class<RCTComponentViewProtocol> AdvancedTextViewCls(void)
{
    NSLog(@"[AdvancedTextView] AdvancedTextViewCls called");
    return AdvancedTextView.class;
}

- (void)dealloc
{
    NSLog(@"[AdvancedTextView] dealloc called");
}

@end
