package es.bancamarch.colorblind3d.domain

/** Error de dominio al leer o interpretar un fichero 3MF. */
class ThreeMfParsingException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
