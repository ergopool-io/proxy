package helpers

import com.google.inject.AbstractModule
import proxy.StartupService

class EagerLoaderModule extends AbstractModule{
  override def configure(): Unit = {
    bind(classOf[StartupService]).asEagerSingleton()
  }
}
