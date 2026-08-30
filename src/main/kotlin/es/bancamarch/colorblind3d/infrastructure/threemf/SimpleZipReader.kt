package es.bancamarch.colorblind3d.infrastructure.threemf

import okio.Buffer
import okio.FileHandle
import java.util.zip.Inflater

private const val EOCD_SIGNATURE = 0x06054b50
private const val CENTRAL_DIR_SIGNATURE = 0x02014b50
private const val LOCAL_FILE_SIGNATURE = 0x04034b50
private const val METHOD_STORED = 0
private const val METHOD_DEFLATED = 8
private const val EOCD_FIXED_SIZE = 22L
private const val MAX_COMMENT_SIZE = 65535L

private data class ZipEntryRecord(
    val name: String,
    val compressionMethod: Int,
    val compressedSize: Long,
    val uncompressedSize: Long,
    val localHeaderOffset: Long,
)

/**
 * Lector de solo-lectura de ficheros ZIP (usado para abrir el contenedor de un `.3mf`),
 * implementado sobre el acceso posicional a fichero de Okio ([okio.FileHandle]) en vez de las
 * clases de alto nivel `java.util.zip.ZipFile`/`ZipInputStream` que se usan habitualmente en Java
 * para *cargar* un ZIP. El único punto de contacto con `java.util.zip` es [Inflater], el códec
 * de descompresión DEFLATE en bruto: no existe una implementación DEFLATE Kotlin-nativa estable
 * publicada, y aquí se usa únicamente como algoritmo de descompresión de bytes ya leídos, no como
 * mecanismo de carga de ficheros.
 */
internal class SimpleZipReader(private val handle: FileHandle) : AutoCloseable {

    private val entries: Map<String, ZipEntryRecord> by lazy { readCentralDirectory() }

    fun exists(name: String): Boolean = entries.containsKey(name)

    /** Nombres de todas las entradas del ZIP, para poder localizar ficheros por patrón (p.ej. `*.model`). */
    fun entryNames(): Set<String> = entries.keys

    fun readText(name: String): String = readBytes(name).toString(Charsets.UTF_8)

    fun readBytes(name: String): ByteArray {
        val entry = entries[name]
            ?: throw NoSuchElementException("Entrada no encontrada en el fichero 3MF: $name")
        return readEntryBytes(entry)
    }

    override fun close() {
        handle.close()
    }

    private fun readEntryBytes(entry: ZipEntryRecord): ByteArray {
        val header = Buffer()
        handle.read(entry.localHeaderOffset, header, 30)
        check(header.readIntLe() == LOCAL_FILE_SIGNATURE) {
            "Cabecera local de ZIP inválida para ${entry.name}"
        }
        header.skip(22) // version, flags, compresión, fecha/hora, crc32, tamaños (ya conocidos)
        val nameLength = header.readShortLe().toLong()
        val extraLength = header.readShortLe().toLong()
        val dataOffset = entry.localHeaderOffset + 30 + nameLength + extraLength

        val compressed = Buffer()
        handle.read(dataOffset, compressed, entry.compressedSize)
        val compressedBytes = compressed.readByteArray()

        return when (entry.compressionMethod) {
            METHOD_STORED -> compressedBytes
            METHOD_DEFLATED -> inflate(compressedBytes, entry.uncompressedSize)
            else -> throw UnsupportedOperationException(
                "Método de compresión ZIP no soportado (${entry.compressionMethod}) para ${entry.name}"
            )
        }
    }

    private fun inflate(compressed: ByteArray, uncompressedSize: Long): ByteArray {
        val inflater = Inflater(true) // true = raw deflate, sin cabecera zlib
        try {
            inflater.setInput(compressed)
            val output = ByteArray(uncompressedSize.toInt())
            var offset = 0
            while (offset < output.size && !inflater.finished()) {
                val read = inflater.inflate(output, offset, output.size - offset)
                if (read == 0 && inflater.needsInput()) break
                offset += read
            }
            return output
        } finally {
            inflater.end()
        }
    }

    private fun readCentralDirectory(): Map<String, ZipEntryRecord> {
        val fileSize = handle.size()
        val scanSize = minOf(fileSize, EOCD_FIXED_SIZE + MAX_COMMENT_SIZE)
        val tail = Buffer()
        handle.read(fileSize - scanSize, tail, scanSize)
        val tailBytes = tail.readByteArray()

        val eocdIndex = findEocdSignature(tailBytes)
            ?: throw IllegalStateException("No se ha encontrado el fin de directorio central del ZIP")

        val eocd = Buffer().apply { write(tailBytes, eocdIndex, tailBytes.size - eocdIndex) }
        eocd.skip(4) // firma, ya localizada
        eocd.skip(2) // número de disco
        eocd.skip(2) // disco donde empieza el directorio central
        eocd.skip(2) // entradas en este disco
        val totalEntries = eocd.readShortLe().toInt() and 0xFFFF
        eocd.skip(4) // tamaño del directorio central
        val centralDirOffset = eocd.readIntLe().toLong() and 0xFFFFFFFFL

        val centralDirSize = (fileSize - scanSize + eocdIndex) - centralDirOffset
        val centralDir = Buffer()
        handle.read(centralDirOffset, centralDir, centralDirSize)

        val result = LinkedHashMap<String, ZipEntryRecord>(totalEntries)
        repeat(totalEntries) { i ->
            val sig = centralDir.readIntLe()
            check(sig == CENTRAL_DIR_SIGNATURE) {
                "Registro de directorio central de ZIP inválido (entrada $i, sig=${sig.toString(16)})"
            }
            centralDir.skip(4) // versión creador / versión necesaria
            centralDir.skip(2) // flags
            val compressionMethod = centralDir.readShortLe().toInt() and 0xFFFF
            centralDir.skip(8) // fecha/hora modificación (4), crc32 (4)
            val compressedSize = centralDir.readIntLe().toLong() and 0xFFFFFFFFL
            val uncompressedSize = centralDir.readIntLe().toLong() and 0xFFFFFFFFL
            val nameLength = centralDir.readShortLe().toInt() and 0xFFFF
            val extraLength = centralDir.readShortLe().toInt() and 0xFFFF
            val commentLength = centralDir.readShortLe().toInt() and 0xFFFF
            centralDir.skip(2) // número de disco
            centralDir.skip(2) // atributos internos
            centralDir.skip(4) // atributos externos
            val localHeaderOffset = centralDir.readIntLe().toLong() and 0xFFFFFFFFL
            val name = centralDir.readUtf8(nameLength.toLong())
            centralDir.skip(extraLength.toLong())
            centralDir.skip(commentLength.toLong())

            result[name] = ZipEntryRecord(
                name = name,
                compressionMethod = compressionMethod,
                compressedSize = compressedSize,
                uncompressedSize = uncompressedSize,
                localHeaderOffset = localHeaderOffset,
            )
        }
        return result
    }

    private fun findEocdSignature(bytes: ByteArray): Int? {
        // PK\x05\x06 en little-endian
        for (i in bytes.size - 4 downTo 0) {
            if (bytes[i] == 0x50.toByte() && bytes[i + 1] == 0x4b.toByte() &&
                bytes[i + 2] == 0x05.toByte() && bytes[i + 3] == 0x06.toByte()
            ) {
                return i
            }
        }
        return null
    }
}
