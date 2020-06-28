package proxy

import java.io.{File, PrintWriter}
import java.nio.file.{Files, Paths}
import java.security.SecureRandom

import com.github.alanverbner.bip39._
import helpers.Encryption
import javax.crypto.BadPaddingException
import loggers.Logger
import org.ergoplatform.P2PKAddress
import org.ergoplatform.appkit._

import scala.io.Source

class Mnemonic(networkType: NetworkType, filename: String, secret: String) {
  private var _value: String = _
  private var _address: P2PKAddress = _

  /**
   * The mnemonic value
   *
   * @return mnemonic
   */
  def value: String = this._value

  /**
   * Address creating using mnemonic
   *
   * @return address
   */
  def address: P2PKAddress = _address

  /**
   * Create address using mnemonic value
   */
  @throws(classOf[Exception])
  def createAddress(): Unit = {
    val secretKey = JavaHelpers.seedToMasterKey(SecretString.create(value))
    val pk = secretKey.key.publicImage
    this._address = JavaHelpers.createP2PKAddress(pk, networkType.networkPrefix)
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
  @throws(classOf[WrongPassword])
  @throws(classOf[FileDoesNotExists])
  def read(key: String): Unit = {
    if (!isFileExists) throw new FileDoesNotExists
    val line = readFile()

    try {
      val sequence = new Encryption(key, secret).decrypt(line)
      if (check(sequence, WordList.load(EnglishWordList).get)) {
        _value = sequence
      }
      else throw new WrongPassword
    }
    catch {
      case _: BadPaddingException =>
        Logger.debug("Mnemonic key was wrong!")
        throw new WrongPassword
    }
  }

  // $COVERAGE-OFF$
  /**
   * Read mnemonic file
   *
   * @return line of the mnemonic file
   */
  def readFile(): String = {
    val f = Source.fromFile(filename)
    val line = f.getLines().mkString
    f.close()

    line
  }
  // $COVERAGE-ON$

  /**
   * Check if mnemonic file exists
   *
   * @return
   */
  def isFileExists: Boolean = Files.exists(Paths.get(filename))

  // $COVERAGE-OFF$
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
      createFile(new Encryption(key, secret).encrypt(_value))
      true
    }
  }
  // $COVERAGE-ON$

  /**
   * Create mnemonic file
   * @param value the value to put in the file
   */
  def createFile(value: String): Unit = {
    val printWriter = new PrintWriter(new File(filename).getCanonicalFile)
    printWriter.write(value)
    printWriter.close()
  }

  final class FileDoesNotExists extends Throwable("Mnemonic file does not exists!")
  final class WrongPassword extends Throwable("Can not read mnemonic with this password! Set the right one or remove mnemonic file")
}
