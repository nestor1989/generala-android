package com.nestordelrio.generala

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewFeature
import com.google.android.play.core.review.ReviewManagerFactory

class MainActivity : ComponentActivity() {

    lateinit var webView: WebView
        private set
    lateinit var prefs: Prefs
        private set
    lateinit var ads: AdsManager
        private set
    lateinit var billing: BillingManager
        private set

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            applyReminder(granted)
            if (!granted) {
                Toast.makeText(this, R.string.reminder_permission_denied, Toast.LENGTH_LONG).show()
            }
        }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        prefs = Prefs(this)
        prefs.onSessionStart()

        val root = FrameLayout(this).apply { setBackgroundColor(FELT) }
        webView = WebView(this).apply { setBackgroundColor(FELT) }
        root.addView(webView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        setContentView(root)

        // Edge-to-edge (obligatorio con targetSdk 35+): el juego queda dentro de las barras y el teclado.
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, ime.bottom))
            WindowInsetsCompat.CONSUMED
        }

        // El juego vive dentro del APK (assets/) y se sirve por https local: no depende de internet.
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()
        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                assetLoader.shouldInterceptRequest(request.url)

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (request.url.host == WebViewAssetLoader.DEFAULT_DOMAIN) return false
                openUrl(request.url.toString())
                return true
            }
        }
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            setSupportZoom(false)
            builtInZoomControls = false
            textZoom = 100
            mediaPlaybackRequiresUserGesture = true
        }
        // Modo claro fijo: el juego no se oscurece aunque el sistema esté en modo oscuro.
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, false)
        }
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        webView.overScrollMode = WebView.OVER_SCROLL_NEVER

        ads = AdsManager(this, prefs)
        billing = BillingManager(this, prefs)

        webView.addJavascriptInterface(NativeBridge(this), "GeneralaNative")
        webView.loadUrl("https://${WebViewAssetLoader.DEFAULT_DOMAIN}/assets/index.html")

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                webView.evaluateJavascript("(window.GeneralaApp && GeneralaApp.onBack()) ? '1' : '0'") { result ->
                    if (result?.contains("1") != true) {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                }
            }
        })

        ads.start()
        billing.start()
        if (prefs.reminderEnabled) Reminder.enable(this)
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        webView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        billing.end()
        webView.destroy()
        super.onDestroy()
    }

    /** Ejecuta JS en el juego desde cualquier hilo. */
    fun js(code: String) = runOnUiThread { webView.evaluateJavascript(code, null) }

    fun onGameFinished(won: Boolean) {
        prefs.gamesFinished = prefs.gamesFinished + 1
        // Pedido de reseña con la ventana oficial de Play: tras una victoria, desde la 3ª partida, una vez.
        if (won && prefs.gamesFinished >= 3 && !prefs.reviewAsked) {
            prefs.reviewAsked = true
            val manager = ReviewManagerFactory.create(this)
            manager.requestReviewFlow().addOnCompleteListener { task ->
                if (task.isSuccessful) manager.launchReviewFlow(this, task.result)
            }
        }
    }

    fun setReminder(enabled: Boolean) {
        if (enabled && !Reminder.canNotify(this) && Build.VERSION.SDK_INT >= 33) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        applyReminder(enabled)
    }

    private fun applyReminder(enabled: Boolean) {
        prefs.reminderEnabled = enabled
        if (enabled) {
            Reminder.ensureChannel(this)
            Reminder.enable(this)
        } else {
            Reminder.disable(this)
        }
        js("GeneralaApp.onReminderChanged($enabled)")
    }

    fun share(text: String) {
        val base = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        try {
            startActivity(Intent(base).setPackage("com.whatsapp"))
        } catch (_: ActivityNotFoundException) {
            startActivity(Intent.createChooser(base, getString(R.string.share_title)))
        }
    }

    fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, url, Toast.LENGTH_LONG).show()
        }
    }

    private companion object {
        val FELT = Color.parseColor("#0F3D2E")
    }
}
