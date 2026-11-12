package com.example.auth

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AuthUser(
    val uid: String,
    val email: String,
    val displayName: String,
    val homeId: String,
    val isGoogleUser: Boolean = false
)

sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: AuthUser) : AuthState()
    object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

interface AuthRepository {
    val authState: StateFlow<AuthState>
    suspend fun signInWithEmail(email: String, pass: String): Result<AuthUser>
    suspend fun registerWithEmail(name: String, email: String, pass: String): Result<AuthUser>
    suspend fun sendPasswordReset(email: String): Result<Unit>
    suspend fun signInWithGoogle(): Result<AuthUser>
    suspend fun signOut()
}

class MockAuthRepository : AuthRepository {
    private val defaultUser = AuthUser(
        uid = "usr-sharma-001",
        email = "sharma.family@volto.in",
        displayName = "Suresh Sharma",
        homeId = "home-blr-bellandur"
    )

    private val _authState = MutableStateFlow<AuthState>(AuthState.Authenticated(defaultUser))
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    override suspend fun signInWithEmail(email: String, pass: String): Result<AuthUser> {
        _authState.value = AuthState.Loading
        delay(1000)
        if (email.isBlank() || !email.contains("@")) {
            val err = "Please enter a valid email address."
            _authState.value = AuthState.Error(err)
            return Result.failure(Exception(err))
        }
        if (pass.length < 6) {
            val err = "Password must be at least 6 characters."
            _authState.value = AuthState.Error(err)
            return Result.failure(Exception(err))
        }

        val user = AuthUser(
            uid = "usr-${email.hashCode()}",
            email = email,
            displayName = email.substringBefore("@").replace(".", " ").capitalize(),
            homeId = "home-${email.hashCode()}"
        )
        _authState.value = AuthState.Authenticated(user)
        return Result.success(user)
    }

    override suspend fun registerWithEmail(name: String, email: String, pass: String): Result<AuthUser> {
        _authState.value = AuthState.Loading
        delay(1200)
        if (name.isBlank()) {
            val err = "Please enter your full name."
            _authState.value = AuthState.Error(err)
            return Result.failure(Exception(err))
        }
        if (email.isBlank() || !email.contains("@")) {
            val err = "Please enter a valid email address."
            _authState.value = AuthState.Error(err)
            return Result.failure(Exception(err))
        }
        if (pass.length < 6) {
            val err = "Password must be at least 6 characters."
            _authState.value = AuthState.Error(err)
            return Result.failure(Exception(err))
        }

        val user = AuthUser(
            uid = "usr-${email.hashCode()}",
            email = email,
            displayName = name,
            homeId = "home-${email.hashCode()}"
        )
        _authState.value = AuthState.Authenticated(user)
        return Result.success(user)
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        delay(800)
        if (email.isBlank() || !email.contains("@")) {
            return Result.failure(Exception("Please provide a valid registered email address."))
        }
        return Result.success(Unit)
    }

    override suspend fun signInWithGoogle(): Result<AuthUser> {
        _authState.value = AuthState.Loading
        delay(1200)
        val user = AuthUser(
            uid = "usr-google-999",
            email = "suresh.sharma.blr@gmail.com",
            displayName = "Suresh Sharma",
            homeId = "home-blr-bellandur",
            isGoogleUser = true
        )
        _authState.value = AuthState.Authenticated(user)
        return Result.success(user)
    }

    override suspend fun signOut() {
        _authState.value = AuthState.Unauthenticated
    }
}
