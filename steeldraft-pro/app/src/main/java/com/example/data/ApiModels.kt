package com.example.data

import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class ProfileDto(
    val uid: String,
    val email: String,
    val displayName: String,
    val organization: String,
    val membershipPlan: String,
    val projectsCount: Int,
    val maxCloudStorageProjects: Int
)

@JsonClass(generateAdapter = true)
data class AuthResponseDto(val token: String, val profile: ProfileDto)

@JsonClass(generateAdapter = true)
data class ProfileEnvelopeDto(val profile: ProfileDto)

@JsonClass(generateAdapter = true)
data class MessageDto(val message: String? = null)

@JsonClass(generateAdapter = true)
data class ErrorDto(val error: String)

@JsonClass(generateAdapter = true)
data class SignupRequest(val email: String, val password: String, val displayName: String, val organization: String)

@JsonClass(generateAdapter = true)
data class LoginRequest(val email: String, val password: String)

@JsonClass(generateAdapter = true)
data class GoogleAuthRequest(val idToken: String)

@JsonClass(generateAdapter = true)
data class ForgotPasswordRequest(val email: String)

@JsonClass(generateAdapter = true)
data class UpdatePlanRequest(val plan: String)

interface SteelDraftApi {
    @POST("api/auth/signup")
    suspend fun signup(@Body body: SignupRequest): Response<AuthResponseDto>

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponseDto>

    @POST("api/auth/google")
    suspend fun googleAuth(@Body body: GoogleAuthRequest): Response<AuthResponseDto>

    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Body body: ForgotPasswordRequest): Response<MessageDto>

    @GET("api/users/me")
    suspend fun getMe(): Response<ProfileEnvelopeDto>

    @PATCH("api/users/me/plan")
    suspend fun updatePlan(@Body body: UpdatePlanRequest): Response<ProfileEnvelopeDto>
}
