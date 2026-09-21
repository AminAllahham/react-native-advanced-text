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
#include <cmath>
#include <string>

#include <react/renderer/attributedstring/AttributedStringBox.h>
#include <react/renderer/attributedstring/ParagraphAttributes.h>
#include <react/renderer/attributedstring/TextAttributes.h>
#include <react/renderer/attributedstring/primitives.h>
#include <react/renderer/mounting/ShadowView.h>
#include <react/renderer/textlayoutmanager/TextLayoutContext.h>
#include <react/renderer/textlayoutmanager/TextLayoutManager.h>

#if defined(ANDROID)
#include <fbjni/fbjni.h>
#endif

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

  // Deliberately never sets `textAttributes.lineHeight`: the component's
  // `lineHeight` prop is a *multiple* of the font's natural line height (see
  // `setLineSpacing(0, multiplier)` in AdvancedTextView.kt / the
  // NSParagraphStyle-based spacing in AdvancedTextView.mm on iOS), not the
  // absolute per-line value `TextAttributes::lineHeight` expects. Forcing an
  // absolute value here would make Fabric apply RN's own CSS-style
  // forced-line-height algorithm, which is a *different* algorithm from
  // `setLineSpacing`'s multiplicative one and drifts from it by an amount
  // that compounds with every wrapped line. Measuring naturally instead and
  // scaling the *result* in `measureContent()` uses the same "multiply the
  // natural total" math `setLineSpacing` itself uses, so they agree exactly
  // regardless of paragraph length.

  auto attributedString = AttributedString{};
  attributedString.appendFragment(AttributedString::Fragment{
      .string = props.text,
      .textAttributes = textAttributes,
      .parentShadowView = ShadowView(*this),
  });
  return attributedString;
}

#if defined(ANDROID)
Float AdvancedTextViewShadowNode::measureHeightViaAndroidNative(
    Float widthDp,
    Float pointScaleFactor) const {
  if (!std::isfinite(widthDp) || widthDp <= 0 || pointScaleFactor <= 0) {
    return -1;
  }

  const auto& props = getConcreteProps();

  const auto weight = toLowerAscii(props.fontWeight);
  // Matches Typeface.NORMAL/BOLD/ITALIC -- the same three-way mapping
  // AdvancedTextView.kt uses for `typeface = when (fontWeight) { ... }`.
  int typefaceStyle = 0; // Typeface.NORMAL
  if (weight == "bold") {
    typefaceStyle = 1; // Typeface.BOLD
  } else if (weight == "italic") {
    typefaceStyle = 2; // Typeface.ITALIC
  }

  std::string fontFamily =
      props.fontFamily.empty() ? "sans-serif" : props.fontFamily;
  Float fontSize = props.fontSize > 0 ? props.fontSize : 16.0;
  Float lineHeightMultiplier = props.lineHeight > 0 ? props.lineHeight : 1.0;
  Float widthPx = widthDp * pointScaleFactor;

  static const auto cls =
      facebook::jni::findClassStatic("com/advancedtext/NativeTextMeasurement");
  static const auto method = cls->getStaticMethod<jfloat(
      std::string,
      std::string,
      jint,
      jfloat,
      jfloat,
      jfloat,
      jfloat)>("measureHeight");

  Float heightPx = method(
      cls,
      props.text,
      fontFamily,
      typefaceStyle,
      static_cast<jfloat>(fontSize),
      static_cast<jfloat>(lineHeightMultiplier),
      static_cast<jfloat>(props.letterSpacing),
      static_cast<jfloat>(widthPx));

  if (heightPx <= 0) {
    return -1;
  }

  return heightPx / pointScaleFactor;
}
#endif

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
      AttributedStringBox{getAttributedString()},
      paragraphAttributes,
      textLayoutContext,
      layoutConstraints);

  auto size = measurement.size;

#if defined(ANDROID)
  // Prefer asking Android's own `StaticLayout` for the height directly (see
  // `measureHeightViaAndroidNative()`) -- it's the same class
  // `AdvancedTextView`'s real rendering is built on, so there's no
  // measurement-vs-render drift to compound over long paragraphs. `size`
  // (from RN's own TextLayoutManager, above) still supplies the width --
  // this only replaces the height. Falls back to the `lineHeight`-scaling
  // math below when the width isn't a finite, positive number (e.g. an
  // unconstrained/intrinsic-width measure pass).
  auto nativeHeight =
      measureHeightViaAndroidNative(size.width, textLayoutContext.pointScaleFactor);
  if (nativeHeight > 0) {
    size.height = nativeHeight;
    return layoutConstraints.clamp(size);
  }
#endif

  // `lineHeight` scales the font's own natural line height (see
  // `getAttributedString()`); since `size.height` above is the *natural*
  // (multiplier == 1) total, `setLineSpacing(0, multiplier)` scaling the
  // same natural total on the view side means multiplying it here
  // reproduces exactly the same number, for any paragraph length.
  if (props.lineHeight > 0) {
    size.height *= props.lineHeight;
  }

  return layoutConstraints.clamp(size);
}

} // namespace facebook::react
