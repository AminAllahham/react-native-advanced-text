# Custom CMake build for react-native-advanced-text.
#
# This replaces the auto-generated
#   build/generated/source/codegen/jni/CMakeLists.txt
# (referenced from react-native.config.js via `cmakeListsPath`) so that the
# Fabric component is backed by our self-measuring ShadowNode
# (common/cpp/react/renderer/components/AdvancedTextViewSpec/ShadowNodes.{h,cpp})
# instead of the plain, non-measuring one that codegen emits.
#
# It still produces the target name the app autolinking expects:
#   react_codegen_AdvancedTextViewSpec
#
# The key detail is include order: `common/cpp` is listed BEFORE the codegen
# output dir, so `#include <react/renderer/components/AdvancedTextViewSpec/ShadowNodes.h>`
# resolves to our version, while Props.h / EventEmitters.h / States.h still
# resolve to the generated ones.

cmake_minimum_required(VERSION 3.13)
set(CMAKE_VERBOSE_MAKEFILE ON)

set(CODEGEN_DIR "${CMAKE_CURRENT_SOURCE_DIR}/build/generated/source/codegen/jni")
set(COMMON_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../common/cpp")

file(GLOB codegen_SRCS CONFIGURE_DEPENDS
  "${CODEGEN_DIR}/*.cpp"
  "${CODEGEN_DIR}/react/renderer/components/AdvancedTextViewSpec/*.cpp")

file(GLOB custom_SRCS CONFIGURE_DEPENDS
  "${COMMON_DIR}/react/renderer/components/AdvancedTextViewSpec/*.cpp")

add_library(
  react_codegen_AdvancedTextViewSpec
  OBJECT
  ${codegen_SRCS}
  ${custom_SRCS}
)

# `common/cpp` MUST come first so our ShadowNodes.h shadows the generated one.
target_include_directories(react_codegen_AdvancedTextViewSpec PUBLIC
  "${COMMON_DIR}"
  "${CODEGEN_DIR}"
  "${CODEGEN_DIR}/react/renderer/components/AdvancedTextViewSpec")

target_link_libraries(
  react_codegen_AdvancedTextViewSpec
  fbjni
  jsi
  reactnative
)

target_compile_reactnative_options(react_codegen_AdvancedTextViewSpec PRIVATE)
