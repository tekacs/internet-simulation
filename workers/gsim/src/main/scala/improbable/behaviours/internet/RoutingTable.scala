package improbable.behaviours.internet

import improbable.papi._
import improbable.serialization.KryoSerializable

import scala.collection.immutable

case class RouteData(routeId: EntityId, destination: EntityId)

class RoutingTable {

  var underlying = immutable.LongMap.empty[RoutingTableEntry]

  def routes: Iterable[RoutingTableEntry] = underlying.values

  def remove(lostDestinations: Iterable[EntityId]) = underlying --= lostDestinations

  def destinations(destinations: Set[EntityId]): Iterable[RoutingTableEntry] = underlying.filterKeys(destinations.contains).values

  def addEntry(route: RoutingTableEntry): Unit = {
    underlying = underlying.updated(route.destination, route)
  }

  def get(destination: Long): Option[RoutingTableEntry] = {
    underlying.get(destination)
  }
}

class RoutingTableEntry(val destination: Int, val nextNetworkId: Int, val length: Short, representativeASPath: Int) extends KryoSerializable {
  def prepend(routeInfo: RouteData): RoutingTableEntry = {

    if (math.random < 1 / length) {
      new RoutingTableEntry(destination, routeInfo.destination.toInt, (length + 1).toShort, nextNetworkId)
    } else {
      new RoutingTableEntry(destination, routeInfo.destination.toInt, (length + 1).toShort, representativeASPath)
    }
  }

  def contains(node: EntityId): Boolean = representativeASPath == node.toInt || destination == node.toInt || nextNetworkId == node.toInt

}

object RoutingTableEntry {

  def apply(destination: EntityId): RoutingTableEntry = new RoutingTableEntry(destination.toInt, destination.toInt, 1, -1)
}
