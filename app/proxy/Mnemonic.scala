package proxy

import java.io.{File, PrintWriter}
import java.nio.file.{Files, Paths}
import java.security.SecureRandom

import com.github.alanverbner.bip39.{EnglishWordList, Entropy256, WordList, check, generate}
import helpers.Encryption
import javax.crypto.BadPaddingException
import org.ergoplatform.appkit._
import proxy.node.Node

import scala.io.Source

object Mnemonic {
  private var _value: String = _
  private val filename: String = Config.mnemonicFilename
  private var _address: String = _

  private def createFile(value: String): Unit = {
    val printWriter = new PrintWriter(new File(filename))
    printWriter.write(value)
    printWriter.close()
  }

  private def readFile(): String = {
    val f = Source.fromFile(filename)
    val line = f.getLines().mkString
    f.close()

    line
  }

  private def reload(): Unit = {
    _value = null
    _address = null
  }

  /**
   * The mnemonic value
   * @return mnemonic
   */
  def value: String = this._value

  /**
   * Address creating using mnemonic
   * @return address
   */
  def address: String = _address

  /**
   * Create address using mnemonic value
   */
  def createAddress(): Unit = {
    val secretKey = JavaHelpers.seedToMasterKey(this._value)
    val pk = secretKey.key.publicImage
    val nodeWalletAddress = {
      try {
        Node.walletAddresses.apply(0)
      } catch {
        case _: IndexOutOfBoundsException => throw new Throwable("Empty wallet addresses")
      }
    }
    val networkPrefix = nodeWalletAddress.apply(0) match {
      case '3' => 16.toByte // Test net
      case '9' => 0.toByte // Main net
    }
    this._address = JavaHelpers.createP2PKAddress(pk, networkPrefix).toString()
  }

  /**
   * Create mnemonic
   */
  def create(): Unit = {
    _value = generate(Entropy256, WordList.load(EnglishWordList).get, new SecureRandom())
  }

  /**
   * Read mnemonic from the file
   *
   * @param key [[String]] encryption key of content
   * @return
   */
  def read(key: String): Boolean = {
    if (!isFileExists) return false
    val line = readFile()

    try {
      val sequence = new Encryption(key).decrypt(line)
      if (check(sequence, WordList.load(EnglishWordList).get)) {
        _value = sequence
        true
      }
      else false
    }
    catch {
      case _: BadPaddingException => false
    }
  }

  /**
   * Check if mnemonic file exists
   *
   * @return
   */
  def isFileExists: Boolean = Files.exists(Paths.get(filename))

  /**
   * Save mnemonic to the file with specified encryption key
   * @param key [[String]] encryption key of content
   * @return
   */
  def save(key: String): Boolean = {
    if (isFileExists)
      false
    else {
      createFile(new Encryption(key).encrypt(_value))
      true
    }
  }
}
