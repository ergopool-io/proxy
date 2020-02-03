package proxy

import java.io.{File, PrintWriter}
import java.nio.file.{Files, Paths}
import java.security.SecureRandom

import com.github.alanverbner.bip39.{EnglishWordList, Entropy128, WordList, check, generate}
import helpers.Encryption
import javax.crypto.BadPaddingException

import scala.io.Source

object Mnemonic {
  private var _value: String = _
  private val filename: String = Config.mnemonicFilename

  /**
   * The mnemonic value
   * @return
   */
  def value: String = this._value

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
  }

  /**
   * Create mnemonic
   */
  def create(): Unit = {
    _value = generate(Entropy128, WordList.load(EnglishWordList).get, new SecureRandom())
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
