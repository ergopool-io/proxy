package proxy

import javax.inject.Singleton
import proxy.node.Node
import proxy.status.ProxyStatus.MiningDisabledException

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class StartupService {
  Config.loadPoolConfig()

  if (Mnemonic.isFileExists)
    new MiningDisabledException("Load mnemonic to continue")
  else
    Mnemonic.create()

  Future {
    while (Node.pk == "") Thread.sleep(500)
    Node.createProtectionScript()
    Node.fetchUnspentBoxes()
  }
}
