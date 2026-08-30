package es.bancamarch.colorblind3d.infrastructure.filamentcolors.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

/** DAO Room para leer/escribir el catálogo de filamentos cacheado en SQLite. */
@Dao
interface SwatchDao {

    @Query("SELECT * FROM swatches")
    suspend fun getAll(): List<SwatchEntity>

    @Query("SELECT DISTINCT manufacturerName FROM swatches ORDER BY manufacturerName")
    suspend fun getManufacturerNames(): List<String>

    @Query("SELECT COUNT(*) FROM swatches")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(swatches: List<SwatchEntity>)

    @Query("DELETE FROM swatches")
    suspend fun deleteAll()

    /** Sustituye el catálogo completo por [swatches] de forma atómica. */
    @Transaction
    suspend fun replaceAll(swatches: List<SwatchEntity>) {
        deleteAll()
        insertAll(swatches)
    }
}
