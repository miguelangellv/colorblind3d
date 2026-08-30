import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.4.10"
    kotlin("plugin.serialization") version "2.4.10"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10"
    id("org.jetbrains.compose") version "1.12.0"
    id("com.google.devtools.ksp") version "2.3.11"
}

group = "es.bancamarch"
version = "0.2.1"

repositories {
    mavenCentral()
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

val ktorVersion = "3.5.2"
val xmlutilVersion = "1.0.2"
val coilVersion = "3.6.0"
val roomVersion = "2.8.4"

dependencies {
    // Compose Multiplatform for Desktop
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.components.resources)

    // Coroutines (integración con el event loop de Swing/AWT usado por Compose Desktop)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.11.0")

    // Okio: lectura de ficheros y del contenido ZIP del .3mf (en vez de java.util.zip)
    implementation("com.squareup.okio:okio:3.18.1")

    // xmlutil + kotlinx.serialization: parseo Kotlin-nativo del XML del 3MF (en vez de javax.xml)
    implementation("io.github.pdvrieze.xmlutil:core-jvm:$xmlutilVersion")
    implementation("io.github.pdvrieze.xmlutil:serialization-jvm:$xmlutilVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    // Ktor client: consultas a la API pública de filamentcolors.xyz
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    // Coil3: carga y cacheo de imágenes de los filamentos
    implementation("io.coil-kt.coil3:coil-compose:$coilVersion")
    implementation("io.coil-kt.coil3:coil-network-ktor3:$coilVersion")

    // Room: cache del catálogo de filamentos en una base de datos SQLite local (JVM desktop,
    // sin depender de Android) usando el driver de SQLite embebido de androidx.sqlite.
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.sqlite:sqlite-bundled:2.7.0")
    ksp("androidx.room:room-compiler:$roomVersion")

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
}

kotlin {
    jvmToolchain(17)
}

compose.desktop {
    application {
        mainClass = "es.bancamarch.colorblind3d.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Dmg, TargetFormat.Msi)
            packageName = "ColorBlind3D"
            packageVersion = "0.2.1"
        }
        buildTypes.release.proguard {
            configurationFiles.from(project.file("proguard-rules.pro"))
        }
    }
}

tasks.test {
    useJUnitPlatform()
}