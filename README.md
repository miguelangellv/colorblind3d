# Colorblind 3D

Aplicación de escritorio (Compose Multiplatform / JVM) pensada para ayudar a personas
daltónicas a saber **en texto** qué colores usa un modelo `.3mf` exportado desde Bambu Studio,
y qué filamentos reales se parecen más a esos colores, usando el catálogo público de
[filamentcolors.xyz](https://filamentcolors.xyz/).

## Cómo usarla

1. Ejecuta la aplicación:
   ```bash
   ./gradlew run
   ```
2. Pulsa **"Abrir fichero 3MF"** y selecciona un `.3mf` de Bambu Studio.
3. La aplicación mostrará, para cada color/filamento usado en el modelo:
   - Su nombre y código hexadecimal en texto.
   - Los 5 filamentos reales cuya imagen y ficha (nombre, fabricante) más se parecen a ese color.
4. Pulsa **"Más alternativas"** en cualquier color para ver los siguientes 5 candidatos más
   cercanos (usando el catálogo ya descargado, sin más peticiones de red).

La primera vez que se ejecuta, la app descarga y cachea en una base de datos **Room/SQLite**
(`~/.colorblind-friend/swatches-cache.db`) el catálogo completo de swatches de filamentcolors.xyz
(unos 2200+ filamentos). La caché se refresca automáticamente cada 7 días.

Con el icono de filtro (🔽) de la barra superior puedes abrir el selector de **marcas de
filamento**: por defecto están todas habilitadas, tiene un buscador rápido para localizar una
marca por nombre, y puedes desmarcar las que no te interesen para que solo se tengan en cuenta
esas marcas al buscar coincidencias de color. La selección se guarda en la misma base de datos
Room/SQLite y se recuerda la próxima vez que abras la aplicación.

Los filamentos sugeridos para cada color se muestran en un **grid adaptable** (no en una única
fila con scroll): las tarjetas van ocupando el ancho disponible y saltan a una nueva fila
automáticamente según el tamaño de la ventana.

## Arquitectura (DDD por capas)

```
es.bancamarch.colorblind3d
├── domain            -> Entidades y value objects puros (RgbColor, LabColor, ColorSample,
│   └── ports            FilamentSwatch/FilamentMatch, ThreeMfDocument) e interfaces de puertos
│                        (ThreeMfReader, FilamentColorRepository,
│                        ManufacturerPreferencesRepository), sin dependencias externas.
├── application       -> Casos de uso: LoadThreeMfFileUseCase, MatchFilamentsForColorUseCase,
│                        RequestMoreAlternativesUseCase, PreloadFilamentCatalogUseCase,
│                        GetAvailableManufacturersUseCase, LoadManufacturerFilterUseCase,
│                        SaveManufacturerFilterUseCase.
├── infrastructure     -> Adaptadores de los puertos:
│   ├── threemf           - SimpleZipReader (lector ZIP propio en Kotlin puro sobre Okio) +
│   │                       KtXmlReader (xmlutil) + kotlinx.serialization para leer los ficheros
│   │                       de Bambu Studio (project_settings.config / model_settings.config).
│   └── filamentcolors    - Cliente Ktor + caché en Room/SQLite del catálogo de
│                          filamentcolors.xyz, con comparación de color en espacio Lab (CIEDE2000)
│                          y filtro opcional por marca/fabricante. También persiste ahí qué
│                          marcas ha deshabilitado el usuario (ManufacturerPreferencesRepositoryImpl).
│       └── db               Entidades Room (SwatchEntity, CacheMetadataEntity), DAOs y la clase
│                            FilamentCacheDatabase (androidx.room + androidx.sqlite-bundled).
└── ui                -> Compose: screens (MainScreen), components (ColorSwatchChip,
                          FilamentMatchCard, FilamentMatchList, ModelColorSection,
                          ManufacturerSelectorDialog) y state (MainViewModel + MainUiState con
                          StateFlow).
```

## Decisiones técnicas relevantes

- **Sin `java.util.zip`/`javax.xml.parsers` como librería de carga de ficheros**: el `.3mf` es
  un ZIP que se lee con un parser de cabeceras ZIP escrito a mano (`SimpleZipReader`) sobre la
  API pública de ficheros de **Okio**, y el XML interno se parsea con **xmlutil**
  (`KtXmlReader`), ambas Kotlin-first. El único punto donde se usa una clase de la librería
  estándar de Java es `java.util.zip.Inflater`, exclusivamente como códec de descompresión
  DEFLATE en bruto (no hay alternativa Kotlin-pura madura en Maven Central a día de hoy); no se
  usa como API de "carga de ficheros".
- **Imágenes con Coil 3** (`coil-compose` + `coil-network-ktor3`) para las fotos de los
  filamentos devueltos por filamentcolors.xyz.
- **Coincidencia de color** calculada en el cliente con distancia CIEDE2000 en espacio Lab
  (no existe un endpoint público de "color match" en la API).
- **Caché con Room sobre SQLite** (`androidx.room` 2.8.4 + `androidx.sqlite:sqlite-bundled`,
  generado con **KSP**): aunque `androidx.room` está pensado sobre todo para Android, publica un
  variant Gradle de tipo `jvm` que funciona directamente en un proyecto `kotlin("jvm")` de
  escritorio sin necesidad del plugin de Kotlin Multiplatform.

## Distribución (Linux, Windows y Arch Linux)

El empaquetado nativo (instalador `.deb` en Linux, `.msi` en Windows) se genera con el plugin de
Compose Desktop, que por debajo usa `jpackage`. **`jpackage` no permite cross-compilar**: para
producir un `.msi` de Windows hace falta ejecutarlo en un Windows real (con el toolset WiX
instalado), así que no es viable hacerlo desde Linux ni siquiera con Docker. Por eso la
distribución para ambos sistemas se genera con **GitHub Actions**
(`.github/workflows/build-distributables.yml`), usando una matriz de runners
`ubuntu-latest`/`windows-latest`:

```bash
# Generar el paquete Linux (.deb) localmente:
./gradlew packageReleaseDeb

# Generar el ejecutable "distributable" sin empaquetar (para probarlo directamente):
./gradlew createReleaseDistributable
./build/compose/binaries/main-release/app/ColorBlind3D/bin/ColorBlind3D
```

`jpackage` tampoco soporta el formato de paquete de **Arch Linux** (`.pkg.tar.zst`), así que el
workflow incluye un job adicional `package-archlinux` que corre en un contenedor
`archlinux:base-devel`: genera el mismo directorio "distributable" con
`createReleaseDistributable` y lo empaqueta con `makepkg` usando un `PKGBUILD` generado
dinámicamente (instala la app en `/opt/colorblind3d`, crea un symlink en `/usr/bin/colorblind3d`
y una entrada `.desktop`). Se ha verificado localmente con Docker (`archlinux:base-devel` +
`makepkg` real contra un directorio de app simulado) que el `PKGBUILD` generado es válido y
produce un `.pkg.tar.zst` instalable con `pacman -U`.

La build de **release** habilita ProGuard (vía `proguard-rules.pro`) para minificar/optimizar el
jar empaquetado. Esto se ha verificado localmente ejecutando el binario Linux real (no solo
`./gradlew run`): sin las reglas de `-keep` adecuadas, ProGuard rompe en tiempo de ejecución la
localización por reflexión de la implementación de Room (`FilamentCacheDatabase_Impl`), el
`ServiceLoader` de Ktor (motor CIO y soporte de `kotlinx.serialization`) y la firma de los
métodos JNI nativos de `androidx.sqlite.driver.bundled`. `proguard-rules.pro` mantiene estas
clases/miembros explícitamente; tras añadirlas, el binario release arranca, descarga y persiste
el catálogo completo de filamentcolors.xyz en la base de datos Room/SQLite sin errores.

## Tests

```bash
./gradlew test
```

Incluye tests de humo/integración reales (parseo de ficheros `.3mf` de ejemplo y llamadas a la
API real de filamentcolors.xyz), que se omiten automáticamente si el fichero de ejemplo o el
acceso a red no están disponibles en el entorno.
