#import "AdvancedTextView.h"

#import <react/renderer/components/AdvancedTextViewSpec/ComponentDescriptors.h>
#import <react/renderer/components/AdvancedTextViewSpec/EventEmitters.h>
#import <react/renderer/components/AdvancedTextViewSpec/Props.h>
#import <react/renderer/components/AdvancedTextViewSpec/RCTComponentViewHelpers.h>

#import "RCTFabricComponentsPlugins.h"

using namespace facebook::react;

@interface AdvancedTextView () <RCTAdvancedTextViewViewProtocol, UIGestureRecognizerDelegate>

@property (nonatomic, strong) UITextView *textView;
@property (nonatomic, strong) NSMutableArray<NSDictionary *> *wordRanges;
@property (nonatomic, strong) NSMutableDictionary<NSNumber *, UIColor *> *highlightColors;
@property (nonatomic, strong) NSArray<NSString *> *menuOptions;
@property (nonatomic, assign) NSInteger indicatorWordIndex;

@end

@implementation AdvancedTextView

+ (ComponentDescriptorProvider)componentDescriptorProvider
{
    return concreteComponentDescriptorProvider<AdvancedTextViewComponentDescriptor>();
}

- (instancetype)initWithFrame:(CGRect)frame
{
    if (self = [super initWithFrame:frame]) {
        static const auto defaultProps = std::make_shared<const AdvancedTextViewProps>();
        _props = defaultProps;

        _wordRanges = [NSMutableArray array];
        _highlightColors = [NSMutableDictionary dictionary];
        _indicatorWordIndex = -1;

        [self setupTextView];
        [self setupGestureRecognizers];
    }

    return self;
}

- (void)setupTextView
{
    _textView = [[UITextView alloc] initWithFrame:self.bounds];
    _textView.editable = NO;
    _textView.scrollEnabled = YES;
    _textView.backgroundColor = [UIColor clearColor];
    _textView.textContainerInset = UIEdgeInsetsMake(8, 8, 8, 8);
    _textView.font = [UIFont systemFontOfSize:16];
    _textView.textColor = [UIColor labelColor];

    self.contentView = _textView;
}

- (void)setupGestureRecognizers
{
    // Single tap for word selection
    UITapGestureRecognizer *tapGesture = [[UITapGestureRecognizer alloc]
                                          initWithTarget:self
                                          action:@selector(handleTap:)];
    tapGesture.delegate = self;
    [_textView addGestureRecognizer:tapGesture];

    // Long press for context menu
    UILongPressGestureRecognizer *longPressGesture = [[UILongPressGestureRecognizer alloc]
                                                      initWithTarget:self
                                                      action:@selector(handleLongPress:)];
    longPressGesture.delegate = self;
    [_textView addGestureRecognizer:longPressGesture];
}

- (void)updateProps:(Props::Shared const &)props oldProps:(Props::Shared const &)oldProps
{
    const auto &oldViewProps = *std::static_pointer_cast<AdvancedTextViewProps const>(_props);
    const auto &newViewProps = *std::static_pointer_cast<AdvancedTextViewProps const>(props);

    // Update text
    if (oldViewProps.text != newViewProps.text) {
        NSString *text = [NSString stringWithUTF8String:newViewProps.text.c_str()];
        [self updateTextContent:text];
    }

    // Update highlighted words
    if (oldViewProps.highlightedWords != newViewProps.highlightedWords) {
        [self updateHighlightedWords:newViewProps.highlightedWords];
    }

    // Update menu options
    if (oldViewProps.menuOptions != newViewProps.menuOptions) {
        [self updateMenuOptions:newViewProps.menuOptions];
    }

    // Update indicator word index
    if (oldViewProps.indicatorWordIndex != newViewProps.indicatorWordIndex) {
        _indicatorWordIndex = newViewProps.indicatorWordIndex;
        [self updateTextAppearance];
    }

    [super updateProps:props oldProps:oldProps];
}

- (void)updateTextContent:(NSString *)text
{
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

    [self updateTextAppearance];
}

- (void)updateHighlightedWords:(const std::vector<AdvancedTextViewHighlightedWordsStruct> &)highlightedWords
{
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
}

- (void)updateMenuOptions:(const std::vector<std::string> &)options
{
    NSMutableArray *menuArray = [NSMutableArray array];
    for (const auto &option : options) {
        [menuArray addObject:[NSString stringWithUTF8String:option.c_str()]];
    }
    _menuOptions = [menuArray copy];
}

- (void)updateTextAppearance
{
    if (_wordRanges.count == 0) return;

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

        UIColor *highlightColor = _highlightColors[index];
        if (highlightColor) {
            [attributedString addAttribute:NSBackgroundColorAttributeName
                                     value:highlightColor
                                     range:range];
        }

        // Add indicator (underline or special formatting) for indicated word
        if (_indicatorWordIndex >= 0 && [index integerValue] == _indicatorWordIndex) {
            [attributedString addAttribute:NSUnderlineStyleAttributeName
                                     value:@(NSUnderlineStyleSingle)
                                     range:range];
            [attributedString addAttribute:NSUnderlineColorAttributeName
                                     value:[UIColor systemBlueColor]
                                     range:range];
        }
    }

    _textView.attributedText = attributedString;
}

- (void)handleTap:(UITapGestureRecognizer *)gesture
{
    if (gesture.state != UIGestureRecognizerStateEnded) return;

    CGPoint location = [gesture locationInView:_textView];
    NSInteger wordIndex = [self wordIndexAtPoint:location];

    if (wordIndex >= 0 && wordIndex < _wordRanges.count) {
        NSDictionary *wordInfo = _wordRanges[wordIndex];
        NSString *word = wordInfo[@"word"];

        [self emitWordPressEvent:word index:wordIndex];
    }
}

- (void)handleLongPress:(UILongPressGestureRecognizer *)gesture
{
    if (gesture.state != UIGestureRecognizerStateBegan) return;

    CGPoint location = [gesture locationInView:_textView];
    NSInteger wordIndex = [self wordIndexAtPoint:location];

    if (wordIndex >= 0 && wordIndex < _wordRanges.count) {
        NSDictionary *wordInfo = _wordRanges[wordIndex];
        NSString *word = wordInfo[@"word"];

        [self showContextMenuForWord:word atIndex:wordIndex location:location];
    }
}

- (NSInteger)wordIndexAtPoint:(CGPoint)point
{
    // Adjust point for text container insets
    point.x -= _textView.textContainerInset.left;
    point.y -= _textView.textContainerInset.top;

    NSLayoutManager *layoutManager = _textView.layoutManager;
    NSTextContainer *textContainer = _textView.textContainer;

    NSUInteger characterIndex = [layoutManager characterIndexForPoint:point
                                                       inTextContainer:textContainer
                              fractionOfDistanceBetweenInsertionPoints:nil];

    // Find which word this character belongs to
    for (NSDictionary *wordInfo in _wordRanges) {
        NSValue *rangeValue = wordInfo[@"range"];
        NSRange range = [rangeValue rangeValue];

        if (NSLocationInRange(characterIndex, range)) {
            return [wordInfo[@"index"] integerValue];
        }
    }

    return -1;
}

- (void)showContextMenuForWord:(NSString *)word atIndex:(NSInteger)index location:(CGPoint)location
{
    if (!_menuOptions || _menuOptions.count == 0) return;

    UIAlertController *alert = [UIAlertController alertControllerWithTitle:word
                                                                   message:nil
                                                            preferredStyle:UIAlertControllerStyleActionSheet];

    for (NSString *option in _menuOptions) {
        UIAlertAction *action = [UIAlertAction actionWithTitle:option
                                                         style:UIAlertActionStyleDefault
                                                       handler:^(UIAlertAction * _Nonnull action) {
            [self emitSelectionEvent:word menuOption:option];
        }];
        [alert addAction:action];
    }

    UIAlertAction *cancelAction = [UIAlertAction actionWithTitle:@"Cancel"
                                                           style:UIAlertActionStyleCancel
                                                         handler:nil];
    [alert addAction:cancelAction];

    // Present from the root view controller
    UIViewController *rootVC = [self findViewController];
    if (rootVC) {
        // For iPad, set up popover presentation
        if (alert.popoverPresentationController) {
            alert.popoverPresentationController.sourceView = _textView;
            alert.popoverPresentationController.sourceRect = CGRectMake(location.x, location.y, 1, 1);
        }

        [rootVC presentViewController:alert animated:YES completion:nil];
    }
}

- (UIViewController *)findViewController
{
    UIResponder *responder = self;
    while (responder) {
        if ([responder isKindOfClass:[UIViewController class]]) {
            return (UIViewController *)responder;
        }
        responder = [responder nextResponder];
    }
    return nil;
}

- (void)emitWordPressEvent:(NSString *)word index:(NSInteger)index
{
    if (_eventEmitter) {
        auto emitter = std::static_pointer_cast<const AdvancedTextViewEventEmitter>(_eventEmitter);

        AdvancedTextViewEventEmitter::OnWordPress event;
        event.word = [word UTF8String];
        event.index = static_cast<int>(index);

        emitter->onWordPress(event);
    }
}

- (void)emitSelectionEvent:(NSString *)selectedText menuOption:(NSString *)option
{
    if (_eventEmitter) {
        auto emitter = std::static_pointer_cast<const AdvancedTextViewEventEmitter>(_eventEmitter);

        AdvancedTextViewEventEmitter::OnSelection event;
        event.selectedText = [selectedText UTF8String];
        event.event = [option UTF8String];

        emitter->onSelection(event);
    }
}

- (void)layoutSubviews
{
    [super layoutSubviews];
    _textView.frame = self.bounds;
}

- (UIColor *)hexStringToColor:(NSString *)stringToConvert
{
    if (!stringToConvert || [stringToConvert length] == 0) return nil;

    NSString *noHashString = [stringToConvert stringByReplacingOccurrencesOfString:@"#" withString:@""];
    NSScanner *stringScanner = [NSScanner scannerWithString:noHashString];

    unsigned hex;
    if (![stringScanner scanHexInt:&hex]) return nil;

    int r = (hex >> 16) & 0xFF;
    int g = (hex >> 8) & 0xFF;
    int b = (hex) & 0xFF;

    return [UIColor colorWithRed:r / 255.0f green:g / 255.0f blue:b / 255.0f alpha:1.0f];
}

Class<RCTComponentViewProtocol> AdvancedTextViewCls(void)
{
    return AdvancedTextView.class;
}

@end
