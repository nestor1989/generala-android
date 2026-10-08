# Generala: proyecto Android

App nativa mínima: una Activity con WebView que carga el juego desde `assets/` (sin internet), más funciones nativas por un puente JS: guardado, vibración, AdMob (recompensado + intersticial + consentimiento UMP), "Sin anuncios" con Play Billing 8, pedido de reseña, compartir por WhatsApp y recordatorio diario opcional.

| Dato | Valor |
| --- | --- |
| Paquete | `com.nestordelrio.generala` (debug: `.debug`) |
| minSdk / targetSdk / compileSdk | 26 / 36 / 36 |
| AGP / Gradle / Kotlin | 8.9.1 / 8.11.1 / 2.2.20 (mismas versiones de AGP y Gradle que Cazapalabras) |
| Producto de compra única | `remove_ads` |
| Política de privacidad | https://claude.ai/artifact/GsV9BoFUhKBacPvEnVQYV3 (compartila como "cualquiera con el link") |

> No lo pude compilar en la nube porque la red bloquea los repositorios de Google y Maven. El Kotlin pasó una verificación de sintaxis y el juego se probó completo en navegador con el puente simulado. Si Android Studio sugiere actualizar alguna versión al sincronizar, aceptá la sugerencia.

## 1. Abrir y probar en debug

1. Android Studio → Open → esta carpeta (`generala-android`). Esperá el Gradle Sync.
2. Run en tu teléfono. El build de debug usa los **IDs de prueba de AdMob**: vas a ver anuncios con la etiqueta "Test Ad".
3. En Europa aparece el formulario de consentimiento; en Argentina normalmente no.

## 2. Poner los IDs reales de AdMob

En AdMob: Apps → Agregar app (Android, todavía no publicada) → crear dos bloques: **Intersticial** y **Recompensado**. Copiá los IDs en `gradle.properties`:

```
ADMOB_APP_ID=ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY
ADMOB_INTERSTITIAL_ID=ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ
ADMOB_REWARDED_ID=ca-app-pub-XXXXXXXXXXXXXXXX/WWWWWWWWWW
```

Si faltan, el build de release falla a propósito. En AdMob → Privacidad y mensajes, creá el mensaje de RGPD (lo muestra el SDK de UMP que ya está integrado).

## 3. Play Console

1. Crear la app "Generala" (juego, gratis).
2. Monetizar → Productos → Productos únicos → crear `remove_ads` ("Sin anuncios", USD 1,99 con precios locales) y activarlo.
3. Subir el bundle firmado: Build → Generate Signed App Bundle (o `./gradlew bundleRelease` con tu firma). Con Play App Signing, usá tu keystore de subida habitual.
4. Para probar la compra: agregá tu cuenta en Configuración → Prueba de licencias y subí a prueba interna.

### Formularios (respuestas sugeridas)

- **ID de publicidad:** sí, lo usa → para publicidad o marketing.
- **Seguridad de los datos:** ver `store-listing/admob-y-play-console.md` (Parte 2, paso 5), con la tabla exacta de qué declarar.
- **Público objetivo:** 13 años o más (evita la política de Familias).
- **Clasificación de contenido:** juego de dados de puntaje; "no" a apuestas simuladas.
- **Anuncios:** sí, la app contiene anuncios.

## 4. Checklist de prueba en el teléfono

- [ ] Partida completa contra la máquina y otra pasando el teléfono.
- [ ] Salir a mitad de partida (atrás o cerrar la app) y ver "Continuar partida".
- [ ] "Tirada extra" y "Deshacer" (este aparece al tachar una casilla) muestran un anuncio recompensado y dan el premio.
- [ ] El intersticial aparece recién desde la segunda vez que abrís la app, cada 3 partidas, al tocar "Jugar de nuevo".
- [ ] Compra de prueba de "Sin anuncios": desaparecen los intersticiales y sigue así al reinstalar.
- [ ] Vibración al tocar y tirar dados, al anotar y al ganar.
- [ ] Recordatorio: activarlo pide permiso de notificaciones (Android 13+).
- [ ] "Invitar a jugar por WhatsApp" abre WhatsApp con el resultado.
- [ ] Con el modo oscuro del sistema activado, el juego se sigue viendo claro (modo claro fijo).

## Cómo está armado

- `web/game.html`: el código del juego (el mismo del prototipo).
- `app/src/main/assets/index.html`: el juego dentro de la app. **No se edita a mano**: se genera con `python3 tools/build_web.py` desde `web/game.html` (el mismo archivo del prototipo), con las fuentes Karla y Fraunces incluidas (licencia OFL).
- `NativeBridge.kt`: lo que el juego puede pedir (`window.GeneralaNative`).
- `AdsManager.kt`: consentimiento UMP, reglas del intersticial (cada 3 partidas, nunca en la primera sesión, 1 cada 5 min) y recompensados.
- `BillingManager.kt`: "Sin anuncios", restauración y reembolsos.
- `Reminder.kt`: recordatorio a las 19 h con WorkManager, solo si ese día no abrió la app.
