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

AttributedString AdvancedTextViewShadowNode::getAttributedString() const {
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

  if (props.lineHeight > 0) {
    // The component treats `lineHeight` as a multiple of the font size
    // (see AdvancedTextView.mm / AdvancedTextView.kt). `TextAttributes`
    // expects an absolute value; approximate the platform default single
    // line height as 1.2 * fontSize.
    textAttributes.lineHeight =
        props.lineHeight * textAttributes.fontSize * 1.2;
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

  auto paragraphAttributes = ParagraphAttributes{};
  paragraphAttributes.maximumNumberOfLines = 0; // unlimited -> natural wrapping
  paragraphAttributes.adjustsFontSizeToFit = false;

  auto textLayoutContext = TextLayoutContext{};
  textLayoutContext.pointScaleFactor = layoutContext.pointScaleFactor;
  textLayoutContext.surfaceId = getSurfaceId();

  auto measurement = textLayoutManager_->measure(
      AttributedStringBox{getAttributedString()},
      paragraphAttributes,
      textLayoutContext,
      layoutConstraints);

  return layoutConstraints.clamp(measurement.size);
}

} // namespace facebook::react
