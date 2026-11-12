package com.example.auth

import android.content.Context
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(
    private val context: Context? = null
) : AuthRepository {

    private val auth: FirebaseAuth? by lazy {
        try {
            if (FirebaseApp.getApps(context ?: return@lazy null).isNotEmpty()) {
                FirebaseAuth.getInstance()
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkCurrentAuthState()
    }

    private fun checkCurrentAuthState() {
        val fbAuth = auth
        if (fbAuth == null) {
            _authState.value = AuthState.Unauthenticated
            return
        }

        fbAuth.addAuthStateListener { firebaseAuth ->
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                _authState.value = AuthState.Authenticated(
                    AuthUser(
                        uid = currentUser.uid,
                        email = currentUser.email ?: "",
                        displayName = currentUser.displayName ?: currentUser.email?.substringBefore("@") ?: "User",
                        homeId = "home-${currentUser.uid.take(8)}",
                        isGoogleUser = currentUser.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }
                    )
                )
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    override suspend fun signInWithEmail(email: String, pass: String): Result<AuthUser> {
        val fbAuth = auth ?: return Result.failure(Exception("Firebase not configured. Please supply google-services.json."))
        _authState.value = AuthState.Loading

        return try {
            val authResult = fbAuth.signInWithEmailAndPassword(email.trim(), pass).await()
            val user = authResult.user ?: throw Exception("User profile not found")
            val authUser = AuthUser(
                uid = user.uid,
                email = user.email ?: email,
                displayName = user.displayName ?: email.substringBefore("@"),
                homeId = "home-${user.uid.take(8)}"
            )
            _authState.value = AuthState.Authenticated(authUser)
            Result.success(authUser)
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: "Invalid email or password"
            _authState.value = AuthState.Error(msg)
            Result.failure(Exception(msg))
        }
    }

    override suspend fun registerWithEmail(name: String, email: String, pass: String): Result<AuthUser> {
        val fbAuth = auth ?: return Result.failure(Exception("Firebase not configured. Please supply google-services.json."))
        _authState.value = AuthState.Loading

        return try {
            val authResult = fbAuth.createUserWithEmailAndPassword(email.trim(), pass).await()
            val user = authResult.user ?: throw Exception("User registration failed")

            user.updateProfile(
                UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
            ).await()

            val authUser = AuthUser(
                uid = user.uid,
                email = user.email ?: email,
                displayName = name,
                homeId = "home-${user.uid.take(8)}"
            )
            _authState.value = AuthState.Authenticated(authUser)
            Result.success(authUser)
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: "Registration error"
            _authState.value = AuthState.Error(msg)
            Result.failure(Exception(msg))
        }
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        val fbAuth = auth ?: return Result.failure(Exception("Firebase not configured."))
        return try {
            fbAuth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithGoogle(): Result<AuthUser> {
        val fbAuth = auth ?: return Result.failure(Exception("Firebase not configured. Please supply google-services.json."))
        val ctx = context ?: return Result.failure(Exception("Context missing for Credential Manager."))
        _authState.value = AuthState.Loading

        return try {
            val credentialManager = CredentialManager.create(ctx)
            val googleIdOption = GetSignInWithGoogleOption.Builder("server_client_id_placeholder")
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(ctx, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleAuthProvider.GOOGLE_SIGN_IN_METHOD) {
                val googleIdToken = credential.data.getString("androidx.credentials.BUNDLE_KEY_ID_TOKEN") ?: ""
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val authResult = fbAuth.signInWithCredential(firebaseCredential).await()
                val user = authResult.user ?: throw Exception("Google sign in failed")
                val authUser = AuthUser(
                    uid = user.uid,
                    email = user.email ?: "",
                    displayName = user.displayName ?: "Google User",
                    homeId = "home-${user.uid.take(8)}",
                    isGoogleUser = true
                )
                _authState.value = AuthState.Authenticated(authUser)
                Result.success(authUser)
            } else {
                throw Exception("Unrecognized Google Credential")
            }
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: "Google Sign-In canceled or failed"
            _authState.value = AuthState.Error(msg)
            Result.failure(Exception(msg))
        }
    }

    override suspend fun signOut() {
        auth?.signOut()
        _authState.value = AuthState.Unauthenticated
    }
}
