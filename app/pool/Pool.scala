package pool
import io.circe.Json

class Pool(packetBodyMethod: Json => String) extends PoolConfig with PoolQueue {
  override lazy val connection: String = readKey("pool.connection")
  override var maxChunkSize: Short = _
  override lazy val poolServerValidationRoute: String = readKey("pool.route.share")
  override def packetBody(json: Json): String = packetBodyMethod(json)
}
