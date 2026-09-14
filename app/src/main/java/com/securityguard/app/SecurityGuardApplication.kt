package com.securityguard.app

import android.app.Application
import com.securityguard.app.core.database.SecurityGuardDatabase
import com.securityguard.app.core.security.PinManager

class SecurityGuardApplication : Application() {
    val database: SecurityGuardDatabase by lazy { SecurityGuardDatabase.create(this) }
    val pinManager: PinManager by lazy { PinManager(this) }
}
