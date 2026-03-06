package com.shade.app.crypto

import org.bouncycastle.crypto.agreement.X25519Agreement
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.generators.X25519KeyPairGenerator
import org.bouncycastle.crypto.modes.ChaCha20Poly1305
import org.bouncycastle.crypto.params.HKDFParameters
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.ParametersWithIV
import org.bouncycastle.crypto.params.X25519KeyGenerationParameters
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import org.bouncycastle.util.encoders.Hex
import java.security.SecureRandom

class MessageCryptoManager {

    fun generateX25519KeyPairHex(): Pair<String, String> {
        val random = SecureRandom()
        val generator = X25519KeyPairGenerator()
        generator.init(X25519KeyGenerationParameters(random))

        val keyPair = generator.generateKeyPair()
        val publicKey = keyPair.public as X25519PublicKeyParameters
        val privateKey = keyPair.private as X25519PrivateKeyParameters

        val publicKeyHex = Hex.toHexString(publicKey.encoded)
        val privateKeyHex = Hex.toHexString(privateKey.encoded)

        return Pair(publicKeyHex, privateKeyHex)
    }

    fun generateSharedSecret(privateKeyHex: String, otherPublicKeyHex: String): String {
        val privateKeyBytes = Hex.decode(privateKeyHex)
        val otherPublicKeyBytes = Hex.decode(otherPublicKeyHex)

        val privateKeyParam = X25519PrivateKeyParameters(privateKeyBytes, 0)
        val otherPublicKeyParam = X25519PublicKeyParameters(otherPublicKeyBytes, 0)

        val agreement = X25519Agreement()
        agreement.init(privateKeyParam)

        val sharedSecretBytes = ByteArray(agreement.agreementSize)
        agreement.calculateAgreement(otherPublicKeyParam, sharedSecretBytes, 0)

        return Hex.toHexString(sharedSecretBytes)
    }

    fun deriveConversationKey(sharedSecretHex: String, keyVersion: Int): String {
        val sharedSecretBytes = Hex.decode(sharedSecretHex)

        val infoString = "Shade-Message-Key-v$keyVersion"
        val infoBytes = infoString.toByteArray(Charsets.UTF_8)

        val hkdf = HKDFBytesGenerator(SHA256Digest())
        hkdf.init(HKDFParameters(sharedSecretBytes, ByteArray(0), infoBytes))

        val derivedKey = ByteArray(32)
        hkdf.generateBytes(derivedKey, 0, 32)

        return Hex.toHexString(derivedKey)
    }

    fun encryptMessage(plaintext: String, derivedKeyHex: String): Pair<String, String> {
        val keyBytes = Hex.decode(derivedKeyHex)
        val plainBytes = plaintext.toByteArray(Charsets.UTF_8)

        val nonce = ByteArray(12)
        SecureRandom().nextBytes(nonce)

        val cipher = ChaCha20Poly1305()
        cipher.init(true, ParametersWithIV(KeyParameter(keyBytes), nonce))

        val cipherBytes = ByteArray(cipher.getOutputSize(plainBytes.size))
        val len = cipher.processBytes(plainBytes, 0, plainBytes.size, cipherBytes, 0)
        cipher.doFinal(cipherBytes, len)

        return Pair(Hex.toHexString(cipherBytes), Hex.toHexString(nonce))
    }

    fun decryptMessage(ciphertextHex: String, nonceHex: String, derivedKeyHex: String): String {
        val keyBytes = Hex.decode(derivedKeyHex)
        val ciphertextBytes = Hex.decode(ciphertextHex)
        val nonceBytes = Hex.decode(nonceHex)

        val cipher = ChaCha20Poly1305()
        cipher.init(false, ParametersWithIV(KeyParameter(keyBytes), nonceBytes))

        val plainBytes = ByteArray(cipher.getOutputSize(ciphertextBytes.size))
        val len = cipher.processBytes(ciphertextBytes, 0, ciphertextBytes.size, plainBytes, 0)

        cipher.doFinal(plainBytes, len)

        return String(plainBytes, Charsets.UTF_8)
    }

}