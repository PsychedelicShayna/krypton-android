package com.psychedelicshayna.krypton

import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.crypto.Cipher

import java.security.InvalidParameterException
import java.security.MessageDigest
import java.security.SecureRandom

import org.signal.argon2.*

class VaultSecurity {
    private val secureRandom = SecureRandom()

    val aesBlockSize: Int = 16

    private var ivMaskLength: Int = aesBlockSize
    private var derivedAesCipherKey: Argon2.Result? = null
    private var passwordSha256: ByteArray? = null

    private fun applyPkcs7Padding(data: ByteArray, multiple: Int = 16): ByteArray {
        val delta: Int = multiple - (data.size % multiple)

        return if(delta != multiple) {
            data.toMutableList().apply {
                addAll(Array(delta) { delta.toByte() })
            }.toByteArray()
        } else {
            data
        }
    }

    private fun stripPkcs7Padding(data: ByteArray): ByteArray {
        if(data.isEmpty()) return data

        val delta: Int = data.last().toUByte().toInt()

        val pkcs7Valid: Boolean = if(data.size >= delta) {
            data.slice(IntRange((data.size) - delta, data.size - 1)).all { it == delta.toByte() }
        } else {
            false
        }

        return if(pkcs7Valid) {
            data.copyOfRange(0, (data.size) - delta)
        } else {
            data
        }
    }

    private fun sha512Iterations(data: ByteArray, iterations: Int): ByteArray {
        val sha512: MessageDigest = MessageDigest.getInstance("SHA-512")
        var digest: ByteArray = data

        for(i in 1..iterations) {
            digest = sha512.digest(digest)
        }

        return digest
    }

    private fun deriveAesCipherKey(password: String): Argon2.Result {
        val argon2: Argon2 = Argon2.Builder(Version.V13).let {
            it.type(Type.Argon2id)
            it.memoryCost(MemoryCost.MiB(32))
            it.parallelism(4)
            it.iterations(16)
            it.hashLength(32)
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
                IvParameterSpec(ByteArray(aesBlockSize).apply {
                    secureRandom.nextBytes(this)
                })
            )
        }
    }

    fun decryptVault(decryptionInput:ByteArray): ByteArray {
        return stripPkcs7Padding(setupAesCipherObject(Cipher.DECRYPT_MODE).doFinal(decryptionInput)).let {
            it.copyOfRange(ivMaskLength, it.size)
        }
    }

    fun encryptVault(encryptionInput: ByteArray): ByteArray {
        return ByteArray(ivMaskLength).apply {
            secureRandom.nextBytes(this)
        }.toMutableList().apply {
            addAll(encryptionInput.toTypedArray())
        }.toByteArray().let {
            setupAesCipherObject(Cipher.ENCRYPT_MODE).doFinal(applyPkcs7Padding(it))
        }
    }

    fun setCryptoParameters(password: String, ivMaskLength: Int = 16) {
        if(ivMaskLength < aesBlockSize) {
            throw InvalidParameterException(
                "The provided IV mask length is smaller than the AES block size of $aesBlockSize bytes!")
        }

        derivedAesCipherKey = deriveAesCipherKey(password)
        passwordSha256 = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        this.ivMaskLength = ivMaskLength
    }

    fun verifyPassword(password: String): Boolean =
        passwordSha256?.contentEquals(MessageDigest.getInstance("SHA-256").digest(password.toByteArray())) ?: false
}