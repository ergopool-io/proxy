package proxy

import javax.inject.Singleton

@Singleton
class StartupService {
  Config.loadPoolConfig()
}
