#import "AdvancedTextView.h"

#import <react/renderer/components/AdvancedTextViewSpec/ComponentDescriptors.h>
#import <react/renderer/components/AdvancedTextViewSpec/EventEmitters.h>
#import <react/renderer/components/AdvancedTextViewSpec/Props.h>
#import <react/renderer/components/AdvancedTextViewSpec/RCTComponentViewHelpers.h>

#import "RCTFabricComponentsPlugins.h"

using namespace facebook::react;


// Forward declaration
@class AdvancedTextView;

@interface AdvancedTextView () <RCTAdvancedTextViewViewProtocol, UIGestureRecognizerDelegate, UITextViewDelegate>

@property (nonatomic, strong) NSMutableArray<NSDictionary *> *wordRanges;
@property (nonatomic, strong) NSMutableDictionary<NSNumber *, UIColor *> *highlightColors;
@property (nonatomic, strong) NSArray<NSString *> *menuOptions;
@property (nonatomic, assign) NSInteger indicatorWordIndex;

// ✅ ADD THIS LINE
- (void)handleCustomMenuAction:(UIMenuItem *)sender;

@end


// Custom UITextView subclass to override menu behavior
@interface CustomTextView : UITextView
@property (nonatomic, weak) AdvancedTextView *parentView;
@end

@implementation CustomTextView

- (BOOL)canPerformAction:(SEL)action withSender:(id)sender
{
    NSLog(@"[CustomTextView] canPerformAction: %@", NSStringFromSelector(action));

    // Only allow our custom menu actions
    if (action == @selector(handleCustomMenuAction:)) {
        NSLog(@"[CustomTextView] ✅ Allowing custom action");
        return YES;
    }

    // Block ALL system actions
    NSLog(@"[CustomTextView] ❌ Blocking system action: %@", NSStringFromSelector(action));
    return NO;
}

- (void)handleCustomMenuAction:(UIMenuItem *)sender
{
    // Forward to parent view
    if (self.parentView) {

        [self.parentView handleCustomMenuAction:sender];
    }
}

@end



@interface AdvancedTextView () <RCTAdvancedTextViewViewProtocol, UIGestureRecognizerDelegate, UITextViewDelegate>

@property (nonatomic, strong) CustomTextView *textView;

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
        _textView = [[CustomTextView alloc] initWithFrame:self.bounds];
        _textView.parentView = self;
        _textView.editable = NO;
        _textView.selectable = YES;
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
            NSLog(@"[AdvancedTextView] Text changed");
        }

        // Check highlighted words change
        if (oldViewProps.highlightedWords.size() != newViewProps.highlightedWords.size()) {
            highlightsChanged = YES;
        } else {
            for (size_t i = 0; i < oldViewProps.highlightedWords.size(); i++) {
                const auto &oldHW = oldViewProps.highlightedWords[i];
                const auto &newHW = newViewProps.highlightedWords[i];
                if (oldHW.index != newHW.index || oldHW.highlightColor != newHW.highlightColor) {
                    highlightsChanged = YES;
                    break;
                }
            }
        }

        // Check menu options change
        if (oldViewProps.menuOptions.size() != newViewProps.menuOptions.size()) {
            menuChanged = YES;
        } else {
            for (size_t i = 0; i < oldViewProps.menuOptions.size(); i++) {
                if (oldViewProps.menuOptions[i] != newViewProps.menuOptions[i]) {
                    menuChanged = YES;
                    break;
                }
            }
        }

        // Check indicator change
        if (oldViewProps.indicatorWordIndex != newViewProps.indicatorWordIndex) {
            indicatorChanged = YES;
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
        NSLog(@"[AdvancedTextView] Exception in updateProps: %@", exception.reason);
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

        _textView.text = text;

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
    @try {
        if (_wordRanges.count == 0 || !_textView.text || _textView.text.length == 0) {
            return;
        }

        NSMutableAttributedString *attributedString = [[NSMutableAttributedString alloc]
                                                        initWithString:_textView.text];

        [attributedString addAttribute:NSFontAttributeName
                                 value:[UIFont systemFontOfSize:16]
                                 range:NSMakeRange(0, attributedString.length)];

        [attributedString addAttribute:NSForegroundColorAttributeName
                                 value:[UIColor labelColor]
                                 range:NSMakeRange(0, attributedString.length)];

        for (NSDictionary *wordInfo in _wordRanges) {
            NSNumber *index = wordInfo[@"index"];
            NSValue *rangeValue = wordInfo[@"range"];
            NSRange range = [rangeValue rangeValue];

            if (range.location + range.length > attributedString.length) {
                continue;
            }

            UIColor *highlightColor = _highlightColors[index];
            if (highlightColor) {
                [attributedString addAttribute:NSBackgroundColorAttributeName
                                         value:highlightColor
                                         range:range];
            }

            if (_indicatorWordIndex >= 0 && [index integerValue] == _indicatorWordIndex) {
                UIColor *indicatorColor = [[UIColor systemBlueColor] colorWithAlphaComponent:0.3];
                [attributedString addAttribute:NSBackgroundColorAttributeName
                                         value:indicatorColor
                                         range:range];
            }
        }

        _textView.attributedText = attributedString;
    } @catch (NSException *exception) {
        NSLog(@"[AdvancedTextView] Exception in updateTextAppearance: %@", exception.reason);
    }
}

- (void)handleTap:(UITapGestureRecognizer *)gesture
{
    @try {
        if (gesture.state != UIGestureRecognizerStateEnded) return;

        CGPoint location = [gesture locationInView:_textView];
        NSInteger wordIndex = [self wordIndexAtPoint:location];

        // Dismiss any existing selection
        _textView.selectedTextRange = nil;

        if (wordIndex >= 0 && wordIndex < _wordRanges.count) {
            NSDictionary *wordInfo = _wordRanges[wordIndex];
            NSString *word = wordInfo[@"word"];

            [self emitWordPressEvent:word index:wordIndex];
        }
    } @catch (NSException *exception) {
        NSLog(@"[AdvancedTextView] Exception in handleTap: %@", exception);
    }
}

#pragma mark - UITextViewDelegate

- (void)textViewDidChangeSelection:(UITextView *)textView
{
    NSString *selectedText = [textView.text substringWithRange:textView.selectedRange];

    if (selectedText.length > 0) {
        NSLog(@"[AdvancedTextView] Selected text: %@", selectedText);

        if (_menuOptions && _menuOptions.count > 0) {
            [self setupCustomMenuItems];
        }
    }
}

- (void)setupCustomMenuItems
{
    NSLog(@"[AdvancedTextView] Setting up %lu custom menu items", (unsigned long)_menuOptions.count);

    UIMenuController *menuController = [UIMenuController sharedMenuController];
    NSMutableArray *customItems = [NSMutableArray array];

    for (NSString *option in _menuOptions) {
        UIMenuItem *item = [[UIMenuItem alloc] initWithTitle:option
                                                      action:@selector(handleCustomMenuAction:)];
        [customItems addObject:item];
        NSLog(@"[AdvancedTextView] Created menu item: %@", option);
    }

    menuController.menuItems = customItems;
}


- (void)handleCustomMenuAction:(UIMenuItem *)sender
{
    NSLog(@"[AdvancedTextView] Custom menu action: %@", sender.title);

    NSString *selectedText = [_textView.text substringWithRange:_textView.selectedRange];

    [self emitSelectionEvent:selectedText menuOption:sender.title];

    dispatch_async(dispatch_get_main_queue(), ^{
        self->_textView.selectedTextRange = nil;
        NSLog(@"[AdvancedTextView] Selection cleared");
    });
}

- (NSInteger)wordIndexAtPoint:(CGPoint)point
{
    @try {
        if (!_textView.layoutManager || !_textView.textContainer) {
            return -1;
        }

        point.x -= _textView.textContainerInset.left;
        point.y -= _textView.textContainerInset.top;

        NSLayoutManager *layoutManager = _textView.layoutManager;
        NSTextContainer *textContainer = _textView.textContainer;

        NSUInteger characterIndex = [layoutManager characterIndexForPoint:point
                                                           inTextContainer:textContainer
                                  fractionOfDistanceBetweenInsertionPoints:nil];

        for (NSDictionary *wordInfo in _wordRanges) {
            NSValue *rangeValue = wordInfo[@"range"];
            NSRange range = [rangeValue rangeValue];

            if (NSLocationInRange(characterIndex, range)) {
                return [wordInfo[@"index"] integerValue];
            }
        }

        return -1;
    } @catch (NSException *exception) {
        return -1;
    }
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
        }
    } @catch (NSException *exception) {
        NSLog(@"[AdvancedTextView] Exception in emitSelectionEvent: %@", exception);
    }
}

- (void)layoutSubviews
{
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
            return nil;
        }

        NSString *noHashString = [stringToConvert stringByReplacingOccurrencesOfString:@"#" withString:@""];
        NSScanner *stringScanner = [NSScanner scannerWithString:noHashString];

        unsigned hex;
        if (![stringScanner scanHexInt:&hex]) {
            return nil;
        }

        int r = (hex >> 16) & 0xFF;
        int g = (hex >> 8) & 0xFF;
        int b = (hex) & 0xFF;

        return [UIColor colorWithRed:r / 255.0f green:g / 255.0f blue:b / 255.0f alpha:1.0f];
    } @catch (NSException *exception) {
        return nil;
    }
}

Class<RCTComponentViewProtocol> AdvancedTextViewCls(void)
{
    return AdvancedTextView.class;
}

- (void)dealloc
{
    NSLog(@"[AdvancedTextView] dealloc called");
}

@end
