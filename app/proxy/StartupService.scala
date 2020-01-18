package proxy

import javax.inject.Singleton
import proxy.node.Node

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class StartupService {
  Config.loadPoolConfig()

  Future {
    while (Node.pk == "") Thread.sleep(500)
    Node.createProtectionScript()
    Node.fetchUnspentBoxes()
  }
}
