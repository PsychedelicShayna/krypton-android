package com.psychedelicshayna.krypton

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.InvalidParameterException
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Security
import java.util.stream.Collectors
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


class VaultSecurity {
    private val secureRandom = SecureRandom()

    private val aesBlockSize: Int = 16
    private var aesCipherKey: ByteArray? = null
    private var aesCipherKeyDigest: ByteArray? = null
    private var ivMaskLength: Int? = null

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

        val delta: Int = data.last().toInt()

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

    fun setCryptoParameters(password: String, hashingAlgorithm: String, aesKeyLengthString: String, ivMaskLength: Int?) {
        val proposedAesKeyLength = when(aesKeyLengthString) {
            "AES-128" -> 16
            "AES-196" -> 24
            "AES-256" -> 32
            else      -> 0
        }

        if(ivMaskLength != null && ivMaskLength < aesBlockSize) {
            throw InvalidParameterException(
                "The provided IV mask length is smaller than the AES block size of $aesBlockSize bytes!")
        }

        if(proposedAesKeyLength == 0) {
            throw InvalidParameterException(
                  "The provided AES key length string ($aesKeyLengthString) doesn't match any known"
                + "strings, and so it cannot be resolved into an integer!"
            )
        }

        val passwordDigestObject = MessageDigest.getInstance(hashingAlgorithm)
        val proposedAesCipherKeyDigest: ByteArray = passwordDigestObject.digest(password.toByteArray())

        if(proposedAesCipherKeyDigest.size < proposedAesKeyLength) {
            throw InvalidParameterException(
                "The provided hashing algorithm produces an output that is smaller than the requested AES key length!")
        }

        aesCipherKey = proposedAesCipherKeyDigest.copyOfRange(0, proposedAesKeyLength)
        aesCipherKeyDigest = proposedAesCipherKeyDigest
        this.ivMaskLength = ivMaskLength
    }

    private fun setupAesCipherObject(cipherMode: Int): Cipher {
        if(aesCipherKey == null)  {
            throw InvalidParameterException("The AES cipher key is null, and cannot be used for encryption.")
        } else if(aesCipherKey!!.size !in arrayOf(16, 24, 32)) {
            throw InvalidParameterException(
                "The AES cipher key has an invalid size! Expected a key with size 16, 24, or 32, "
                        + "but got a key with size ${aesCipherKey!!.size} instead.")
        }

        val aesCipherObject: Cipher = Cipher.getInstance("AES_${aesCipherKey!!.size * 8}/CBC/NoPadding")

        if((aesCipherKeyDigest?.size ?: 0) < aesBlockSize) {
            throw InvalidParameterException(
                "The AES cipher key digest is smaller than the AES block size; expected >= $aesBlockSize, got"
                        + "${aesCipherKeyDigest?.size ?: "null"} instead!")
        }

        val initializationVector: ByteArray = if(ivMaskLength != null) {
            ByteArray(aesBlockSize).apply { secureRandom.nextBytes(this) }
        } else {
            aesCipherKeyDigest!!.copyOfRange(0, aesCipherObject.blockSize)
        }

        aesCipherObject.init(cipherMode, SecretKeySpec(aesCipherKey, "AES"), IvParameterSpec(initializationVector))
        return aesCipherObject
    }

    fun decryptVault(decryptionInput:ByteArray): ByteArray {
        val cipherObject: Cipher = setupAesCipherObject(Cipher.DECRYPT_MODE)

        return stripPkcs7Padding(cipherObject.doFinal(decryptionInput)).let {
            if(ivMaskLength != null) { it.copyOfRange(ivMaskLength!!, it.size) } else { it }
        }
    }

    fun encryptVault(encryptionInput: ByteArray) : ByteArray {
        val cipherObject: Cipher = setupAesCipherObject(Cipher.ENCRYPT_MODE)

        return if(ivMaskLength != null) {
            val ivMaskBytes: MutableList<Byte> = ByteArray(ivMaskLength!!).apply { secureRandom.nextBytes(this) }.toMutableList()
            ivMaskBytes.apply { addAll(encryptionInput.toTypedArray()) }.toByteArray()
        } else {
            encryptionInput
        }.let {
            cipherObject.doFinal(applyPkcs7Padding(it))
        }
    }
}

fun main() {
//    for (provider in Security.getProviders()) {
//        for (service in provider.services) {
//            val algorithm = service.algorithm
//            println(algorithm)
//        }
//    }
    val vaultSecurity = VaultSecurity()
    vaultSecurity.setCryptoParameters("Password", "SHA-512", "AES-256", 4096)

    val path = Paths.get(".")
    Files.list(path).collect(Collectors.partitioningBy { Files.isRegularFile(it) })

    Path.listDirectoryEntries()
}
