package es.bancamarch.colorblind3d.infrastructure.filamentcolors.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

/** DAO Room para persistir qué fabricantes ha deshabilitado el usuario en el selector de marcas. */
@Dao
interface DisabledManufacturerDao {

    @Query("SELECT manufacturerName FROM disabled_manufacturers")
    suspend fun getAll(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(manufacturers: List<DisabledManufacturerEntity>)

    @Query("DELETE FROM disabled_manufacturers")
    suspend fun deleteAll()

    /** Sustituye el conjunto completo de fabricantes deshabilitados por [disabled]. */
    @Transaction
    suspend fun replaceAll(disabled: Set<String>) {
        deleteAll()
        insertAll(disabled.map { DisabledManufacturerEntity(it) })
    }
}
