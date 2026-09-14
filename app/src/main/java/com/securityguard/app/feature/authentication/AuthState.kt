package com.securityguard.app.feature.authentication

sealed interface AuthState {
    data object Locked : AuthState
    data object Authenticating : AuthState
    data object Authenticated : AuthState
    data class Error(val message: String) : AuthState
}
