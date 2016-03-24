package improbable.behaviours.internet.commands

import improbable.behaviours.internet.RoutingTableEntry
import improbable.papi._
import improbable.papi.world.messaging.CustomMsg
import improbable.serialization.KryoSerializable

sealed trait RoutingCommand extends KryoSerializable

case class Destroy() extends RoutingCommand

case class ScheduleUpdate(msg: RoutingCommand, msDelay: Int = 200) extends RoutingCommand

case class ForwardTo(entityId: EntityId, command: IncomingCommand) extends RoutingCommand

case class RoutingTableUpdated() extends CustomMsg with RoutingCommand


import improbable.papi.world.messaging.CustomMsg

sealed trait IncomingCommand extends CustomMsg

case class RegisterRoute(routeId: EntityId, destination: EntityId) extends IncomingCommand

case class PlayerRequestDestory() extends IncomingCommand

case class DeregisterRoute(dest: EntityId) extends IncomingCommand

case class RegisterRouteTableUpdate(source: EntityId, routingTable: Iterable[RoutingTableEntry], msg: String) extends IncomingCommand

case class RoutesLost(destination: Set[EntityId], source: EntityId) extends IncomingCommand

case class SendRoutingTables(msg: String) extends IncomingCommand with RoutingCommand


