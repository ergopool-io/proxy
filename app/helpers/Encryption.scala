package helpers

import java.util.Base64

import javax.crypto.{Cipher, SecretKeyFactory}
import javax.crypto.spec.{IvParameterSpec, PBEKeySpec, SecretKeySpec}

/**
 * Encryption and Decryption using specified key with AES/CBC/PKCS5Padding algorithm
 * @param key the key to use for making secret key
 */
class Encryption(key: String, secret: String) {
  private val Algorithm = "AES/CBC/PKCS5Padding"
  private val _key: SecretKeySpec = setKey(key)
  private val IvSpec = new IvParameterSpec(new Array[Byte](16))

  /**
   * Create secret key using key
   * @param value the key to use for generating secret key
   * @return secret key
   */
  private def setKey(value: String): SecretKeySpec = {
    val salt: Array[Byte] = secret.padTo(16, '_').take(16).getBytes
    val spec = new PBEKeySpec(value.toCharArray, salt, 65536, 256)
    val f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")

    val key = f.generateSecret(spec).getEncoded
    new SecretKeySpec(key, "AES")
  }

  /**
   * Encrypt text using secret key
   * @param text the text to be encrypted
   * @return encrypted text
   */
  def encrypt(text: String): String = {
    val cipher = Cipher.getInstance(Algorithm)
    cipher.init(Cipher.ENCRYPT_MODE, _key, IvSpec)

    new String(Base64.getEncoder.encode(cipher.doFinal(text.getBytes("utf-8"))), "utf-8")
  }

  /**
   * Decrypt text using secret key
   * @param text the text to be decrypted
   * @return decrypted text
   */
  def decrypt(text: String): String = {
    val cipher = Cipher.getInstance(Algorithm)
    cipher.init(Cipher.DECRYPT_MODE, _key, IvSpec)

    new String(cipher.doFinal(Base64.getDecoder.decode(text.getBytes("utf-8"))), "utf-8")
  }
}
