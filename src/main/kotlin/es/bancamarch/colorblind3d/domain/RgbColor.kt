package es.bancamarch.colorblind3d.domain

/**
 * Color RGB (0..255 por canal). Value object del dominio, sin dependencias externas.
 */
data class RgbColor(val red: Int, val green: Int, val blue: Int) {

    init {
        require(red in 0..255) { "red debe estar entre 0 y 255" }
        require(green in 0..255) { "green debe estar entre 0 y 255" }
        require(blue in 0..255) { "blue debe estar entre 0 y 255" }
    }

    /** Representación hexadecimal en mayúsculas sin el símbolo `#`, p.ej. "FF00AA". */
    val hex: String
        get() = "%02X%02X%02X".format(red, green, blue)

    /** Conversión a espacio de color Lab (CIE L*a*b*) pasando por sRGB linealizado y XYZ (D65). */
    fun toLab(): LabColor {
        val r = linearize(red / 255.0)
        val g = linearize(green / 255.0)
        val b = linearize(blue / 255.0)

        // Matriz sRGB -> XYZ (D65)
        val x = r * 0.4124564 + g * 0.3575761 + b * 0.1804375
        val y = r * 0.2126729 + g * 0.7151522 + b * 0.0721750
        val z = r * 0.0193339 + g * 0.1191920 + b * 0.9503041

        // Blanco de referencia D65
        val xn = x / 0.95047
        val yn = y / 1.00000
        val zn = z / 1.08883

        val fx = labF(xn)
        val fy = labF(yn)
        val fz = labF(zn)

        val l = 116.0 * fy - 16.0
        val a = 500.0 * (fx - fy)
        val bb = 200.0 * (fy - fz)

        return LabColor(l, a, bb)
    }

    private fun linearize(channel: Double): Double =
        if (channel <= 0.04045) channel / 12.92 else Math.pow((channel + 0.055) / 1.055, 2.4)

    private fun labF(t: Double): Double =
        if (t > EPSILON) Math.cbrt(t) else (KAPPA * t + 16.0) / 116.0

    companion object {
        private const val EPSILON = 216.0 / 24389.0
        private const val KAPPA = 24389.0 / 27.0

        /** Crea un [RgbColor] a partir de un hex con o sin `#`, admitiendo 3 o 6 dígitos. */
        fun fromHex(hex: String): RgbColor {
            val clean = hex.removePrefix("#").trim()
            val normalized = if (clean.length == 3) {
                clean.map { "$it$it" }.joinToString("")
            } else {
                clean
            }
            require(normalized.length >= 6) { "Color hexadecimal inválido: $hex" }
            val r = normalized.substring(0, 2).toInt(16)
            val g = normalized.substring(2, 4).toInt(16)
            val b = normalized.substring(4, 6).toInt(16)
            return RgbColor(r, g, b)
        }
    }
}
