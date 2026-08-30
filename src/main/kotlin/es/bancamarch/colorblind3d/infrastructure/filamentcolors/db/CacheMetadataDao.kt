package es.bancamarch.colorblind3d.infrastructure.filamentcolors.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/** DAO Room para leer/escribir la marca de tiempo de la última descarga del catálogo. */
@Dao
interface CacheMetadataDao {

    @Query("SELECT * FROM cache_metadata WHERE id = ${CacheMetadataEntity.SINGLETON_ID}")
    suspend fun get(): CacheMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(metadata: CacheMetadataEntity)
}
