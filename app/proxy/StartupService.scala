package proxy

import javax.inject.Singleton
import proxy.loggers.Logger
import proxy.node.Node
import proxy.status.{ProxyStatus, StatusType}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class StartupService {
  ProxyStatus.setStatus(StatusType.green, "Proxy")
  Config.loadPoolConfig()

  if (Mnemonic.isFileExists) {
    ProxyStatus.disableMining("Load mnemonic to continue", "Mnemonic")
  } else {
    Mnemonic.create()
    try {
      Mnemonic.createAddress()
    } catch {
      case e: Throwable =>
        Logger.error("Exception happened", e)
        System.exit(1)
    }
  }

  Future {
    try {
      while (Node.pk == "" || Mnemonic.address == null) Thread.sleep(500)
      Node.createProtectionScript()
      Node.fetchUnspentBoxes()
      Logger.debug(Config.lockAddress)
      if (ProxyStatus.category != "Config") ProxyStatus.reset()
    } catch {
      case e: Throwable =>
        Logger.error(e.toString, e)
    }
  }
}
