# NativeTextMeasurement is only called from C++ (ShadowNodes.cpp) via JNI,
# so R8 can't see it's used and would strip or rename it.
-keep class com.advancedtext.NativeTextMeasurement { *; }
