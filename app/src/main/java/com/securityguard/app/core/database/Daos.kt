package com.securityguard.app.core.database

import androidx.room.*

@Dao
interface GuardDao {
    @Query("SELECT * FROM guard WHERE id = 1 LIMIT 1") suspend fun get(): GuardEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(value: GuardEntity)
}

@Dao
interface ShiftDao {
    @Query("SELECT * FROM shifts WHERE active = 1 ORDER BY startTime") suspend fun active(): List<ShiftEntity>
    @Query("SELECT * FROM shifts WHERE id = :id LIMIT 1") suspend fun get(id: Long): ShiftEntity?
    @Insert suspend fun insert(value: ShiftEntity): Long
    @Update suspend fun update(value: ShiftEntity)
}

@Dao
interface ShiftSessionDao {
    @Query("SELECT * FROM shift_sessions WHERE status = 'ACTIVE' LIMIT 1") suspend fun active(): ShiftSessionEntity?
    @Query("SELECT * FROM shift_sessions WHERE id = :id LIMIT 1") suspend fun get(id: Long): ShiftSessionEntity?
    @Query("SELECT * FROM shift_sessions WHERE dutyDate = :date ORDER BY actualCheckIn") suspend fun forDate(date: String): List<ShiftSessionEntity>
    @Query("SELECT * FROM shift_sessions WHERE dutyDate BETWEEN :start AND :end ORDER BY actualCheckIn") suspend fun range(start: String, end: String): List<ShiftSessionEntity>
    @Insert suspend fun insert(value: ShiftSessionEntity): Long
    @Update suspend fun update(value: ShiftSessionEntity)
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance WHERE attendanceDate BETWEEN :start AND :end ORDER BY attendanceDate DESC") suspend fun range(start: String, end: String): List<AttendanceEntity>
    @Insert suspend fun insert(value: AttendanceEntity): Long
    @Update suspend fun update(value: AttendanceEntity)
}

@Dao
interface CheckpointDao {
    @Query("SELECT * FROM checkpoints WHERE active = 1 ORDER BY displayOrder") suspend fun active(): List<CheckpointEntity>
    @Insert suspend fun insert(value: CheckpointEntity): Long
    @Update suspend fun update(value: CheckpointEntity)
}

@Dao
interface ShiftCheckpointDao {
    @Query("SELECT * FROM shift_checkpoints WHERE shiftId = :shiftId AND active = 1 ORDER BY displayOrder") suspend fun forShift(shiftId: Long): List<ShiftCheckpointEntity>
    @Insert suspend fun insert(value: ShiftCheckpointEntity): Long
}

@Dao
interface RoundDao {
    @Query("SELECT * FROM rounds WHERE shiftSessionId = :sessionId ORDER BY scheduledTime") suspend fun forSession(sessionId: Long): List<RoundEntity>
    @Query("SELECT * FROM rounds WHERE id = :id LIMIT 1") suspend fun get(id: Long): RoundEntity?
    @Query("SELECT * FROM rounds WHERE status = 'IN_PROGRESS' LIMIT 1") suspend fun active(): RoundEntity?
    @Insert suspend fun insert(value: RoundEntity): Long
    @Update suspend fun update(value: RoundEntity)
}

@Dao
interface RoundCheckpointDao {
    @Query("SELECT * FROM round_checkpoints WHERE roundId = :roundId ORDER BY displayOrderSnapshot") suspend fun forRound(roundId: Long): List<RoundCheckpointEntity>
    @Insert suspend fun insertAll(values: List<RoundCheckpointEntity>)
    @Update suspend fun update(value: RoundCheckpointEntity)
}

@Dao
interface SalaryDao {
    @Query("SELECT * FROM salary_settings WHERE effectiveFrom <= :date ORDER BY effectiveFrom DESC LIMIT 1") suspend fun effective(date: String): SalarySettingsEntity?
    @Insert suspend fun insert(value: SalarySettingsEntity): Long
    @Query("SELECT * FROM salary_settings ORDER BY effectiveFrom DESC") suspend fun all(): List<SalarySettingsEntity>
}

@Dao
interface WeeklyOffDao {
    @Query("SELECT * FROM weekly_off_schedules WHERE effectiveFrom <= :date AND active = 1 ORDER BY effectiveFrom DESC LIMIT 1") suspend fun effective(date: String): WeeklyOffScheduleEntity?
    @Insert suspend fun insert(value: WeeklyOffScheduleEntity): Long
}

@Dao
interface SalaryAdjustmentDao {
    @Query("SELECT * FROM salary_adjustments WHERE adjustmentDate BETWEEN :start AND :end ORDER BY adjustmentDate") suspend fun range(start: String, end: String): List<SalaryAdjustmentEntity>
    @Insert suspend fun insert(value: SalaryAdjustmentEntity): Long
}

@Dao
interface PayrollDao {
    @Query("SELECT * FROM payroll WHERE periodStart = :start AND periodEnd = :end LIMIT 1") suspend fun find(start: String, end: String): PayrollEntity?
    @Insert suspend fun insert(value: PayrollEntity): Long
    @Update suspend fun update(value: PayrollEntity)
    @Query("SELECT * FROM payroll WHERE periodStart = :start AND periodEnd = :end LIMIT 1") suspend fun findByPeriod(start: String, end: String): PayrollEntity?
}

@Dao
interface PayrollVersionDao {
    @Query("SELECT * FROM payroll_versions WHERE payrollId = :payrollId ORDER BY versionNumber DESC") suspend fun forPayroll(payrollId: Long): List<PayrollVersionEntity>
    @Insert suspend fun insert(value: PayrollVersionEntity): Long
}

@Dao
interface AuditLogDao {
    @Insert suspend fun insert(value: AuditLogEntity): Long
}

@Dao
interface AppSettingDao {
    @Query("SELECT * FROM app_settings WHERE `key` = :key LIMIT 1") suspend fun get(key: String): AppSettingEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun put(value: AppSettingEntity)
}
