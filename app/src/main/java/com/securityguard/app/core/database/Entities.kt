package com.securityguard.app.core.database

import androidx.room.*

@Entity(tableName = "guard")
data class GuardEntity(
    @PrimaryKey val id: Long = 1,
    val fullName: String,
    val guardId: String?,
    val dutySite: String,
    val joiningDate: String,
    val mobile: String?,
    val address: String?,
    val note: String?,
    val photoPath: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "shifts")
data class ShiftEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val startTime: String,
    val endTime: String,
    val crossMidnight: Boolean,
    val roundsEnabled: Boolean,
    val roundIntervalMinutes: Int,
    val lateAfterMinutes: Int,
    val alertRepeatMinutes: Int,
    val vibrationEnabled: Boolean,
    val active: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "shift_sessions", indices = [Index("shiftId", "dutyDate", "status")])
data class ShiftSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shiftId: Long,
    val dutyDate: String,
    val scheduledStart: Long,
    val scheduledEnd: Long,
    val actualCheckIn: Long?,
    val actualCheckOut: Long?,
    val sessionType: String,
    val status: String,
    val source: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "attendance", indices = [Index("attendanceDate", "shiftSessionId")])
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shiftSessionId: Long?,
    val attendanceDate: String,
    val status: String,
    val source: String,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "checkpoints")
data class CheckpointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val displayOrder: Int,
    val active: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "shift_checkpoints", indices = [Index(value = ["shiftId", "checkpointId"], unique = true)])
data class ShiftCheckpointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shiftId: Long,
    val checkpointId: Long,
    val displayOrder: Int,
    val active: Boolean
)

@Entity(tableName = "rounds", indices = [Index("shiftSessionId", "scheduledTime", "status")])
data class RoundEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shiftSessionId: Long,
    val scheduledTime: Long,
    val actualStart: Long?,
    val actualComplete: Long?,
    val status: String,
    val durationSeconds: Long?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "round_checkpoints", indices = [Index("roundId", "checkpointId")])
data class RoundCheckpointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roundId: Long,
    val checkpointId: Long,
    val checkpointNameSnapshot: String,
    val displayOrderSnapshot: Int,
    val completedAt: Long?,
    val status: String
)

@Entity(tableName = "leaves")
data class LeaveEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val leaveDate: String,
    val leaveType: String,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "salary_settings", indices = [Index("effectiveFrom")])
data class SalarySettingsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val effectiveFrom: String,
    val salaryType: String,
    val basicSalaryPaise: Long,
    val regularHourlyRatePaise: Long,
    val otEnabled: Boolean,
    val otRatePaise: Long,
    val otRateType: String,
    val otRounding: String,
    val paidLeaveRule: String,
    val unpaidLeaveRule: String,
    val halfDayRule: String,
    val payrollDivisor: Int,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "weekly_off_schedules", indices = [Index("effectiveFrom", "active")])
data class WeeklyOffScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val effectiveFrom: String,
    val daysOfWeek: String,
    val active: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "salary_adjustments", indices = [Index("adjustmentDate")])
data class SalaryAdjustmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val adjustmentDate: String,
    val type: String,
    val amountPaise: Long,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "payroll", indices = [Index("periodStart", "periodEnd")])
data class PayrollEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val periodStart: String,
    val periodEnd: String,
    val basicSalaryPaise: Long,
    val regularPayPaise: Long,
    val overtimePayPaise: Long,
    val bonusPaise: Long,
    val allowancePaise: Long,
    val advancePaise: Long,
    val deductionPaise: Long,
    val grossSalaryPaise: Long,
    val netSalaryPaise: Long,
    val status: String,
    val paymentStatus: String,
    val paymentDate: String?,
    val paymentMode: String?,
    val paymentNote: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "payroll_versions")
data class PayrollVersionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val payrollId: Long,
    val versionNumber: Int,
    val calculatedAt: Long,
    val grossSalaryPaise: Long,
    val netSalaryPaise: Long,
    val reason: String,
    val createdAt: Long
)

@Entity(tableName = "audit_logs", indices = [Index("entityType", "entityId", "createdAt")])
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val action: String,
    val entityType: String,
    val entityId: String,
    val oldValue: String?,
    val newValue: String?,
    val reason: String?,
    val createdAt: Long
)

@Entity(tableName = "app_settings", indices = [Index(value = ["key"], unique = true)])
data class AppSettingEntity(
    @PrimaryKey val key: String,
    val value: String,
    val updatedAt: Long
)
