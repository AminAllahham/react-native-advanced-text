require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

# Resolve the react-native source dir so we can point at the Fabric text
# headers directly (they are not exposed by `install_modules_dependencies`).
react_common_dir =
  begin
    resolved = `cd "#{__dir__}" && node --print "require.resolve('react-native/package.json')" 2>/dev/null`.strip
    (resolved.empty? || !File.exist?(resolved)) ? "" : File.join(File.dirname(resolved), "ReactCommon")
  rescue StandardError
    ""
  end

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
  # (rather than replace it) so:
  #  1. our `common/cpp` headers win over the codegen output, and
  #  2. <react/renderer/textlayoutmanager/*.h> resolves (TextLayoutManager /
  #     TextLayoutContext), which `install_modules_dependencies` does NOT add.
  xcconfig = s.to_hash["pod_target_xcconfig"] || {}

  # `common/cpp` MUST stay first so our ShadowNodes.h shadows the codegen one.
  leading_paths = ["\"$(PODS_TARGET_SRCROOT)/common/cpp\""]

  # Extra paths for the Fabric text headers (append order is fine here).
  trailing_paths = [
    "\"$(PODS_ROOT)/Headers/Public/React-FabricComponents\"",
    "\"$(PODS_ROOT)/Headers/Private/React-FabricComponents\"",
  ]
  unless react_common_dir.empty?
    trailing_paths << "\"#{react_common_dir}\""
    trailing_paths << "\"#{File.join(react_common_dir, "react/renderer/textlayoutmanager/platform/ios")}\""
  end

  existing_header_paths = xcconfig["HEADER_SEARCH_PATHS"]
  xcconfig["HEADER_SEARCH_PATHS"] =
    (leading_paths + [existing_header_paths].compact + trailing_paths).join(" ")
  xcconfig["CLANG_CXX_LANGUAGE_STANDARD"] ||= "c++20"
  s.pod_target_xcconfig = xcconfig

  s.dependency "React-FabricComponents"
end
