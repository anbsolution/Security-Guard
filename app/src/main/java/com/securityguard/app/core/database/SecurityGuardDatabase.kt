package com.securityguard.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [GuardEntity::class, ShiftEntity::class, ShiftSessionEntity::class, AttendanceEntity::class,
        CheckpointEntity::class, ShiftCheckpointEntity::class, RoundEntity::class, RoundCheckpointEntity::class,
        LeaveEntity::class, SalarySettingsEntity::class, WeeklyOffScheduleEntity::class,
        SalaryAdjustmentEntity::class, PayrollEntity::class, PayrollVersionEntity::class,
        AuditLogEntity::class, AppSettingEntity::class],
    version = 1,
    exportSchema = true
)
abstract class SecurityGuardDatabase : RoomDatabase() {
    abstract fun guardDao(): GuardDao
    abstract fun shiftDao(): ShiftDao
    abstract fun shiftSessionDao(): ShiftSessionDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun checkpointDao(): CheckpointDao
    abstract fun shiftCheckpointDao(): ShiftCheckpointDao
    abstract fun roundDao(): RoundDao
    abstract fun roundCheckpointDao(): RoundCheckpointDao
    abstract fun salaryDao(): SalaryDao
    abstract fun weeklyOffDao(): WeeklyOffDao
    abstract fun salaryAdjustmentDao(): SalaryAdjustmentDao
    abstract fun payrollDao(): PayrollDao
    abstract fun payrollVersionDao(): PayrollVersionDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun appSettingDao(): AppSettingDao

    companion object {
        fun create(context: Context): SecurityGuardDatabase = Room.databaseBuilder(
            context.applicationContext, SecurityGuardDatabase::class.java, "security_guard.db"
        ).setJournalMode(JournalMode.WRITE_AHEAD_LOGGING).build()
    }
}
