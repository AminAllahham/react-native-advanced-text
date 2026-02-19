#import "AdvancedTextView.h"

#import <react/renderer/components/AdvancedTextViewSpec/ComponentDescriptors.h>
#import <react/renderer/components/AdvancedTextViewSpec/EventEmitters.h>
#import <react/renderer/components/AdvancedTextViewSpec/Props.h>
#import <react/renderer/components/AdvancedTextViewSpec/RCTComponentViewHelpers.h>

#import "RCTFabricComponentsPlugins.h"

using namespace facebook::react;


@class AdvancedTextView;

@interface AdvancedTextView () <RCTAdvancedTextViewViewProtocol, UIGestureRecognizerDelegate, UITextViewDelegate>

@property (nonatomic, strong) NSMutableArray<NSDictionary *> *wordRanges;
@property (nonatomic, strong) NSMutableDictionary<NSNumber *, UIColor *> *highlightColors;
@property (nonatomic, strong) NSArray<NSString *> *menuOptions;
@property (nonatomic, assign) NSInteger indicatorWordIndex;
@property (nonatomic, assign) CGFloat fontSize;
@property (nonatomic, strong) NSString *fontWeight;
@property (nonatomic, strong) UIColor *textColor;
@property (nonatomic, strong) NSString *textAlign;
@property (nonatomic, strong) NSString *fontFamily;
@property (nonatomic, assign) CGFloat lineHeight;

- (void)handleCustomMenuAction:(UIMenuItem *)sender;

@end


@interface CustomTextView : UITextView
@property (nonatomic, weak) AdvancedTextView *parentView;
@end

@implementation CustomTextView

- (BOOL)canPerformAction:(SEL)action withSender:(id)sender
{
    NSLog(@"[CustomTextView] canPerformAction: %@", NSStringFromSelector(action));

    if (action == @selector(handleCustomMenuAction:)) {
        NSLog(@"[CustomTextView] ✅ Allowing custom action");
        return YES;
    }

    NSLog(@"[CustomTextView] ❌ Blocking system action: %@", NSStringFromSelector(action));
    return NO;
}

- (void)handleCustomMenuAction:(UIMenuItem *)sender
{
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
            _fontSize = 16.0;
            _fontWeight = @"normal";
            _textColor = [UIColor labelColor];
            _textAlign = @"left";
            _fontFamily = @"System";
            _lineHeight = 0.0;

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
        BOOL styleChanged = NO;

        if (oldViewProps.fontSize != newViewProps.fontSize && newViewProps.fontSize) {
            NSLog(@"[AdvancedTextView] Updating fontSize to: %f", newViewProps.fontSize);
            _fontSize = static_cast<CGFloat>(newViewProps.fontSize);
            styleChanged = YES;
        }

        if (oldViewProps.textAlign != newViewProps.textAlign && !newViewProps.textAlign.empty()) {
            NSLog(@"[AdvancedTextView] Updating textAlign to: %s", newViewProps.textAlign.c_str());
            _textAlign = [NSString stringWithUTF8String:newViewProps.textAlign.c_str()];
            styleChanged = YES;
        }

        if (oldViewProps.fontWeight != newViewProps.fontWeight && !newViewProps.fontWeight.empty()) {
            NSLog(@"[AdvancedTextView] Updating fontWeight to: %s", newViewProps.fontWeight.c_str());
            _fontWeight = [NSString stringWithUTF8String:newViewProps.fontWeight.c_str()];
            styleChanged = YES;
        }

        if (oldViewProps.fontFamily != newViewProps.fontFamily && !newViewProps.fontFamily.empty()) {
            NSLog(@"[AdvancedTextView] Updating fontFamily to: %s", newViewProps.fontFamily.c_str());
            _fontFamily = [NSString stringWithUTF8String:newViewProps.fontFamily.c_str()];
            styleChanged = YES;
        }

        if (oldViewProps.color != newViewProps.color && !newViewProps.color.empty()) {
            NSLog(@"[AdvancedTextView] Updating color to: %s", newViewProps.color.c_str());
            NSString *colorStr = [NSString stringWithUTF8String:newViewProps.color.c_str()];
            _textColor = [self hexStringToColor:colorStr];
            styleChanged = YES;
        }

        if (oldViewProps.text != newViewProps.text) {
            textChanged = YES;
            NSLog(@"[AdvancedTextView] Text changed");
        }

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

        if (oldViewProps.indicatorWordIndex != newViewProps.indicatorWordIndex) {
            indicatorChanged = YES;
        }

        if (oldViewProps.lineHeight != newViewProps.lineHeight) {
            NSLog(@"[AdvancedTextView] Updating lineHeight to: %f", newViewProps.lineHeight);
            _lineHeight = static_cast<CGFloat>(newViewProps.lineHeight);
            styleChanged = YES;
        }

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

        if (styleChanged) {
            NSLog(@"[AdvancedTextView] Style properties changed, updating appearance");
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

        [_wordRanges removeAllObjects];

        NSRange searchRange = NSMakeRange(0, text.length);
        NSCharacterSet *whitespaceSet = [NSCharacterSet whitespaceAndNewlineCharacterSet];

        NSInteger wordIndex = 0;
        NSMutableArray *allMatches = [NSMutableArray array];

        while (searchRange.location < text.length) {
            while (searchRange.location < text.length &&
                [whitespaceSet characterIsMember:[text characterAtIndex:searchRange.location]]) {
                searchRange.location++;
                searchRange.length = text.length - searchRange.location;
            }

            if (searchRange.location >= text.length) break;

            NSUInteger wordStart = searchRange.location;
            while (searchRange.location < text.length &&
                ![whitespaceSet characterIsMember:[text characterAtIndex:searchRange.location]]) {
                searchRange.location++;
            }

            NSRange wordRange = NSMakeRange(wordStart, searchRange.location - wordStart);
            NSString *word = [text substringWithRange:wordRange];

            [allMatches addObject:@{
                @"word": word,
                @"range": [NSValue valueWithRange:wordRange],
                @"index": @(wordIndex)
            }];

            wordIndex++;
            searchRange.length = text.length - searchRange.location;
        }

        for (NSInteger i = 0; i < allMatches.count; i++) {
            NSMutableDictionary *wordInfo = [allMatches[i] mutableCopy];
            NSRange wordRange = [wordInfo[@"range"] rangeValue];

            NSUInteger extendedEnd;
            if (i + 1 < allMatches.count) {
                NSRange nextRange = [allMatches[i + 1][@"range"] rangeValue];
                extendedEnd = nextRange.location;
            } else {
                extendedEnd = text.length;
            }

            NSRange extendedRange = NSMakeRange(wordRange.location, extendedEnd - wordRange.location);
            wordInfo[@"extendedRange"] = [NSValue valueWithRange:extendedRange];

            [_wordRanges addObject:[wordInfo copy]];
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
        if (!_textView.text || _textView.text.length == 0) {
            return;
        }

        NSMutableAttributedString *attributedString = [[NSMutableAttributedString alloc]
                                                        initWithString:_textView.text];

        if (_lineHeight > 0) {
            NSMutableParagraphStyle *paragraphStyle = [[NSMutableParagraphStyle alloc] init];

            UIFont *font = nil;
            if (_fontFamily && _fontFamily.length > 0) {
                font = [UIFont fontWithName:_fontFamily size:_fontSize > 0 ? _fontSize : 16.0];
            }
            if (!font) {
                font = [UIFont systemFontOfSize:_fontSize > 0 ? _fontSize : 16.0];
            }

            CGFloat lineSpacing = (_lineHeight * font.lineHeight) - font.lineHeight;
            paragraphStyle.lineSpacing = lineSpacing;

            [attributedString addAttribute:NSParagraphStyleAttributeName
                                     value:paragraphStyle
                                     range:NSMakeRange(0, attributedString.length)];
        }

        UIFont *font = nil;

        if (_fontFamily && _fontFamily.length > 0) {
            font = [UIFont fontWithName:_fontFamily size:_fontSize > 0 ? _fontSize : 16.0];
        }

        if (!font) {
            if (_fontWeight && [_fontWeight.lowercaseString isEqualToString:@"bold"]) {
                font = [UIFont boldSystemFontOfSize:_fontSize > 0 ? _fontSize : 16.0];
            } else if (_fontWeight && [_fontWeight.lowercaseString isEqualToString:@"italic"]) {
                font = [UIFont italicSystemFontOfSize:_fontSize > 0 ? _fontSize : 16.0];
            } else {
                font = [UIFont systemFontOfSize:_fontSize > 0 ? _fontSize : 16.0];
            }
        }

        [attributedString addAttribute:NSFontAttributeName
                                 value:font
                                 range:NSMakeRange(0, attributedString.length)];


        UIColor *color = _textColor ?: [UIColor labelColor];
        [attributedString addAttribute:NSForegroundColorAttributeName
                                 value:color
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
                NSValue *extendedRangeValue = wordInfo[@"extendedRange"];
                NSRange extendedRange = extendedRangeValue ? [extendedRangeValue rangeValue] : range;

                if (extendedRange.location + extendedRange.length <= attributedString.length) {
                    [attributedString addAttribute:NSBackgroundColorAttributeName
                                            value:highlightColor
                                            range:extendedRange];
                }
            }

            if (_indicatorWordIndex >= 0 && [index integerValue] == _indicatorWordIndex) {
                UIColor *indicatorColor = [[UIColor systemBlueColor] colorWithAlphaComponent:0.3];
                [attributedString addAttribute:NSBackgroundColorAttributeName
                                        value:indicatorColor
                                        range:range];
            }
        }

        _textView.attributedText = attributedString;

        if (_textAlign) {
            if ([_textAlign.lowercaseString isEqualToString:@"center"]) {
                _textView.textAlignment = NSTextAlignmentCenter;
            } else if ([_textAlign.lowercaseString isEqualToString:@"right"]) {
                _textView.textAlignment = NSTextAlignmentRight;
            } else {
                _textView.textAlignment = NSTextAlignmentLeft;
            }
        } else {
            _textView.textAlignment = NSTextAlignmentLeft;
        }

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

- (void)setFontSize:(CGFloat)fontSize {
    _fontSize = fontSize;
    [self updateTextAppearance];
}

- (void)setFontWeight:(NSString *)fontWeight {
    _fontWeight = fontWeight;
    [self updateTextAppearance];
}

- (void)setTextColor:(UIColor *)textColor {
    _textColor = textColor;
    [self updateTextAppearance];
}

- (void)setTextAlign:(NSString *)textAlign {
    _textAlign = textAlign;
    [self updateTextAppearance];
}

- (void)setFontFamily:(NSString *)fontFamily {
    _fontFamily = fontFamily;
    [self updateTextAppearance];
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
