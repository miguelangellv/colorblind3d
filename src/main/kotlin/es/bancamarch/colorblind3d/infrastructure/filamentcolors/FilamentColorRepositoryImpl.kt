package es.bancamarch.colorblind3d.infrastructure.filamentcolors

import es.bancamarch.colorblind3d.domain.FilamentMatch
import es.bancamarch.colorblind3d.domain.FilamentSwatch
import es.bancamarch.colorblind3d.domain.LabColor
import es.bancamarch.colorblind3d.domain.RgbColor
import es.bancamarch.colorblind3d.domain.ports.FilamentColorRepository
import es.bancamarch.colorblind3d.infrastructure.filamentcolors.db.CacheMetadataEntity
import es.bancamarch.colorblind3d.infrastructure.filamentcolors.db.FilamentCacheDatabase
import es.bancamarch.colorblind3d.infrastructure.filamentcolors.db.defaultFilamentCacheDatabasePath
import es.bancamarch.colorblind3d.infrastructure.filamentcolors.db.openFilamentCacheDatabase
import es.bancamarch.colorblind3d.infrastructure.filamentcolors.db.toDomain
import es.bancamarch.colorblind3d.infrastructure.filamentcolors.db.toEntity
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/**
 * Implementación del catálogo de filamentos usando la API pública de
 * https://filamentcolors.xyz/api/swatch/ vía Ktor, con cache en una base de datos SQLite local
 * (gestionada con Room) para no tener que volver a descargar el catálogo completo en cada
 * ejecución ni en cada petición de "más alternativas".
 */
class FilamentColorRepositoryImpl(
    private val database: FilamentCacheDatabase = openFilamentCacheDatabase(defaultFilamentCacheDatabasePath()),
    private val cacheMaxAgeMillis: Long = DEFAULT_CACHE_MAX_AGE_MILLIS,
    private val httpClient: HttpClient = defaultHttpClient(),
) : FilamentColorRepository {

    private val mutex = Mutex()
    private var catalog: List<FilamentSwatch> = emptyList()

    override suspend fun ensureCatalogLoaded(forceRefresh: Boolean) {
        mutex.withLock {
            if (catalog.isNotEmpty() && !forceRefresh) return

            if (!forceRefresh) {
                loadFromDatabaseCache()?.let {
                    catalog = it
                    return
                }
            }

            val downloaded = downloadFullCatalog()
            catalog = downloaded
            saveToDatabaseCache(downloaded)
        }
    }

    override fun catalogSize(): Int = catalog.size

    override fun availableManufacturers(): List<String> =
        catalog.map { it.manufacturerName }.distinct().sorted()

    override fun bestMatches(
        target: LabColor,
        offset: Int,
        limit: Int,
        allowedManufacturers: Set<String>?,
    ): List<FilamentMatch> =
        catalog
            .asSequence()
            .filter { allowedManufacturers == null || it.manufacturerName in allowedManufacturers }
            .map { swatch -> FilamentMatch(swatch, target.deltaE2000(swatch.lab)) }
            .sortedBy { it.distance }
            .drop(offset)
            .take(limit)
            .toList()

    private suspend fun downloadFullCatalog(): List<FilamentSwatch> {
        val swatches = mutableListOf<SwatchDto>()
        var url: String? = "$API_BASE_URL/?format=json&page_size=$PAGE_SIZE"
        while (url != null) {
            val page: SwatchPageDto = httpClient.get(url).body()
            swatches += page.results
            url = page.next
        }
        return swatches.map { it.toDomain() }
    }

    private suspend fun loadFromDatabaseCache(): List<FilamentSwatch>? {
        val metadata = database.cacheMetadataDao().get() ?: return null
        val age = System.currentTimeMillis() - metadata.downloadedAtEpochMillis
        if (age > cacheMaxAgeMillis) return null
        val swatches = database.swatchDao().getAll()
        if (swatches.isEmpty()) return null
        return swatches.map { it.toDomain() }
    }

    private suspend fun saveToDatabaseCache(swatches: List<FilamentSwatch>) {
        try {
            database.swatchDao().replaceAll(swatches.map { it.toEntity() })
            database.cacheMetadataDao().upsert(
                CacheMetadataEntity(downloadedAtEpochMillis = System.currentTimeMillis()),
            )
        } catch (_: Exception) {
            // La cache es una optimización: si falla escribirla, seguimos con el catálogo en memoria.
        }
    }

    private fun SwatchDto.toDomain(): FilamentSwatch {
        val lab = if (labL != null && labA != null && labB != null) {
            LabColor(labL, labA, labB)
        } else {
            // Algunos swatches no traen Lab precalculado desde la API: lo derivamos del hex.
            RgbColor.fromHex(hexColor).toLab()
        }
        return FilamentSwatch(
            id = id,
            slug = slug,
            colorName = colorName,
            manufacturerName = manufacturer?.name ?: "Desconocido",
            filamentType = filamentType?.name ?: "",
            hex = hexColor,
            lab = lab,
            imageFrontUrl = imageFront,
            cardImageUrl = cardImg,
            purchaseUrl = amazonPurchaseLink ?: mfrPurchaseLink,
        )
    }

    companion object {
        private const val API_BASE_URL = "https://filamentcolors.xyz/api/swatch"
        private const val PAGE_SIZE = 250
        private const val DEFAULT_CACHE_MAX_AGE_MILLIS = 7L * 24 * 60 * 60 * 1000 // 7 días

        private fun defaultHttpClient(): HttpClient = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true })
            }
        }
    }
}
