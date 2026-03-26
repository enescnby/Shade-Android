package com.shade.app.data.repository

import com.shade.app.data.remote.api.AuthService
import com.shade.app.data.remote.dto.LoginInitRequest
import com.shade.app.data.remote.dto.LoginInitResponse
import com.shade.app.data.remote.dto.LoginVerifyRequest
import com.shade.app.data.remote.dto.LoginVerifyResponse
import com.shade.app.data.remote.dto.RegisterRequest
import com.shade.app.data.remote.dto.RegisterResponse
import com.shade.app.domain.model.AuthResult
import com.shade.app.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authService: AuthService
) : AuthRepository {

    override suspend fun register(
        identityPublicKey: String,
        encryptedIdentityPrivateKey: String,
        encryptionPublicKey: String,
        encryptedEncryptionPrivateKey: String,
        salt: String,
        deviceModel: String,
        fcmToken: String
    ): Result<AuthResult> {
        return try {
            val response = authService.register(
                RegisterRequest(
                    identityPublicKey,
                    encryptedIdentityPrivateKey,
                    encryptionPublicKey,
                    encryptedEncryptionPrivateKey,
                    salt,
                    deviceModel,
                    fcmToken
                )
            )
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                Result.success(AuthResult(body.shadeId, body.userId, null))
            } else {
                Result.failure(Exception("Registration failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loginInit(shadeId: String): Result<LoginInitResponse> {
        return try {
            val response = authService.loginInit(LoginInitRequest(shadeId))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Login can not initialized"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loginVerify(
        shadeId: String,
        challenge: String,
        signature: String,
        deviceModel: String,
        fcmToken: String
    ): Result<AuthResult> {
        return try {
            val response = authService.loginVerify(
                LoginVerifyRequest(shadeId, challenge, signature, deviceModel, fcmToken)
            )
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                Result.success(AuthResult(body.shadeId, body.userId, body.accessToken))
            } else {
                Result.failure(Exception("Verification failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}