package com.shade.app.domain.usecase

import com.shade.app.crypto.AuthCryptoManager
import com.shade.app.domain.repository.AuthRepository
import com.shade.app.security.KeyVaultManager
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repository: AuthRepository,
    private val authCrypto: AuthCryptoManager,
) {
    suspend operator fun invoke(
        shadeId: String,
        mnemonic: List<String>,
        deviceModel: String,
        fcmToken: String
    ): Result<String> {
        return try {
            val initResult = repository.loginInit(shadeId)
            val loginData = initResult.getOrThrow()

            val aesKey = authCrypto.deriveAesKeyFromMnemonic(mnemonic, loginData.salt)

            val decryptedPrivateKeyHex = authCrypto.decryptPrivateKey(
                loginData.encryptedIdentityPrivateKey,
                aesKey
            )

            val signature = authCrypto.signChallenge(
                decryptedPrivateKeyHex,
                loginData.challenge
            )

            repository.loginVerify(
                shadeId = shadeId,
                challenge = loginData.challenge,
                signature = signature,
                deviceModel = deviceModel,
                fcmToken = fcmToken
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}