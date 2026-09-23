package com.example.data

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.example.model.MembershipTier
import com.example.model.UserProfile
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import retrofit2.Response

/**
 * Web client ID from Google Cloud Console -> APIs & Services -> Credentials ->
 * OAuth 2.0 Client IDs -> "Web application". Put the SAME value in
 * GOOGLE_WEB_CLIENT_ID on the server (/steeldraft-server/.env). Required only
 * for the "Continue with Google" button; email/password works without it.
 */
const val GOOGLE_WEB_CLIENT_ID = "REPLACE_WITH_YOUR_GOOGLE_WEB_CLIENT_ID"

sealed class AuthOutcome {
    data class Success(val profile: UserProfile) : AuthOutcome()
    data class Error(val message: String) : AuthOutcome()
}

/**
 * Talks to our own self-hosted auth server (see /steeldraft-server) — no
 * Firebase, no third-party auth billing. Sessions are a JWT stored on-device
 * via [TokenStore]; the server is the only source of truth for accounts.
 */
class AuthRepository(context: Context) {
    private val tokenStore = TokenStore(context)
    private val api = NetworkModule.createApi(context, tokenStore)

    suspend fun signUp(email: String, password: String, displayName: String, organization: String): AuthOutcome {
        if (password.length < 6) return AuthOutcome.Error("Password must be at least 6 characters.")
        return runCatching {
            api.signup(
                SignupRequest(
                    email = email.trim(),
                    password = password,
                    displayName = displayName.ifBlank { "CAD Engineer" },
                    organization = organization.ifBlank { "Independent Engineer" }
                )
            )
        }.fold(
            onSuccess = { handleAuthResponse(it) },
            onFailure = { AuthOutcome.Error(networkErrorMessage(it)) }
        )
    }

    suspend fun signIn(email: String, password: String): AuthOutcome = runCatching {
        api.login(LoginRequest(email = email.trim(), password = password))
    }.fold(
        onSuccess = { handleAuthResponse(it) },
        onFailure = { AuthOutcome.Error(networkErrorMessage(it)) }
    )

    /** Google sign-in through Credential Manager; our server verifies the ID token itself. */
    suspend fun signInWithGoogle(context: Context): AuthOutcome {
        if (GOOGLE_WEB_CLIENT_ID.startsWith("REPLACE_WITH")) {
            return AuthOutcome.Error(
                "Google sign-in isn't configured yet. Set GOOGLE_WEB_CLIENT_ID in " +
                    "AuthRepository.kt (Android) and .env (server)."
            )
        }
        return try {
            val credentialManager = CredentialManager.create(context)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(GOOGLE_WEB_CLIENT_ID)
                .build()
            val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
            val response = credentialManager.getCredential(context, request)
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(response.credential.data)
            val apiResponse = api.googleAuth(GoogleAuthRequest(idToken = googleIdTokenCredential.idToken))
            handleAuthResponse(apiResponse)
        } catch (e: GetCredentialException) {
            AuthOutcome.Error(e.localizedMessage ?: "Google sign-in was cancelled.")
        } catch (e: Exception) {
            AuthOutcome.Error(networkErrorMessage(e))
        }
    }

    suspend fun sendPasswordReset(email: String): AuthOutcome = runCatching {
        api.forgotPassword(ForgotPasswordRequest(email = email.trim()))
    }.fold(
        onSuccess = { resp ->
            if (resp.isSuccessful) AuthOutcome.Success(UserProfile(email = email.trim()))
            else AuthOutcome.Error(parseError(resp))
        },
        onFailure = { AuthOutcome.Error(networkErrorMessage(it)) }
    )

    fun signOut() = tokenStore.clear()

    /** Called on app start: if a session token exists, ask the server whether it's still valid. */
    suspend fun restoreSession(): UserProfile? {
        if (tokenStore.getToken().isNullOrBlank()) return null
        return runCatching { api.getMe() }.getOrNull()?.let { resp ->
            if (resp.isSuccessful) resp.body()?.profile?.toDomain() else {
                tokenStore.clear() // stale/expired token
                null
            }
        }
    }

    suspend fun updateMembership(tier: MembershipTier): AuthOutcome = runCatching {
        api.updatePlan(UpdatePlanRequest(plan = tier.name))
    }.fold(
        onSuccess = { resp ->
            if (resp.isSuccessful) resp.body()?.profile?.toDomain()?.let { AuthOutcome.Success(it) }
                ?: AuthOutcome.Error("Server returned no profile.")
            else AuthOutcome.Error(parseError(resp))
        },
        onFailure = { AuthOutcome.Error(networkErrorMessage(it)) }
    )

    private fun handleAuthResponse(response: Response<AuthResponseDto>): AuthOutcome {
        if (!response.isSuccessful) return AuthOutcome.Error(parseError(response))
        val body = response.body() ?: return AuthOutcome.Error("Empty response from server.")
        tokenStore.saveToken(body.token)
        return AuthOutcome.Success(body.profile.toDomain())
    }

    private fun parseError(response: Response<*>): String {
        val raw = response.errorBody()?.string()
        val extracted = raw?.substringAfter("\"error\":\"")?.substringBefore('"')
        return extracted?.takeIf { it.isNotBlank() && it != raw } ?: when (response.code()) {
            401 -> "Incorrect e-mail or password."
            404 -> "No account found for that e-mail."
            409 -> "An account already exists for that e-mail."
            in 500..599 -> "Server error — try again in a moment."
            else -> "Something went wrong (code ${response.code()})."
        }
    }

    private fun networkErrorMessage(t: Throwable): String = when {
        t.message?.contains("Unable to resolve host", true) == true ->
            "Can't reach the server. Check API_BASE_URL and your connection."
        t.message?.contains("Connection refused", true) == true ->
            "Can't reach the server — is it running?"
        else -> t.localizedMessage ?: "Something went wrong. Please try again."
    }
}

private fun ProfileDto.toDomain() = UserProfile(
    uid = uid,
    email = email,
    displayName = displayName,
    organization = organization,
    membershipPlan = runCatching { MembershipTier.valueOf(membershipPlan) }.getOrDefault(MembershipTier.FREE_STARTER),
    projectsCount = projectsCount,
    maxCloudStorageProjects = maxCloudStorageProjects
)
