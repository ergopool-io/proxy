package routers

import controllers.ProxyController
import javax.inject.Inject
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._

/**
 * Router for proxy
 * 
 * @constructor create new router
 * @param controller controller to route requests to it
 */ 
class ProxyRouter @Inject()(controller: ProxyController) extends SimpleRouter {

  override def routes: Routes = {
    case POST(p"/mining/solution") =>
      controller.sendSolution()

    case POST(p"/mining/share") =>
      controller.sendShare()

    case GET(p"/mining/candidate") =>
      controller.getMiningCandidate()

    case GET(p"/info") =>
      controller.changeInfo()

    case GET(p"/api-docs/swagger.conf") =>
      controller.changeSwagger()

    case POST(p"/proxy/config/reload") =>
      controller.reloadConfig()

    case POST(p"/proxy/status/reset") =>
      controller.resetStatus()

    case GET(p"/proxy/test") =>
      controller.test()

    case POST(p"/proxy/mnemonic/load") =>
      controller.loadMnemonic()

    case POST(p"/proxy/mnemonic/save") =>
      controller.saveMnemonic()

    case GET(p"/$path*") =>
      controller.proxyPass()

    case POST(p"/$path*") =>
      controller.proxyPass()
  }
}
