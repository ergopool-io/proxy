package proxy

import node.NodeClient
import play.api.mvc.{RawBuffer, Request}
import scalaj.http.HttpResponse

trait LowerLayerNodeInterface {
  val client: NodeClient
  type Response = HttpResponse[Array[Byte]]

  lazy val nodeConnection: String = client.connection

  /**
   * Pass a request directly to the node
   *
   * @param request request to pass
   * @return response from the node
   */
  def sendRequestToNode(request: Request[RawBuffer]): Response = {
    client.sendRequest(request.uri, request)
  }

  /**
   * Send a solution directly to the node
   *
   * @param request the solution to send
   * @return response from the node
   */
  def sendSolutionToNode(request: Request[RawBuffer]): Response = {
    client.sendSolution(request)
  }

  /**
   * Get node info
   * @return response from the node
   */
  def nodeInfo: Response = {
    client.info
  }

  /**
   * Parse the node error response and get the message
   *
   * @param response the error response from the node
   * @return error message
   */
  def parseError(response: Response): String = {
    client.parseErrorResponse(response)
  }
}
