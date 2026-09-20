/*
 * Implementation of the self-measuring <AdvancedTextView> ShadowNode.
 * See ShadowNodes.h for why this file shadows the codegen output.
 *
 * NOTE: this file deliberately does NOT define `AdvancedTextViewComponentName`.
 * That symbol stays owned by the codegen-generated `ShadowNodes.cpp`, which is
 * still compiled (via the podspec on iOS and via CMakeLists.txt on Android).
 */

#include <react/renderer/components/AdvancedTextViewSpec/ShadowNodes.h>

#include <algorithm>
#include <cctype>
#include <string>

#include <react/renderer/attributedstring/AttributedStringBox.h>
#include <react/renderer/attributedstring/ParagraphAttributes.h>
#include <react/renderer/attributedstring/TextAttributes.h>
#include <react/renderer/attributedstring/primitives.h>
#include <react/renderer/mounting/ShadowView.h>
#include <react/renderer/textlayoutmanager/TextLayoutContext.h>
#include <react/renderer/textlayoutmanager/TextLayoutManager.h>

namespace facebook::react {

namespace {

std::string toLowerAscii(std::string value) {
  std::transform(value.begin(), value.end(), value.begin(), [](unsigned char c) {
    return static_cast<char>(std::tolower(c));
  });
  return value;
}

} // namespace

AdvancedTextViewShadowNode::AdvancedTextViewShadowNode(
    const ShadowNode& sourceShadowNode,
    const ShadowNodeFragment& fragment)
    : ConcreteViewShadowNode(sourceShadowNode, fragment),
      textLayoutManager_(
          static_cast<const AdvancedTextViewShadowNode&>(sourceShadowNode)
              .textLayoutManager_) {}

TextAttributes AdvancedTextViewShadowNode::baseTextAttributes() const {
  const auto& props = getConcreteProps();

  auto textAttributes = TextAttributes::defaultTextAttributes();

  // Font size: matches the 16pt fallback used by the native views.
  textAttributes.fontSize =
      props.fontSize > 0 ? static_cast<Float>(props.fontSize) : 16.0;

  if (!props.fontFamily.empty()) {
    textAttributes.fontFamily = props.fontFamily;
  }

  // The native views treat `fontWeight` as either a weight ("bold") or a
  // style ("italic"); mirror that here so wrapping metrics line up.
  const auto weight = toLowerAscii(props.fontWeight);
  if (weight == "bold") {
    textAttributes.fontWeight = FontWeight::Bold;
  } else if (weight == "italic") {
    textAttributes.fontStyle = FontStyle::Italic;
  }

  if (props.letterSpacing != 0) {
    textAttributes.letterSpacing = props.letterSpacing;
  }

  return textAttributes;
}

#if defined(ANDROID)
Float AdvancedTextViewShadowNode::measureNaturalLineHeight(
    const TextAttributes& baseTextAttributes,
    const TextLayoutContext& textLayoutContext) const {
  // `baseTextAttributes` deliberately has no `lineHeight` set, so this
  // resolves to the font's own natural single-line height -- exactly what
  // CssLineHeightSpan.kt multiplies on the view side.
  auto probeString = AttributedString{};
  probeString.appendFragment(AttributedString::Fragment{
      .string = "M",
      .textAttributes = baseTextAttributes,
      .parentShadowView = ShadowView(*this),
  });

  auto probeParagraphAttributes = ParagraphAttributes{};
  probeParagraphAttributes.maximumNumberOfLines = 1;
  // Measure the tight ascent+descent box (no extra top/bottom accent
  // padding). This is the same "natural line height" basis
  // CssLineHeightSpan.kt derives from `Paint.getFontMetricsInt()` on the
  // view side (which is unaffected by `includeFontPadding`, a Layout-level
  // setting) -- measuring with padding included here would double count it
  // once per rendered line once the resulting value is multiplied and
  // applied to the whole paragraph.
  probeParagraphAttributes.includeFontPadding = false;

  auto measurement = textLayoutManager_->measure(
      AttributedStringBox{probeString},
      probeParagraphAttributes,
      textLayoutContext,
      LayoutConstraints{});

  return measurement.size.height;
}
#endif

AttributedString AdvancedTextViewShadowNode::getAttributedString(
    const TextAttributes& baseTextAttributes,
    const TextLayoutContext& textLayoutContext) const {
  const auto& props = getConcreteProps();

  auto textAttributes = baseTextAttributes;

  // The component treats `lineHeight` as a multiple of the font's own
  // natural line height (see CssLineHeightSpan.kt on Android / the
  // NSParagraphStyle-based spacing in AdvancedTextView.mm on iOS) -- NOT the
  // absolute per-line value `TextAttributes::lineHeight` expects. A
  // multiplier of 1 *is* the natural line height, so there is nothing to
  // override in that case (overriding it with an approximation, as this
  // used to do unconditionally, made even the "no lineHeight override" case
  // measure taller or shorter than the real render).
  if (props.lineHeight > 0 && props.lineHeight != 1.0) {
#if defined(ANDROID)
    // Android's natural single-line height depends on the resolved
    // font/size/weight and isn't a fixed ratio of fontSize, so a constant
    // approximation drifts from the real value CssLineHeightSpan.kt derives
    // from Paint.getFontMetricsInt() -- by an amount that compounds with
    // every wrapped line, which is why the error scaled with paragraph
    // length. Measure the real natural height for these exact attributes
    // instead, the same way the view derives it.
    auto naturalLineHeight =
        measureNaturalLineHeight(textAttributes, textLayoutContext);
    textAttributes.lineHeight = naturalLineHeight > 0
        ? props.lineHeight * naturalLineHeight
        : props.lineHeight * textAttributes.fontSize * 1.2;
#else
    textAttributes.lineHeight =
        props.lineHeight * textAttributes.fontSize * 1.2;
#endif
  }

  auto attributedString = AttributedString{};
  attributedString.appendFragment(AttributedString::Fragment{
      .string = props.text,
      .textAttributes = textAttributes,
      .parentShadowView = ShadowView(*this),
  });
  return attributedString;
}

Size AdvancedTextViewShadowNode::measureContent(
    const LayoutContext& layoutContext,
    const LayoutConstraints& layoutConstraints) const {
  const auto& props = getConcreteProps();

  if (props.text.empty()) {
    return layoutConstraints.clamp({0, 0});
  }

  if (!textLayoutManager_) {
    textLayoutManager_ =
        std::make_shared<const TextLayoutManager>(getContextContainer());
  }

  auto textLayoutContext = TextLayoutContext{};
  textLayoutContext.pointScaleFactor = layoutContext.pointScaleFactor;
  textLayoutContext.surfaceId = getSurfaceId();

  auto paragraphAttributes = ParagraphAttributes{};
  paragraphAttributes.maximumNumberOfLines = 0; // unlimited -> natural wrapping
  paragraphAttributes.adjustsFontSizeToFit = false;

  auto measurement = textLayoutManager_->measure(
      AttributedStringBox{
          getAttributedString(baseTextAttributes(), textLayoutContext)},
      paragraphAttributes,
      textLayoutContext,
      layoutConstraints);

  return layoutConstraints.clamp(measurement.size);
}

} // namespace facebook::react
