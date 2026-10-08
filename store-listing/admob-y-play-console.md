# Generala: AdMob y publicación en Play Store

Guía paso a paso para pasar de los anuncios de prueba a los reales y publicar la app. Todo lo que es texto para pegar está en `titulos-y-descripciones.md`, y las imágenes están en esta misma carpeta.

| Dato | Valor |
| --- | --- |
| Nombre de la app | Generala |
| Paquete | `com.nestordelrio.generala` |
| Versión | 1.0.0 (versionCode 1) |
| Categoría | Juegos → Juegos de mesa |
| Precio | Gratis, con anuncios y compra integrada |
| Producto de compra única | `remove_ads` ("Sin anuncios") |
| Política de privacidad | https://claude.ai/artifact/GsV9BoFUhKBacPvEnVQYV3 |
| Email de contacto | nestordelrio1989@gmail.com |

---

## Parte 1: AdMob (tu cuenta ya existe)

### 1. Crear la app en AdMob

AdMob → Apps → Agregar app → Android → "¿Está publicada en una tienda?": **No**. Nombre: `Generala`.

Copiá el **App ID** (formato `ca-app-pub-4930505659937183~XXXXXXXXXX`). Cuando la app esté publicada en Play, volvé a AdMob y vinculala a la ficha de la tienda (Configuración de la app → Tiendas de aplicaciones).

### 2. Crear las dos unidades de anuncio

| Formato | Nombre sugerido | Configuración |
| --- | --- | --- |
| Intersticial | `Generala - Intersticial entre partidas` | Tipos: imagen y video |
| Recompensado | `Generala - Recompensado tirada extra y deshacer` | Recompensa: 1 · "tirada" |

La frecuencia no se toca en AdMob porque ya está en el código: un intersticial cada 3 partidas, nunca en la primera sesión y como máximo uno cada 5 minutos.

### 3. Pegar los IDs en el proyecto

En `~/generala-android/gradle.properties`, sacá el `#` de estas líneas y completalas:

```
ADMOB_APP_ID=ca-app-pub-4930505659937183~XXXXXXXXXX
ADMOB_INTERSTITIAL_ID=ca-app-pub-4930505659937183/XXXXXXXXXX
ADMOB_REWARDED_ID=ca-app-pub-4930505659937183/XXXXXXXXXX
```

Los builds de debug siguen usando siempre los IDs de prueba. El bundle de release los toma de acá, y si falta alguno no compila, para que nunca se publique con anuncios de prueba.

### 4. Tu celular como dispositivo de prueba (antes de instalar un release)

Con IDs reales, ver o tocar anuncios desde tu propio celular cuenta como actividad inválida y puede suspender la cuenta de AdMob.

1. Instalá un build de **release** una vez y mirá el Logcat, filtrando por `Ads`. Aparece una línea con `setTestDeviceIds(Arrays.asList("33BE2250B4..."))`.
2. Pegá ese ID en `gradle.properties`:
   ```
   ADMOB_TEST_DEVICE_IDS=33BE2250B43518CCDA7DE426D04EE231
   ```
3. Recompilá. Tu celular va a ver anuncios con la etiqueta "Test Ad" y el resto de los jugadores, los reales. Podés dejarlo así también en el bundle que subís a Play.

### 5. Mensaje de consentimiento (Europa y Reino Unido)

AdMob → Privacidad y mensajes → RGPD → Crear mensaje → seleccioná la app Generala.

- URL de privacidad: la de la tabla de arriba.
- Idiomas: español, inglés, portugués, francés, alemán e italiano.
- Publicalo.

El código ya lo muestra solo donde corresponde, con el SDK de UMP. El link "Opciones de anuncios" del pie de pantalla aparece solo en esos países.

### 6. app-ads.txt (recomendado, no bloquea el lanzamiento)

Si ya lo publicaste para Cazapalabras, sirve el mismo archivo: es por cuenta de AdMob, no por app. Solo tenés que poner en la ficha de Generala el mismo sitio web que usaste en Cazapalabras. Si todavía no lo hiciste, los pasos están en `~/cazapalabras/store-listing/admob-integration-guide.md`, punto 5.

### 7. Revisión de la app en AdMob

Las apps nuevas pasan por una revisión ("app readiness") y hasta que termina sirven pocos anuncios. Se completa después de vincular la ficha de Play, así que hacé el paso 1 de vinculación apenas la app esté publicada.

---

## Parte 2: Play Console

### 1. Crear la app

Crear app → Nombre: `Generala: Juego de Dados` · Idioma predeterminado: **Español (Latinoamérica) – es-419** · App o juego: **Juego** · Gratis o de pago: **Gratis**. Aceptá las declaraciones.

### 2. Configuración de la tienda

- Categoría: **Juegos → Juegos de mesa**.
- Etiquetas: Juegos de mesa, Dados, Un jugador, Multijugador local, Sin conexión.
- Email: nestordelrio1989@gmail.com · Sitio web: el mismo de app-ads.txt, si lo tenés.

### 3. Ficha principal y traducciones

Presencia en la tienda → Ficha principal. Pegá la ficha es-419 de `titulos-y-descripciones.md` y subí:

| Recurso | Archivo |
| --- | --- |
| Ícono 512 × 512 | `ic_launcher_512.png` |
| Gráfico destacado 1024 × 500 | `banner/feature_graphic_es.png` |
| Capturas de teléfono (5) | `screenshots/es/es_1…es_5` en ese orden |

Después agregá la traducción **es-ES** con su texto, el mismo gráfico y las mismas capturas. Y en Fichas de la tienda personalizadas, creá una nueva para **México** con su ficha.

Las fichas en en, pt-BR, fr, de e it ya están escritas y los gráficos destacados están en `banner/`. Pero hoy el juego está solo en español: leé la advertencia al principio de `titulos-y-descripciones.md` antes de publicarlas.

### 4. Producto "Sin anuncios"

Monetizar → Productos → Productos únicos → Crear:

- ID: `remove_ads` (exacto: el código lo busca con ese nombre).
- Nombre: `Sin anuncios` · Descripción: `Quita los anuncios entre partidas para siempre.`
- Precio: USD 1,99 → "Actualizar precios locales" para que Play calcule el de cada país.
- Activalo.

Play solo deja crear productos después de subir un primer bundle con la biblioteca de facturación. Si todavía no aparece la opción, subí primero a prueba interna (paso 6) y volvé.

### 5. Contenido de la app (Política → Contenido de la app)

| Formulario | Respuesta |
| --- | --- |
| Política de privacidad | La URL de la tabla de arriba (compartila antes como "cualquiera con el link") |
| Anuncios | **Sí**, la app contiene anuncios |
| Acceso a la app | Todas las funciones están disponibles sin restricciones |
| Clasificación de contenido | Categoría "Juego". Violencia, sexo, lenguaje y drogas: No. **Apuestas simuladas: No** (se juega por puntos, sin fichas ni casino). Compras digitales: Sí. Resultado esperado: para todo público |
| Público objetivo | **13 años o más**. No marques menores de 13: eso activa la política de Familias y cambia las reglas de anuncios |
| ID de publicidad | **Sí** la usa → Publicidad o marketing |
| App de gobierno, salud, finanzas, noticias | No a todo |

**Seguridad de los datos:**

- ¿Recopila o comparte datos? **Sí**. ¿Están cifrados en tránsito? **Sí**. ¿Los usuarios pueden pedir que se borren? **No**, porque no hay cuentas; los datos del juego se borran al desinstalar.
- Declarar, todos como **recopilados y compartidos**, **obligatorios** y de **procesamiento no efímero**:

| Tipo de dato | Para qué |
| --- | --- |
| Ubicación → Ubicación aproximada | Publicidad o marketing |
| Actividad en la app → Interacciones con la app | Publicidad o marketing, Estadísticas |
| Información y rendimiento de la app → Diagnóstico | Estadísticas |
| Dispositivo u otros IDs | Publicidad o marketing, Prevención de fraudes y seguridad |

No declares datos financieros ni historial de compras. Google no lo exige cuando el pago lo procesa Google Play y la app no accede a los datos de pago. Tampoco declares los nombres de jugadores, porque nunca salen del teléfono.

Estas respuestas salen de la [declaración de datos del SDK de AdMob](https://developers.google.com/admob/android/privacy/play-data-disclosure) y de la [ayuda de Seguridad de los datos de Play](https://support.google.com/googleplay/android-developer/answer/10787469), consultadas el 24 de septiembre de 2026.

### 6. Subir y publicar

1. Generá el bundle firmado: Build → Generate Signed App Bundle → `release`, con la keystore de subida de siempre.
2. **Prueba interna:** subilo, agregá tu mail como tester y probá desde la Play Store. Con tu cuenta en Configuración → Pruebas de licencia, podés comprar "Sin anuncios" sin que te cobren.
3. **Producción:** Países → todos. Creá la versión con el mismo bundle y activá el despliegue gradual en **20%**.
4. Notas de la versión (es-419): `¡Primera versión de Generala! Jugá contra la máquina o pasando el teléfono.`
5. Enviá a revisión. Las apps nuevas suelen tardar de 1 a 7 días.
6. Con 2 o 3 días sin cuelgues en Android vitals, subí el despliegue al 100%.

### 7. Después de publicar

- Vincular la app en AdMob con la ficha de Play (Parte 1, paso 7).
- Pasame a las 2 semanas: de qué países vienen las instalaciones, retención al día 1 y al día 7, y las primeras reseñas. Con eso ajustamos ASO y anuncios.

---

## Checklist rápido

- [ ] App y 2 unidades creadas en AdMob
- [ ] IDs reales en `gradle.properties` (+ ID de tu celular como dispositivo de prueba)
- [ ] Mensaje de RGPD publicado en AdMob
- [ ] Política de privacidad compartida como pública
- [ ] App creada en Play Console, fichas es-419, es-ES y México cargadas
- [ ] Bundle en prueba interna, producto `remove_ads` creado y activo
- [ ] Contenido de la app completo (anuncios, clasificación, público, ID de publicidad, seguridad de los datos)
- [ ] Prueba en tu celular: partida completa, anuncios, compra de prueba, retomar partida
- [ ] Producción al 20% → 100%
- [ ] Vincular AdMob con la ficha publicada
