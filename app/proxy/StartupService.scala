package proxy

import javax.inject.{Inject, Singleton}
import loggers.Logger
import node.{NodeClient, NodeClientError}
import play.api.{Application, Mode}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.Breaks._
import scala.util.control.ControlThrowable

@Singleton
class StartupService @Inject()(application: Application,
                               proxy: Proxy,
                               nodeClient: NodeClient) {
  if (application.mode != Mode.Test) {

    if (proxy.mnemonic.isFileExists)
      proxy.status.mnemonic.setUnhealthy()
    else {
      proxy.mnemonic.create()
      try {
        proxy.mnemonic.createAddress()
      } catch {
        case e: Throwable =>
          Logger.error("Exception happened", e)
          System.exit(1)
      }
      Future {
        breakable {
          while (true) {
            if(!proxy.mnemonic.isFileExists)
            {
              Logger.error(s"""Proxy error: Mnemonic file does not exists, please save your mnemonic file!""".stripMargin)
              Thread.sleep(5000)
            }
            else
            {
              Logger.info("Proxy info: Mnemonic saved!")
              break
            }
          }
        }
      }
    }

    Future {
      breakable {
        while (true) {
          try {
            if (proxy.reloadPoolQueueConfig()) {
              proxy.status.config.setHealthy()
              break
            }
          }
          catch {
            case break: ControlThrowable => throw break
            case error: Throwable =>
              Logger.error(s"Pool: ${error.getMessage}")
              proxy.status.config.setUnhealthy(error.getMessage)
              Thread.sleep(5000)
          }
        }
      }
    }

    val loadBoxesAndBlocks = proxy.getLoadBoxesAndBlocksMethod
    Future {
      var numTried = 0
      while (true) {
        try {
          var check = false
          Future {
            Thread.sleep(1000)
            if (!check) proxy.status.activeSyncing.setUnhealthy()
          }

          loadBoxesAndBlocks(numTried == 1000)

          check = true
          proxy.status.activeSyncing.setHealthy()
          proxy.status.nodeError.setHealthy()

          numTried = (numTried + 1) % 1001
        }
        catch {
          case error: NodeClientError =>
            proxy.status.nodeError.setUnhealthy(error.message)
            Logger.error("Node error in loadBoxesAndBlocks:", error)
          case throwable: Throwable =>
            Logger.error("Error in loadBoxesAndBlocks:", throwable)
        }

        Thread.sleep(1000)
      }
    }
  }
}