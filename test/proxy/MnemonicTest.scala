package proxy

import java.io.{File, PrintWriter}
import java.nio.file.{Files, Paths}

import org.ergoplatform.appkit.NetworkType
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito._
import org.scalatestplus.play.PlaySpec

class MnemonicTest extends PlaySpec with MockitoSugar with BeforeAndAfterEach {

  val mnemonicFileName = "testFile"
  override def beforeEach() {

  }

  override def afterEach() {
    new File(mnemonicFileName).delete()
  }

  "MnemonicTest" should {

    /**
     * Purpose: Throw exception when the mnemonic file does not exists.
     * Prerequisites: None.
     * Scenario: Mock mnemonic and return false on isFileExists.
     * Test Conditions:
     * * should throw FileDoesNotExists exception
     */
    "throw exception on reading mnemonic when file does not exists" in {
      val mnemonic = mock[Mnemonic](withSettings().useConstructor(NetworkType.TESTNET, mnemonicFileName, ""))
      when(mnemonic.isFileExists).thenReturn(false)

      when(mnemonic.read("test")).thenCallRealMethod()
      assertThrows[mnemonic.FileDoesNotExists](mnemonic.read("test"))
    }

    /**
     * Purpose: Throw exception when specified password is wrong.
     * Prerequisites: None.
     * Scenario: Mock mnemonic and return true on isFileExists and return an encrypted value on readFile.
     * Test Conditions:
     * * should throw WrongPassword exception
     */
    "throw exception when password is wrong" in {
      val mnemonic = mock[Mnemonic](withSettings().useConstructor(NetworkType.TESTNET, mnemonicFileName, ""))
      when(mnemonic.isFileExists).thenReturn(true)
      when(mnemonic.readFile()).thenReturn("VJn+1i1dWDqi/1cdmmLErsFmhxKaPe0Uye85E1QgW3K/mhEWGJlB3njNu+QyVxfg" +
        "TBRTcf/ErR2A6GiP5VRJ6nPGouPTnA0T8/eVgMAcLhbTU5rcQH7tQpZ/vO4seuFDTG5TAZbYJimRuBPMr3fq81aE8YZrBXxr/XB/X/NymZ4" +
        "rMlXKAQH7Jj1TIw5ShGineo8fafZcLjOqPFbT8IPizg==")

      when(mnemonic.read("wrong password")).thenCallRealMethod()
      assertThrows[mnemonic.WrongPassword](mnemonic.read("wrong password"))
    }

    /**
     * Purpose: Encrypted value is right.
     * Prerequisites: None.
     * Scenario: Mock mnemonic and return false on isFileExists and return an encrypted value on readFile.
     * Test Conditions:
     * * mnemonic value is correct
     */
    "set the value of object" in {
      val mnemonic = mock[Mnemonic](withSettings().useConstructor(NetworkType.TESTNET, mnemonicFileName, ""))
      when(mnemonic.isFileExists).thenReturn(true)
      when(mnemonic.readFile()).thenReturn("VJn+1i1dWDqi/1cdmmLErsFmhxKaPe0Uye85E1QgW3K/mhEWGJlB3njNu+QyVxfg" +
        "TBRTcf/ErR2A6GiP5VRJ6nPGouPTnA0T8/eVgMAcLhbTU5rcQH7tQpZ/vO4seuFDTG5TAZbYJimRuBPMr3fq81aE8YZrBXxr/XB/X/NymZ4" +
        "rMlXKAQH7Jj1TIw5ShGineo8fafZcLjOqPFbT8IPizg==")

      when(mnemonic.read("test")).thenCallRealMethod()
      mnemonic.read("test")
      when(mnemonic.value).thenCallRealMethod()
      mnemonic.value mustBe "census erode want novel panther lens head hire else south leaf situate hill tiger metal " +
        "maze amused short repeat also canvas skate outdoor video"
    }

    /**
     * Purpose: Create a mnemonic file.
     * Prerequisites: None.
     * Scenario: create a file for mnemonic
     * Test Conditions:
     * * mnemonic file has been created
     */
    "createFile" in {
      val mnemonic = mock[Mnemonic](withSettings().useConstructor(NetworkType.TESTNET, mnemonicFileName, ""))
      when(mnemonic.createFile("test")).thenCallRealMethod()

      mnemonic.createFile("test")
      Files.exists(Paths.get(mnemonicFileName)) mustBe true
    }

    /**
     * Purpose: Check if isFileExists returns true when there exists a mnemonic file.
     * Prerequisites: None.
     * Scenario: Create a file name like mnemonicFileName.
     * Test Conditions:
     * * isFileExists is true
     */
    "isFileExists" in {
      val mnemonic = mock[Mnemonic](withSettings().useConstructor(NetworkType.TESTNET, mnemonicFileName, ""))
      val printWriter = new PrintWriter(new File(mnemonicFileName).getCanonicalFile)
      printWriter.write("test")
      printWriter.close()
      when(mnemonic.isFileExists).thenCallRealMethod()

      mnemonic.isFileExists mustBe true
    }

    /**
     * Purpose: Create mnemonic value address.
     * Prerequisites: None.
     * Scenario: Mock mnemonic and return false on isFileExists.
     * Test Conditions:
     * * should throw WrongPassword exception
     */
    "createAddress" in {
      val mnemonic = mock[Mnemonic](withSettings().useConstructor(NetworkType.TESTNET, mnemonicFileName, ""))
      when(mnemonic.value).thenReturn("census erode want novel panther lens head hire else south leaf situate " +
        "hill tiger metal maze amused short repeat also canvas skate outdoor video")
      when(mnemonic.createAddress()).thenCallRealMethod()
      when(mnemonic.address).thenCallRealMethod()

      mnemonic.createAddress()
      mnemonic.address.toString() mustBe "3WyRt8MCd1XfZnWoPXdrngt9yb2BgdxRtM8RQz7btQSB32XF7sVf"
    }

  }
}
