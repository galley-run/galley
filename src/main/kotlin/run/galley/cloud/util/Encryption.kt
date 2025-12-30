package run.galley.cloud.util

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object Encryption {
  private const val ALGORITHM = "AES/CBC/PKCS5Padding"
  private const val KEY_ALGORITHM = "AES"

  /**
   * Encrypts a string using AES-256-CBC encryption
   * @param plaintext The string to encrypt
   * @param key The encryption key (will be hashed to 256 bits)
   * @return Base64-encoded encrypted string with IV prepended
   */
  fun encrypt(
    plaintext: String?,
    key: String,
  ): String? {
    if (plaintext == null) return null

    // Derive a 256-bit key from the provided key using SHA-256
    val keyBytes = MessageDigest.getInstance("SHA-256").digest(key.toByteArray(StandardCharsets.UTF_8))
    val secretKey = SecretKeySpec(keyBytes, KEY_ALGORITHM)

    // Generate a random IV
    val iv = ByteArray(16)
    SecureRandom().nextBytes(iv)
    val ivSpec = IvParameterSpec(iv)

    // Encrypt
    val cipher = Cipher.getInstance(ALGORITHM)
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
    val encrypted = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))

    // Prepend IV to encrypted data and encode as Base64
    val combined = iv + encrypted
    return Base64.getEncoder().encodeToString(combined)
  }

  /**
   * Decrypts a string using AES-256-CBC encryption
   * @param ciphertext Base64-encoded encrypted string with IV prepended
   * @param key The encryption key (will be hashed to 256 bits)
   * @return Decrypted plaintext string
   */
  fun decrypt(
    ciphertext: String?,
    key: String,
  ): String? {
    if (ciphertext == null) return null

    // Derive a 256-bit key from the provided key using SHA-256
    val keyBytes = MessageDigest.getInstance("SHA-256").digest(key.toByteArray(StandardCharsets.UTF_8))
    val secretKey = SecretKeySpec(keyBytes, KEY_ALGORITHM)

    // Decode Base64
    val combined = Base64.getDecoder().decode(ciphertext)

    // Extract IV and encrypted data
    val iv = combined.sliceArray(0 until 16)
    val encrypted = combined.sliceArray(16 until combined.size)
    val ivSpec = IvParameterSpec(iv)

    // Decrypt
    val cipher = Cipher.getInstance(ALGORITHM)
    cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
    val decrypted = cipher.doFinal(encrypted)

    return String(decrypted, StandardCharsets.UTF_8)
  }
}
