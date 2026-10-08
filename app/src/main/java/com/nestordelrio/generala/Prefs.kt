package com.nestordelrio.generala

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate

/** Guardado nativo: estado del juego (lo escribe el JS) + contadores de la app. */
class Prefs(context: Context) {
    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences("generala", Context.MODE_PRIVATE)

    // --- Estado que maneja el juego web (partida en curso, nombres, onboarding) ---
    fun getWeb(key: String): String? = sp.getString(WEB_PREFIX + key, null)

    fun setWeb(key: String, value: String) {
        sp.edit().apply {
            if (value.isEmpty()) remove(WEB_PREFIX + key) else putString(WEB_PREFIX + key, value)
        }.apply()
    }

    // --- Contadores y banderas nativas ---
    var sessions: Int
        get() = sp.getInt("sessions", 0)
        set(v) = sp.edit().putInt("sessions", v).apply()

    var gamesFinished: Int
        get() = sp.getInt("games_finished", 0)
        set(v) = sp.edit().putInt("games_finished", v).apply()

    var interstitialsShown: Int
        get() = sp.getInt("interstitials_shown", 0)
        set(v) = sp.edit().putInt("interstitials_shown", v).apply()

    var lastInterstitialAt: Long
        get() = sp.getLong("last_interstitial_at", 0L)
        set(v) = sp.edit().putLong("last_interstitial_at", v).apply()

    var lastInterstitialGame: Int
        get() = sp.getInt("last_interstitial_game", -1)
        set(v) = sp.edit().putInt("last_interstitial_game", v).apply()

    var adsRemoved: Boolean
        get() = sp.getBoolean("ads_removed", false)
        set(v) = sp.edit().putBoolean("ads_removed", v).apply()

    var reviewAsked: Boolean
        get() = sp.getBoolean("review_asked", false)
        set(v) = sp.edit().putBoolean("review_asked", v).apply()

    var reminderEnabled: Boolean
        get() = sp.getBoolean("reminder_enabled", false)
        set(v) = sp.edit().putBoolean("reminder_enabled", v).apply()

    /** Día (epoch day) en que se abrió la app por última vez; el recordatorio no molesta si ya jugó hoy. */
    var lastOpenDay: Long
        get() = sp.getLong("last_open_day", 0L)
        set(v) = sp.edit().putLong("last_open_day", v).apply()

    fun onSessionStart() {
        sessions = sessions + 1
        lastOpenDay = LocalDate.now().toEpochDay()
    }

    private companion object {
        const val WEB_PREFIX = "web_"
    }
}
