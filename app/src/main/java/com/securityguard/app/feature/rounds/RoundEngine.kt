package com.securityguard.app.feature.rounds

import com.securityguard.app.core.database.CheckpointDao
import com.securityguard.app.core.database.RoundCheckpointEntity
import com.securityguard.app.core.database.RoundDao
import com.securityguard.app.core.database.RoundEntity
import com.securityguard.app.core.database.ShiftEntity
import com.securityguard.app.core.database.ShiftSessionEntity
import com.securityguard.app.core.notifications.RoundAlertScheduler
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Pure scheduling/generation logic. Business state is persisted in Room. */
object RoundEngine {
    fun scheduledTimes(session: ShiftSessionEntity, shift: ShiftEntity, zone: ZoneId): List<Long> {
        if (!shift.roundsEnabled || shift.roundIntervalMinutes <= 0) return emptyList()
        val start = Instant.ofEpochMilli(session.scheduledStart).atZone(zone).toLocalDateTime()
        val end = Instant.ofEpochMilli(session.scheduledEnd).atZone(zone).toLocalDateTime()
        val result = mutableListOf<Long>()
        var cursor = start
        while (cursor.isBefore(end)) {
            result += cursor.atZone(zone).toInstant().toEpochMilli()
            cursor = cursor.plusMinutes(shift.roundIntervalMinutes.toLong())
        }
        return result
    }

    fun resolveStatus(round: RoundEntity, now: Long, lateAfterMinutes: Int): String {
        return when (round.status) {
            "SCHEDULED" -> if (now >= round.scheduledTime + lateAfterMinutes.coerceAtLeast(0) * 60_000L) "LATE" else "SCHEDULED"
            "LATE" -> "LATE"
            else -> round.status
        }
    }
}

class RoundRepository(private val rounds: RoundDao, private val roundCheckpoints: com.securityguard.app.core.database.RoundCheckpointDao, private val checkpoints: CheckpointDao, private val shiftCheckpoints: com.securityguard.app.core.database.ShiftCheckpointDao? = null, private val alertScheduler: RoundAlertScheduler? = null) {
    suspend fun generateForSession(session: ShiftSessionEntity, shift: ShiftEntity, zone: ZoneId, now: Long = System.currentTimeMillis()): Int {
        if (!shift.roundsEnabled) return 0
        val existing = rounds.forSession(session.id).map { it.scheduledTime }.toHashSet()
        val assignedIds = checkpoints.active().map { it.id }.toSet()
        var created = 0
        RoundEngine.scheduledTimes(session, shift, zone).forEach { scheduled ->
            if (scheduled in existing) return@forEach
            val status = if (now >= scheduled + shift.lateAfterMinutes.coerceAtLeast(0) * 60_000L) "LATE" else "SCHEDULED"
            val id = rounds.insert(RoundEntity(shiftSessionId = session.id, scheduledTime = scheduled, actualStart = null, actualComplete = null, status = status, durationSeconds = null, createdAt = now, updatedAt = now))
            val createdRound = rounds.get(id) ?: return@forEach
            val assigned = shiftCheckpoints?.forShift(shift.id)?.filter { it.active && it.checkpointId in assignedIds }
                ?.mapNotNull { sc -> checkpoints.active().firstOrNull { it.id == sc.checkpointId } }
                ?.sortedBy { it.displayOrder }
                ?: checkpoints.active()
            roundCheckpoints.insertAll(assigned.map { cp -> RoundCheckpointEntity(id, id, cp.id, cp.name, cp.displayOrder, null, "PENDING") })
            alertScheduler?.schedule(createdRound, shift)
            created++
        }
        return created
    }

    suspend fun start(roundId: Long, now: Long = System.currentTimeMillis()) {
        val round = rounds.get(roundId) ?: error("Round not found")
        require(round.status == "SCHEDULED" || round.status == "LATE") { "Round cannot be started from ${round.status}" }
        require(rounds.active() == null) { "Another round is already in progress" }
        rounds.update(round.copy(actualStart = now, status = "IN_PROGRESS", updatedAt = now))
        alertScheduler?.cancel(roundId)
    }

    suspend fun completeCheckpoint(roundId: Long, checkpointId: Long, now: Long = System.currentTimeMillis()) {
        val round = rounds.get(roundId) ?: error("Round not found")
        require(round.status == "IN_PROGRESS") { "Round is not in progress" }
        val item = roundCheckpoints.forRound(roundId).firstOrNull { it.checkpointId == checkpointId } ?: error("Checkpoint not assigned")
        require(item.status == "PENDING") { "Checkpoint already completed" }
        roundCheckpoints.update(item.copy(completedAt = now, status = "COMPLETED"))
    }

    suspend fun complete(roundId: Long, now: Long = System.currentTimeMillis()) {
        val round = rounds.get(roundId) ?: error("Round not found")
        require(round.status == "IN_PROGRESS") { "Round is not in progress" }
        require(roundCheckpoints.forRound(roundId).all { it.status == "COMPLETED" }) { "All checkpoints must be completed" }
        val start = round.actualStart ?: error("Missing start time")
        rounds.update(round.copy(actualComplete = now, status = "COMPLETED", durationSeconds = ((now - start).coerceAtLeast(0L) / 1000L), updatedAt = now))
        alertScheduler?.cancel(roundId)
    }

    suspend fun syncStatuses(session: ShiftSessionEntity, shift: ShiftEntity, now: Long = System.currentTimeMillis()): Int {
        val all = rounds.forSession(session.id)
        var changed = 0
        val unresolved = all.filter { it.status == "SCHEDULED" || it.status == "LATE" }.sortedBy { it.scheduledTime }
        // Once a later scheduled round has arrived, an earlier unresolved round is MISSED.
        unresolved.forEach { round ->
            val laterRoundArrived = all.any {
                it.scheduledTime > round.scheduledTime && it.scheduledTime <= now
            }
            if (laterRoundArrived) {
                rounds.update(round.copy(status = "MISSED", updatedAt = now))
                alertScheduler?.cancel(round.id)
                changed++
            } else {
                val nextStatus = RoundEngine.resolveStatus(round, now, shift.lateAfterMinutes)
                if (nextStatus != round.status) {
                    rounds.update(round.copy(status = nextStatus, updatedAt = now))
                    if (nextStatus == "LATE") alertScheduler?.schedule(round.copy(status = nextStatus), shift)
                    changed++
                }
            }
        }
        return changed
    }
}
