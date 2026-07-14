package com.aistudio.xide.core.provider

/**
 * Abstraction for Authentication.
 */
interface AuthenticationProvider : XideProvider {
    val authMethod: String // e.g., OAuth, UsernamePassword
    suspend fun login(): AuthResult
    suspend fun logout()
    suspend fun getSessionToken(): String?
}

data class AuthResult(val success: Boolean, val error: String?)
