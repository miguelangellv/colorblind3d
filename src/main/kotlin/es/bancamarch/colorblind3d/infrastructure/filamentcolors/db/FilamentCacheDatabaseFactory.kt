package es.bancamarch.colorblind3d.infrastructure.filamentcolors.db

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import okio.Path.Companion.toPath
import okio.FileSystem

/**
 * Crea (o abre si ya existe) la base de datos Room de cache de filamentos en [databaseFilePath].
 * Usa el driver de SQLite embebido de androidx.sqlite (compilado desde fuente, sin depender del
 * SQLite del sistema operativo), por lo que funciona igual en Linux, Windows y macOS.
 */
fun openFilamentCacheDatabase(databaseFilePath: String): FilamentCacheDatabase {
    databaseFilePath.toPath().parent?.let { FileSystem.SYSTEM.createDirectories(it) }
    return Room.databaseBuilder<FilamentCacheDatabase>(name = databaseFilePath)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        // Es una base de datos de solo-cache (swatches descargables de nuevo, preferencias con
        // valor por defecto razonable), así que ante un cambio de esquema entre versiones basta
        // con recrearla en vez de escribir migraciones explícitas.
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
}

/** Ruta por defecto del fichero de base de datos, en el directorio de configuración del usuario. */
fun defaultFilamentCacheDatabasePath(): String =
    System.getProperty("user.home") + "/.colorblind-friend/swatches-cache.db"
