package proxy

import javax.inject.Singleton
import proxy.loggers.Logger
import proxy.node.Node
import proxy.status.ProxyStatus.MiningDisabledException

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class StartupService {
  Config.loadPoolConfig()

  if (Mnemonic.isFileExists)
    new MiningDisabledException("Load mnemonic to continue")
  else {
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
    while (Node.pk == "") Thread.sleep(500)
    Node.createProtectionScript()
    Node.fetchUnspentBoxes()
  }
}
