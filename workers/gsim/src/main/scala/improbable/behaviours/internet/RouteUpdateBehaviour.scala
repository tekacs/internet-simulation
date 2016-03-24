package improbable.behaviours.internet

import com.typesafe.scalalogging.Logger
import improbable.apps._
import improbable.behaviours.internet.commands._
import improbable.papi.entity.behaviour.EntityBehaviourInterface
import improbable.papi.entity.{Entity, EntityBehaviour}
import improbable.papi.world.World
import improbable.papi.worldapp.WorldAppDescriptor

import scala.concurrent.duration._

trait RouterInterface extends EntityBehaviourInterface {

  def routingTable(): RoutingTable

  def routes(): Iterable[RouteData]
}

class RouteUpdateBehaviour(world: World, entity: Entity, logger: Logger) extends EntityBehaviour with RouterInterface {

  val updater = new Router(entity.entityId, logger)

  override def routingTable(): RoutingTable = updater.storedRoutingTable

  override def routes(): Iterable[RouteData] = updater.routes

  override def onReady(): Unit = {
    world.messaging.onReceive {
      case msg: IncomingCommand =>
        updater.interpret(msg).foreach(interpret)
    }
  }

  def interpret(command: RoutingCommand): Unit = {
    command match {
      case RoutingTableUpdated() =>
        world.messaging.sendToEntity(entity.entityId, RoutingTableUpdated())
      case Destroy() =>
        updater.routes.foreach(r => world.messaging.sendToEntity(r.routeId, PlayerRequestDestory()))
        entity.destroy()
      case ScheduleUpdate(msg, delay) =>
        world.timing.after(delay.millis) {
          interpret(msg)
        }
      case ForwardTo(id, msg) => world.messaging.sendToEntity(id, msg)
      case SendRoutingTables(msg) =>
        updater.interpret(SendRoutingTables(msg)).foreach(interpret)
    }
  }

  world.timing.every(1.minute) {
    if (routes().nonEmpty && routingTable().underlying.nonEmpty) {
      val maxLength = routingTable().routes.maxBy(_.length)
      val averageLength = routingTable().routes.map(_.length.toDouble).sum / routingTable().routes.size
      val stats = NetworkStats(
        MaxDistance(from = entity.entityId, to = maxLength.destination, length = maxLength.length)
        , NumberOfRoutes(entity.entityId, routes().size)
        , AverageLengthOfRoute(entity.entityId, averageLength)
      )
      world.messaging.sendToApp(WorldAppDescriptor.forClass[StatsApp].name, stats)
    }
  }
}
