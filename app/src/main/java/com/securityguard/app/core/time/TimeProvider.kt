package com.securityguard.app.core.time
import java.time.Instant
interface TimeProvider { fun now(): Instant }
class SystemTimeProvider: TimeProvider { override fun now()=Instant.now() }
