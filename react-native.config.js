/**
 * Autolinking config for react-native-advanced-text.
 *
 * On Android we ship a custom C++ ShadowNode (see android/CMakeLists.txt and
 * common/cpp/...) so the Fabric component measures its own height from its text
 * and the available width. Pointing `cmakeListsPath` at our own CMakeLists.txt
 * makes the app build that instead of the plain codegen output.
 *
 * `libraryName` (AdvancedTextViewSpec) and `componentDescriptors`
 * (AdvancedTextViewComponentDescriptor) are still auto-derived by the CLI from
 * package.json `codegenConfig` and the JS spec, so they are intentionally not
 * listed here.
 */
module.exports = {
  dependency: {
    platforms: {
      android: {
        cmakeListsPath: 'CMakeLists.txt',
      },
    },
  },
};
