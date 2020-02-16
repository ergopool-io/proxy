package proxy

import java.io.{File, PrintWriter}
import java.nio.file.{Files, Paths}
import java.security.SecureRandom

import com.github.alanverbner.bip39._
import helpers.Encryption
import javax.crypto.BadPaddingException
import org.ergoplatform.P2PKAddress
import org.ergoplatform.appkit._
import proxy.loggers.Logger
import proxy.node.Node

import scala.io.Source

object Mnemonic {
  private val filename: String = Config.mnemonicFilename
  private[this] var _value: String = _
  private var _address: P2PKAddress = _

  /**
   * The mnemonic value
   *
   * @return mnemonic
   */
  def value: String = this._value

  private def setValue(value: String): Unit = {
    _value = value
  }

  /**
   * Address creating using mnemonic
   *
   * @return address
   */
  def address: P2PKAddress = _address

  /**
   * Create address using mnemonic value
   */
  def createAddress(): Unit = {
    val secretKey = JavaHelpers.seedToMasterKey(SecretString.create(this._value))
    val pk = secretKey.key.publicImage
    val nodeWalletAddress = {
      try {
        Node.walletAddresses.apply(0)
      } catch {
        case _: IndexOutOfBoundsException => throw new Throwable("Empty wallet addresses")
      }
    }
    nodeWalletAddress.apply(0) match {
      case '3' => Config.networkType = NetworkType.TESTNET
      case '9' => Config.networkType = NetworkType.MAINNET
    }
    this._address = JavaHelpers.createP2PKAddress(pk, Config.networkType.networkPrefix)
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
        Logger.error(sequence)
        _value = sequence
        true
      }
      else false
    }
    catch {
      case _: BadPaddingException => false
    }
  }

  private def readFile(): String = {
    val f = Source.fromFile(filename)
    val line = f.getLines().mkString
    f.close()

    line
  }

  /**
   * Check if mnemonic file exists
   *
   * @return
   */
  def isFileExists: Boolean = Files.exists(Paths.get(filename))

  /**
   * Save mnemonic to the file with specified encryption key
   *
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

  private def createFile(value: String): Unit = {
    val printWriter = new PrintWriter(new File(filename).getCanonicalFile)
    printWriter.write(value)
    printWriter.close()
  }

  private def reload(): Unit = {
    _value = null
    _address = null
  }
}
