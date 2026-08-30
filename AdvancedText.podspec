require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "AdvancedText"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = package["homepage"]
  s.license      = package["license"]
  s.authors      = package["author"]

  s.platforms    = { :ios => min_ios_version_supported }
  s.source       = { :git => "https://github.com/AminAllahham/react-native-advanced-text.git", :tag => "#{s.version}" }

  # `common/cpp` holds the self-measuring Fabric ShadowNode. Its
  # ShadowNodes.h intentionally shadows the codegen output, so `common/cpp`
  # must come first on the header search path (see below).
  s.source_files = "ios/**/*.{h,m,mm,swift,cpp}", "common/cpp/**/*.{h,cpp}"
  s.private_header_files = "ios/**/*.h"

  install_modules_dependencies(s)

  # `install_modules_dependencies` sets `s.pod_target_xcconfig`; extend it
  # (rather than replace it) so our headers win over the generated ones and
  # so `React-FabricComponents` headers (TextLayoutManager) resolve.
  xcconfig = s.attributes_hash["pod_target_xcconfig"] || {}
  existing_header_paths = xcconfig["HEADER_SEARCH_PATHS"] || "$(inherited)"
  xcconfig["HEADER_SEARCH_PATHS"] =
    "\"$(PODS_TARGET_SRCROOT)/common/cpp\" #{existing_header_paths}"
  xcconfig["CLANG_CXX_LANGUAGE_STANDARD"] ||= "c++20"
  s.pod_target_xcconfig = xcconfig

  # Needed for <react/renderer/textlayoutmanager/TextLayoutManager.h>, which is
  # not pulled in by `install_modules_dependencies`.
  s.dependency "React-FabricComponents"
end
