package com.example.domain

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

/**
 * Exact rational representation of an Indian Rupee amount (num / den rupees).
 * Keeps intermediate OT arithmetic (e.g. 500/18 rupees per half-hour block) exact with zero
 * floating-point or premature rounding drift, rounding to paise (HALF_UP) only at final display
 * or total conversion.
 */
class RationalRupees private constructor(
    val numerator: BigInteger,
    val denominator: BigInteger
) : Comparable<RationalRupees> {

    val isZero: Boolean
        get() = numerator == BigInteger.ZERO

    val isPositive: Boolean
        get() = numerator > BigInteger.ZERO

    operator fun plus(other: RationalRupees): RationalRupees {
        val newNum = numerator.multiply(other.denominator).add(other.numerator.multiply(denominator))
        val newDen = denominator.multiply(other.denominator)
        return of(newNum, newDen)
    }

    operator fun minus(other: RationalRupees): RationalRupees {
        val newNum = numerator.multiply(other.denominator).subtract(other.numerator.multiply(denominator))
        val newDen = denominator.multiply(other.denominator)
        return of(newNum, newDen)
    }

    operator fun times(multiplier: Int): RationalRupees =
        of(numerator.multiply(BigInteger.valueOf(multiplier.toLong())), denominator)

    operator fun times(multiplier: Long): RationalRupees =
        of(numerator.multiply(BigInteger.valueOf(multiplier)), denominator)

    /** Rounds once to exact paise (1/100 rupee) using standard HALF_UP rounding. */
    fun toPaise(): Long {
        val scaled = BigDecimal(numerator.multiply(HUNDRED))
            .divide(BigDecimal(denominator), 0, RoundingMode.HALF_UP)
        return scaled.longValueExact()
    }

    /** Rounds once to rupees with 2 decimal places (paise precision, HALF_UP). */
    fun toBigDecimalRupees(): BigDecimal =
        BigDecimal(numerator).divide(BigDecimal(denominator), 2, RoundingMode.HALF_UP)

    fun toDoubleApprox(): Double =
        BigDecimal(numerator).divide(BigDecimal(denominator), 8, RoundingMode.HALF_UP).toDouble()

    /**
     * Formats in Indian Rupee grouping (e.g. ₹500, ₹527.78, ₹1,027.78, ₹1,25,450.56).
     * When [alwaysShowPaise] is false, omits ".00" for exact whole-rupee amounts while preserving
     * ".XX" whenever paise are non-zero.
     */
    fun formatInr(alwaysShowPaise: Boolean = false): String {
        val paiseTotal = toPaise()
        val negative = paiseTotal < 0L
        val absPaise = kotlin.math.abs(paiseTotal)
        val wholeRupees = absPaise / 100L
        val paisePart = (absPaise % 100L).toInt()
        val grouped = formatIndianGrouping(wholeRupees)
        val sign = if (negative) "-" else ""
        return if (alwaysShowPaise || paisePart != 0) {
            "$sign₹$grouped.${"%02d".format(paisePart)}"
        } else {
            "$sign₹$grouped"
        }
    }

    override fun compareTo(other: RationalRupees): Int =
        numerator.multiply(other.denominator).compareTo(other.numerator.multiply(denominator))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RationalRupees) return false
        return numerator == other.numerator && denominator == other.denominator
    }

    override fun hashCode(): Int = 31 * numerator.hashCode() + denominator.hashCode()

    override fun toString(): String =
        if (denominator == BigInteger.ONE) "₹$numerator" else "₹$numerator/$denominator (${formatInr(true)})"

    companion object {
        private val HUNDRED = BigInteger.valueOf(100L)

        val ZERO = RationalRupees(BigInteger.ZERO, BigInteger.ONE)
        val RUPEES_250 = of(250L)
        val RUPEES_500 = of(500L)

        /** Exact OT rate per started 30-minute block after 18:30: 500/18 = 250/9 rupees. */
        val OT_BLOCK_RATE = of(500L, 18L)

        fun of(rupees: Long, denominator: Long = 1L): RationalRupees =
            of(BigInteger.valueOf(rupees), BigInteger.valueOf(denominator))

        fun of(num: BigInteger, den: BigInteger): RationalRupees {
            require(den != BigInteger.ZERO) { "Denominator cannot be zero" }
            if (num == BigInteger.ZERO) return ZERO
            val normalizedNum: BigInteger
            val normalizedDen: BigInteger
            if (den < BigInteger.ZERO) {
                normalizedNum = num.negate()
                normalizedDen = den.negate()
            } else {
                normalizedNum = num
                normalizedDen = den
            }
            val gcd = normalizedNum.abs().gcd(normalizedDen)
            return RationalRupees(normalizedNum.divide(gcd), normalizedDen.divide(gcd))
        }

        fun formatIndianGrouping(amount: Long): String {
            val digits = amount.toString()
            if (digits.length <= 3) return digits
            val lastThree = digits.takeLast(3)
            val rest = digits.dropLast(3)
            val groupedRest = StringBuilder()
            var i = rest.length
            while (i > 0) {
                val start = (i - 2).coerceAtLeast(0)
                if (groupedRest.isNotEmpty()) groupedRest.insert(0, ',')
                groupedRest.insert(0, rest.substring(start, i))
                i = start
            }
            return "$groupedRest,$lastThree"
        }
    }
}

fun Iterable<RationalRupees>.sumRupees(): RationalRupees {
    var acc = RationalRupees.ZERO
    for (item in this) {
        acc += item
    }
    return acc
}
