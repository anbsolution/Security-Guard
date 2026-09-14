package com.securityguard.app.feature.payroll

import com.securityguard.app.core.database.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToLong

/** Deterministic payroll calculation. Money is always paise and durations are seconds. */
class PayrollCalculator(private val db: SecurityGuardDatabase) {
    data class Result(
        val basic: Long, val regular: Long, val overtime: Long,
        val bonus: Long, val allowance: Long, val advance: Long, val deduction: Long,
        val gross: Long, val net: Long
    )

    suspend fun calculate(start: LocalDate, end: LocalDate): Result {
        val setting = db.salaryDao().effective(start.toString()) ?: throw IllegalStateException("Salary settings are not configured.")
        val sessions = db.shiftSessionDao().range(start.toString(), end.toString())
        val adjustments = db.salaryAdjustmentDao().range(start.toString(), end.toString())
        val days = ChronoUnit.DAYS.between(start, end).toInt() + 1
        val regularSeconds = sessions.filter { it.sessionType != "OVERTIME" && it.actualCheckIn != null && it.actualCheckOut != null }
            .sumOf { ((it.actualCheckOut!! - it.actualCheckIn!!).coerceAtLeast(0L) / 1000L) }
        val otSeconds = sessions.filter { it.sessionType == "OVERTIME" && it.actualCheckIn != null && it.actualCheckOut != null }
            .sumOf { ((it.actualCheckOut!! - it.actualCheckIn!!).coerceAtLeast(0L) / 1000L) }
        val divisor = setting.payrollDivisor.coerceAtLeast(1)
        val hours = regularSeconds / 3600.0
        val otHoursRaw = otSeconds / 3600.0
        val otHours = roundHours(otHoursRaw, setting.otRounding)
        val regular = when (setting.salaryType) {
            "HOURLY" -> (hours * setting.regularHourlyRatePaise).roundToLong()
            "DAILY" -> (sessions.count { it.sessionType != "OVERTIME" && it.actualCheckIn != null } * setting.basicSalaryPaise)
            "MONTHLY" -> setting.basicSalaryPaise
            else -> (hours * setting.regularHourlyRatePaise).roundToLong()
        }
        val overtime = if (setting.otEnabled) {
            val rate = if (setting.otRateType == "SAME_AS_REGULAR") setting.regularHourlyRatePaise else setting.otRatePaise
            (otHours * rate).roundToLong()
        } else 0L
        val bonus = adjustments.filter { it.type == "BONUS" }.sumOf { it.amountPaise }
        val allowance = adjustments.filter { it.type == "ALLOWANCE" }.sumOf { it.amountPaise }
        val advance = adjustments.filter { it.type == "ADVANCE" }.sumOf { it.amountPaise }
        val deduction = adjustments.filter { it.type == "DEDUCTION" }.sumOf { it.amountPaise }
        val gross = regular + overtime + bonus + allowance
        val net = (gross - advance - deduction).coerceAtLeast(0L)
        return Result(setting.basicSalaryPaise, regular, overtime, bonus, allowance, advance, deduction, gross, net)
    }

    private fun roundHours(hours: Double, rounding: String): Double = when (rounding) {
        "15_MIN" -> (hours * 4).roundToLong() / 4.0
        "30_MIN" -> (hours * 2).roundToLong() / 2.0
        "1_HOUR" -> hours.roundToLong().toDouble()
        else -> hours
    }
}
