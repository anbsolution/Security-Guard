package com.securityguard.app.feature.shifts

import com.securityguard.app.core.database.*
import java.time.*
import com.securityguard.app.feature.rounds.RoundRepository
import com.securityguard.app.core.notifications.RoundAlertScheduler

class ShiftManager(private val db: SecurityGuardDatabase) {
    suspend fun activeShifts(): List<ShiftEntity> = db.shiftDao().active()

    private fun todaySelectionKey(): String =
        "today_selected_shift_id_${LocalDate.now(ZoneId.systemDefault())}"

    suspend fun selectedShiftId(): Long? =
        db.appSettingDao().get(todaySelectionKey())?.value?.toLongOrNull()

    suspend fun selectTodayShift(shiftId: Long) {
        check(activeSession() == null) {
            "Check-Out the active shift before changing today's shift"
        }
        require(db.shiftDao().get(shiftId)?.active == true) {
            "Selected shift is not active"
        }

        db.appSettingDao().put(
            AppSettingEntity(
                todaySelectionKey(),
                shiftId.toString(),
                System.currentTimeMillis()
            )
        )
    }

    suspend fun activeSession(): ShiftSessionEntity? = db.shiftSessionDao().active()

    suspend fun startShift(sessionType: String = "REGULAR"): ShiftSessionEntity {
        check(activeSession() == null) { "Check-Out the active shift before starting another shift" }
        val shiftId = selectedShiftId() ?: error("Select today's shift first")
        val shift = db.shiftDao().get(shiftId) ?: error("Selected shift not found")
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val start = LocalTime.parse(shift.startTime)
        val end = LocalTime.parse(shift.endTime)
        val scheduledStart = ZonedDateTime.of(today, start, zone).toInstant().toEpochMilli()
        val endDate = if (shift.crossMidnight || !end.isAfter(start)) today.plusDays(1) else today
        val scheduledEnd = ZonedDateTime.of(endDate, end, zone).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()
        val sessionId = db.shiftSessionDao().insert(
            ShiftSessionEntity(0, shift.id, today.toString(), scheduledStart, scheduledEnd, now, null,
                sessionType, "ACTIVE", "AUTOMATIC", now, now)
        )
        db.attendanceDao().insert(AttendanceEntity(0, sessionId, today.toString(), "PRESENT", "AUTOMATIC", null, now, now))
        val session = db.shiftSessionDao().get(sessionId)!!
        if (shift.roundsEnabled) {
            val repo = RoundRepository(db.roundDao(), db.roundCheckpointDao(), db.checkpointDao(), db.shiftCheckpointDao())
            repo.generateForSession(session, shift, zone, now)
        }
        return session
    }

    suspend fun checkOut(): ShiftSessionEntity {
        val session = activeSession() ?: error("No active shift")
        val activeRound = db.roundDao().active()
        check(activeRound == null || activeRound.shiftSessionId != session.id) { "Complete the active round before Check-Out" }
        val now = System.currentTimeMillis()
        val updated = session.copy(actualCheckOut = now, status = "COMPLETED", updatedAt = now)
        db.shiftSessionDao().update(updated)
        return updated
    }

    suspend fun addShift(name: String, type: String, start: String, end: String, crossMidnight: Boolean,
                          roundsEnabled: Boolean, interval: Int = 60, lateAfter: Int = 1, repeat: Int = 5): Long {
        require(name.isNotBlank())
        LocalTime.parse(start)
        LocalTime.parse(end)
        val now = System.currentTimeMillis()
        return db.shiftDao().insert(ShiftEntity(0, name.trim(), type, start, end, crossMidnight,
            roundsEnabled, interval.coerceAtLeast(1), lateAfter.coerceAtLeast(1), repeat.coerceAtLeast(1), true, true, now, now))
    }
}
