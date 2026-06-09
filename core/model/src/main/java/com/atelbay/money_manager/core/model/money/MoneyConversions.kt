package com.atelbay.money_manager.core.model.money

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Money is stored everywhere as [Long] minor units at a fixed scale of 2 (hundredths),
 * currency-independent. All supported currencies are 2-decimal and the UI uniformly renders
 * 2 fraction digits, so `minor / 100.0` is display-identical to the previous `Double` storage.
 *
 * Rounding is [RoundingMode.HALF_UP] at every major→minor boundary. Conversion guards against
 * `Long` overflow ([MAX_MAJOR_UNITS]) instead of silently wrapping.
 */
const val MINOR_UNIT_SCALE = 2

private val SCALE_FACTOR: BigDecimal = BigDecimal.TEN.pow(MINOR_UNIT_SCALE) // 100

/** Largest major-unit magnitude representable as [Long] minor units (`Long.MAX / 100`). */
const val MAX_MAJOR_UNITS = 9.2e16

/**
 * Converts a major-unit [BigDecimal] to [Long] minor units, rounding HALF_UP at scale 2.
 * @throws ArithmeticException if the value overflows [Long].
 */
fun BigDecimal.toMinorUnits(): Long =
    this.setScale(MINOR_UNIT_SCALE, RoundingMode.HALF_UP)
        .movePointRight(MINOR_UNIT_SCALE)
        .longValueExact()

/** Converts a major-unit [Double] to [Long] minor units. Uses `toString()` to avoid IEEE artifacts. */
fun majorToMinor(major: Double): Long {
    if (major.isNaN() || major.isInfinite() || kotlin.math.abs(major) > MAX_MAJOR_UNITS) {
        throw ArithmeticException("Money value out of range: $major")
    }
    return BigDecimal(major.toString()).toMinorUnits()
}

/** Converts a major-unit [BigDecimal] to [Long] minor units. */
fun majorToMinor(major: BigDecimal): Long = major.toMinorUnits()

/**
 * Parses a user-entered string into [Long] minor units, or `null` if it is not a valid number.
 * Sanitizes to digits plus a single decimal separator (`.` or `,`), then rounds HALF_UP.
 */
fun String.parseToMinorUnitsOrNull(): Long? {
    val normalized = this.trim().replace(',', '.')
    if (normalized.isEmpty()) return null
    // Keep digits, a leading minus, and the first '.'; drop everything else (spaces, grouping).
    val sb = StringBuilder()
    var dotSeen = false
    for ((index, c) in normalized.withIndex()) {
        when {
            c == '-' && index == 0 -> sb.append(c)
            c == '.' && !dotSeen -> { sb.append(c); dotSeen = true }
            c.isDigit() -> sb.append(c)
        }
    }
    val cleaned = sb.toString()
    if (cleaned.isEmpty() || cleaned == "-" || cleaned == ".") return null
    return try {
        BigDecimal(cleaned).toMinorUnits()
    } catch (e: NumberFormatException) {
        null
    } catch (e: ArithmeticException) {
        null
    }
}

/** Converts [Long] minor units to a major-unit [BigDecimal] (exact, no FP). */
fun Long.toMajor(): BigDecimal = BigDecimal(this).movePointLeft(MINOR_UNIT_SCALE)

/** Converts [Long] minor units to a major-unit [Double] — for display/animation only. */
fun Long.toMajorDouble(): Double = this / 100.0

/** Converts [Long] minor units to a plain major-unit string for edit pre-fill (no trailing zeros). */
fun Long.toMajorPlainString(): String = toMajor().stripTrailingZeros().toPlainString()
