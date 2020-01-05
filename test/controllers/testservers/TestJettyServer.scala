package controllers.testservers

import org.eclipse.jetty.server.{NetworkConnector, Server}
import org.eclipse.jetty.servlet.ServletHandler

trait TestJettyServer {
  val serverPort: Int
  val serverName: String

  protected val server: Server

  protected val handler: ServletHandler

  def port(): Int = {
    val conn = server.getConnectors()(0).asInstanceOf[NetworkConnector]
    conn.getPort
  }

  def startServer(): Unit = {
    println(s"$serverName started on ${port()}")
    server.start()
  }

  def stopServer(): Unit = {
    println(s"$serverName stopped")
    server.stop()
  }

  def createServer() = new Server(serverPort)
}
