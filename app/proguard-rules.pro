# Pleos SDKs (Vehicle, SpeechToText, TextToSpeech): keep them as shipped. They bind car and voice
# services by name; we do not inspect them.
-keep class ai.pleos.** { *; }
-dontwarn ai.pleos.**
# android.car is provided by the head unit (useLibrary "android.car").
-dontwarn android.car.**

# The Pleos Vehicle SDK parses capability results with Gson TypeToken. R8 strips generic signatures by
# default and every capability call then fails ("TypeToken must be created with a type argument"), so
# keep signatures and Gson as shipped. (Found on the CarFlash release build, 2026-11-11.)
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-dontwarn com.google.gson.**

# Keep log tags readable in crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
