# El puente JS <-> Android se invoca por reflexión desde el WebView.
-keepattributes JavascriptInterface
-keepclassmembers class com.nestordelrio.generala.NativeBridge {
    @android.webkit.JavascriptInterface <methods>;
}
