package es.bancamarch.colorblind3d.domain

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Color en el espacio CIE L*a*b*. Permite calcular la distancia perceptual entre colores,
 * mucho más fiable para "coincidencia de color" que comparar valores RGB directamente.
 */
data class LabColor(val l: Double, val a: Double, val b: Double) {

    /** Distancia euclídea simple en el espacio Lab (CIE76). Rápida pero menos precisa. */
    fun euclideanDistance(other: LabColor): Double {
        val dl = l - other.l
        val da = a - other.a
        val db = b - other.b
        return sqrt(dl * dl + da * da + db * db)
    }

    /**
     * Distancia perceptual CIEDE2000 entre este color y [other]. Es el estándar de facto para
     * comparar cuán "parecidos" son dos colores a la vista humana.
     */
    fun deltaE2000(other: LabColor): Double {
        val avgL = (l + other.l) / 2.0
        val c1 = hypot(a, b)
        val c2 = hypot(other.a, other.b)
        val avgC = (c1 + c2) / 2.0

        val g = 0.5 * (1 - sqrt(avgC.pow(7) / (avgC.pow(7) + 25.0.pow(7))))
        val a1p = a * (1 + g)
        val a2p = other.a * (1 + g)

        val c1p = hypot(a1p, b)
        val c2p = hypot(a2p, other.b)
        val avgCp = (c1p + c2p) / 2.0

        val h1p = hueAngle(a1p, b)
        val h2p = hueAngle(a2p, other.b)

        val deltaLp = other.l - l
        val deltaCp = c2p - c1p

        val deltahp = when {
            c1p * c2p == 0.0 -> 0.0
            Math.abs(h2p - h1p) <= 180.0 -> h2p - h1p
            h2p - h1p > 180.0 -> h2p - h1p - 360.0
            else -> h2p - h1p + 360.0
        }
        val deltaHp = 2 * sqrt(c1p * c2p) * sin(Math.toRadians(deltahp) / 2.0)

        val avgHp = when {
            c1p * c2p == 0.0 -> h1p + h2p
            Math.abs(h1p - h2p) <= 180.0 -> (h1p + h2p) / 2.0
            (h1p + h2p) < 360.0 -> (h1p + h2p + 360.0) / 2.0
            else -> (h1p + h2p - 360.0) / 2.0
        }

        val t = 1 - 0.17 * cos(Math.toRadians(avgHp - 30)) +
            0.24 * cos(Math.toRadians(2 * avgHp)) +
            0.32 * cos(Math.toRadians(3 * avgHp + 6)) -
            0.20 * cos(Math.toRadians(4 * avgHp - 63))

        val deltaTheta = 30 * exp(-((avgHp - 275) / 25.0).pow(2))
        val rc = 2 * sqrt(avgCp.pow(7) / (avgCp.pow(7) + 25.0.pow(7)))
        val sl = 1 + (0.015 * (avgL - 50).pow(2)) / sqrt(20 + (avgL - 50).pow(2))
        val sc = 1 + 0.045 * avgCp
        val sh = 1 + 0.015 * avgCp * t
        val rt = -sin(Math.toRadians(2 * deltaTheta)) * rc

        val kl = 1.0
        val kc = 1.0
        val kh = 1.0

        val termL = deltaLp / (kl * sl)
        val termC = deltaCp / (kc * sc)
        val termH = deltaHp / (kh * sh)

        return sqrt(termL.pow(2) + termC.pow(2) + termH.pow(2) + rt * termC * termH)
    }

    private fun hueAngle(x: Double, y: Double): Double {
        if (x == 0.0 && y == 0.0) return 0.0
        val angle = Math.toDegrees(atan2(y, x))
        return if (angle < 0) angle + 360.0 else angle
    }
}
