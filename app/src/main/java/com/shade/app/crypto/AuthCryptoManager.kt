package com.shade.app.crypto

import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import org.bouncycastle.util.encoders.Hex
import java.security.SecureRandom

class AuthCryptoManager {

    fun generateEd25519KeyPairHex(): Pair<String, String> {
        val random = SecureRandom()
        val generator = Ed25519KeyPairGenerator()
        generator.init(Ed25519KeyGenerationParameters(random))

        val keyPair = generator.generateKeyPair()
        val publicKey = keyPair.public as Ed25519PublicKeyParameters
        val privateKey = keyPair.private as Ed25519PrivateKeyParameters

        val publicKeyHex = Hex.toHexString(publicKey.encoded)
        val privateKeyHex = Hex.toHexString(privateKey.encoded)

        return Pair(publicKeyHex, privateKeyHex)
    }

    fun signChallenge(privateKeyHex: String, challengeHex: String): String {
        val privateKeyBytes = Hex.decode(privateKeyHex)
        val challengeBytes = Hex.decode(challengeHex)

        val privateKeyParam = Ed25519PrivateKeyParameters(privateKeyBytes, 0)

        val signer = Ed25519Signer()
        signer.init(true, privateKeyParam)
        signer.update(challengeBytes, 0, challengeBytes.size)

        val signatureBytes = signer.generateSignature()

        return Hex.toHexString(signatureBytes)
    }
}