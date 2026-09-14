package com.securityguard.app.feature.checkpoints

import com.securityguard.app.core.database.CheckpointDao
import com.securityguard.app.core.database.CheckpointEntity
import com.securityguard.app.core.database.ShiftCheckpointDao
import com.securityguard.app.core.database.ShiftCheckpointEntity

class CheckpointManager(private val checkpoints: CheckpointDao, private val assignments: ShiftCheckpointDao) {
    suspend fun add(name: String, order: Int? = null, now: Long = System.currentTimeMillis()): Long {
        val actualOrder = order ?: (checkpoints.active().size + 1)
        require(name.isNotBlank()) { "Checkpoint name is required" }
        return checkpoints.insert(CheckpointEntity(name = name.trim(), displayOrder = actualOrder, active = true, createdAt = now, updatedAt = now))
    }

    suspend fun assignToShift(shiftId: Long, checkpointId: Long, order: Int, active: Boolean = true): Long =
        assignments.insert(ShiftCheckpointEntity(shiftId = shiftId, checkpointId = checkpointId, displayOrder = order, active = active))

    suspend fun activeForShift(shiftId: Long) = assignments.forShift(shiftId)
}
