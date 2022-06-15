package com.psychedelicshayna.krypton

import org.signal.argon2.Argon2
import org.signal.argon2.MemoryCost
import org.signal.argon2.Type
import org.signal.argon2.Version
import java.security.InvalidParameterException
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class KryptonCrypto {
    object CryptoConstants {
        const val aesBlockSize: Int = 16

        const val argon2MemoryCostMiB: Int = 32
        const val argon2Parallelism: Int = 4
        const val argon2Iterations: Int = 16
        const val argon2HashLength: Int = 32
    }

    private val secureRandom = SecureRandom()

    private var ivMaskLength: Int = CryptoConstants.aesBlockSize
    private var derivedAesCipherKey: Argon2.Result? = null
    private var passwordSha256: ByteArray? = null

    private fun applyPkcs7Padding(data: ByteArray, multiple: Int = 16): ByteArray {
        val delta: Int = multiple - (data.size % multiple)

        return if (delta != multiple) {
            data.toMutableList().apply {
                addAll(
                    Array(delta) {
                        delta.toByte()
                    }
                )
            }.toByteArray()
        } else {
            data
        }
    }

    private fun stripPkcs7Padding(data: ByteArray): ByteArray {
        if (data.isEmpty()) return data

        val delta: Int = data.last().toUByte().toInt()

        val pkcs7Valid: Boolean = if (data.size >= delta) {
            data.slice(IntRange((data.size) - delta, data.size - 1)).all { it == delta.toByte() }
        } else {
            false
        }

        return if (pkcs7Valid) {
            data.copyOfRange(0, (data.size) - delta)
        } else {
            data
        }
    }

    private fun sha512Iterations(data: ByteArray, iterations: Int): ByteArray {
        val sha512: MessageDigest = MessageDigest.getInstance("SHA-512")
        var digest: ByteArray = data

        for (i in 1..iterations)
            digest = sha512.digest(digest)

        return digest
    }

    private fun deriveAesCipherKey(password: String): Argon2.Result {
        val argon2: Argon2 = Argon2.Builder(Version.V13).let {
            it.type(Type.Argon2id)
            it.memoryCost(MemoryCost.MiB(CryptoConstants.argon2MemoryCostMiB))
            it.parallelism(CryptoConstants.argon2Parallelism)
            it.iterations(CryptoConstants.argon2Iterations)
            it.hashLength(CryptoConstants.argon2HashLength)
            it.build()
        }

        val argon2Key: ByteArray = sha512Iterations(password.toByteArray(), 2048)
        val argon2Salt: ByteArray = sha512Iterations(argon2Key, 2048)

        return argon2.hash(argon2Key, argon2Salt)
    }

    private fun setupAesCipherObject(cipherMode: Int): Cipher {
        return Cipher.getInstance("AES_256/CBC/NoPadding").apply {
            init(
                cipherMode,
                SecretKeySpec(derivedAesCipherKey!!.hash, "AES"),
                IvParameterSpec(
                    ByteArray(CryptoConstants.aesBlockSize).apply {
                        secureRandom.nextBytes(this)
                    }
                )
            )
        }
    }

    fun decrypt(decryptionInput: ByteArray): ByteArray {
        return stripPkcs7Padding(setupAesCipherObject(Cipher.DECRYPT_MODE).doFinal(decryptionInput)).let {
            it.copyOfRange(ivMaskLength, it.size)
        }
    }

    fun encrypt(encryptionInput: ByteArray): ByteArray {
        return ByteArray(ivMaskLength).apply {
            secureRandom.nextBytes(this)
        }.toMutableList().apply {
            addAll(encryptionInput.toTypedArray())
        }.toByteArray().let {
            setupAesCipherObject(Cipher.ENCRYPT_MODE).doFinal(applyPkcs7Padding(it))
        }
    }

    fun setCryptoParameters(password: String, ivMaskLength: Int = 16) {
        if (ivMaskLength < CryptoConstants.aesBlockSize) {

            throw InvalidParameterException(
                "The provided IV mask length is smaller than the AES block size of " +
                    "${CryptoConstants.aesBlockSize} bytes!"
            )
        }

        derivedAesCipherKey = deriveAesCipherKey(password)
        passwordSha256 = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        this.ivMaskLength = ivMaskLength
    }

    fun verifyPassword(password: String): Boolean =
        passwordSha256?.contentEquals(MessageDigest.getInstance("SHA-256").digest(password.toByteArray())) ?: false
}
