package com.klvcorp.lumio.bluetooth

import java.util.UUID

/** Community-documented ELK-BLEDOM frames. Different firmware may behave differently. */
object ElkBledomProtocol {
    private const val SUFFIX = "-0000-1000-8000-00805f9b34fb"
    val knownPairs = listOf(
        UUID.fromString("0000fff0$SUFFIX") to UUID.fromString("0000fff3$SUFFIX"),
        UUID.fromString("0000ffe5$SUFFIX") to UUID.fromString("0000ffe9$SUFFIX")
    )

    fun compatible(name: String): Boolean = listOf("ELK-BLEDOM", "ELK-BLEDOB", "LEDBLE", "MELK", "BJ_LED")
        .any { name.contains(it, ignoreCase = true) }

    fun power(on: Boolean): ByteArray = if (on) bytes(0x7E, 0x04, 0x04, 0xF0, 0, 1, 0xFF, 0, 0xEF)
        else bytes(0x7E, 0x04, 0x04, 0, 0, 0, 0xFF, 0, 0xEF)

    fun rgb(color: Int): ByteArray = bytes(0x7E, 0x07, 0x05, 0x03,
        (color shr 16) and 255, (color shr 8) and 255, color and 255, 0x10, 0xEF)

    fun brightness(percent: Int): ByteArray = bytes(0x7E, 0x04, 0x01,
        percent.coerceIn(0, 100), 0, 0, 0, 0, 0xEF)

    /** Optional native 7-color crossfade. Not all firmware revisions implement mode 0x8A. */
    fun nativeRainbow(): ByteArray = bytes(0x7E, 0x07, 0x03, 0x8A, 0x03, 0xFF, 0xFF, 0x00, 0xEF)
    fun nativeEffectSpeed(speed: Int): ByteArray = bytes(0x7E, 0x07, 0x02,
        speed.coerceIn(1, 100), 0xFF, 0xFF, 0xFF, 0x00, 0xEF)

    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }
}
