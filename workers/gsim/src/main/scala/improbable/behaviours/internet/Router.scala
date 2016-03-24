package improbable.behaviours.internet

import com.typesafe.scalalogging.Logger
import improbable.behaviours.internet.commands._
import improbable.papi._
import org.slf4j.helpers.NOPLogger

import scala.collection.mutable

case class Router(entityId: EntityId, logger: Logger = Logger(NOPLogger.NOP_LOGGER)) {

  val storedRoutingTable = new RoutingTable
  var changedDestinations = mutable.Set.empty[EntityId]

  var pending = false
  var reschedule = false

  var routes = List.empty[RouteData]

  def interpret(cmd: IncomingCommand): List[RoutingCommand] = {
    cmd match {
      case PlayerRequestDestory() =>
        destroy()

      case RegisterRoute(routeId, destination) =>
        registerRoute(routeId, destination)

      case DeregisterRoute(to) =>
        deregisterRoute(to)

      case RoutesLost(destinations, source) =>
        routesLost(destinations, source)

      case RegisterRouteTableUpdate(sourceId, routingTable, msg) =>
        routingTableUpdateReceived(sourceId, routingTable, msg)

      case SendRoutingTables(msg) =>
        if (reschedule) {
          reschedule = false
          List(ScheduleUpdate(SendRoutingTables("rescheduled")))
        } else {
          sendRoutingTables(msg)
        }
    }
  }

  def routingTableUpdateReceived(sourceId: EntityId, routingTable: Iterable[RoutingTableEntry], msg: String): List[RoutingCommand] = {

    routes.find(_.destination == sourceId) match {
      case None => Nil
      case Some(myRouteInfo) =>
        routingTable.flatMap {
          case entry if entry.contains(entityId) =>
            List.empty[RoutingCommand]
          case entry if entry.destination.toLong == entityId =>  Nil
          case entry =>
            val destination = entry.destination
            // don't care about routes to me
            val distance = entry.length + 1 // new distance is next hops distance + 1
            storedRoutingTable.get(destination) match {
              case None => // we don't have a route, so just take this one
                addRouteToDestination(myRouteInfo, entry, "New route")
              case Some(currentRoute) =>
                if (currentRoute.length > distance) {
                  // this is 'better', so just accept it
                  addRouteToDestination(myRouteInfo, entry, "route got better")
                } else if (currentRoute.nextNetworkId.toLong == myRouteInfo.destination && (currentRoute.length < distance)) {
                  // this was the route we were taking, and it just got longer
                  addRouteToDestination(myRouteInfo, entry, "route got longer")
                } else {
                  // this route is no better, so we just drop it
                  Nil
                }
            }
        }.toList
    }
  }

  def addRouteToDestination(myRouteInfo: RouteData, entry: RoutingTableEntry, msg: String): List[RoutingCommand] = {
    entry.prepend(myRouteInfo)
    val newEntry = entry.prepend(myRouteInfo)
    storedRoutingTable.addEntry(newEntry)
    changedDestinations += entry.destination
    scheduleRoutingTableSend(msg) :+ RoutingTableUpdated()
  }

  def routesLost(destinations: Set[EntityId], source: EntityId): List[RoutingCommand] = {
    logger.trace(s" $entityId lost route to $destinations via $source")
    val routesWeWereUsing = storedRoutingTable.destinations(destinations)
    val (routesWeLost, routesWeCanOffer) = routesWeWereUsing.partition(_.nextNetworkId == source)

    val forwardLostCommands = if (routesWeLost.nonEmpty) {
      val lostDestinations = routesWeLost.map(_.destination.toLong).toSet
      storedRoutingTable.remove(lostDestinations)
      changedDestinations --= lostDestinations
      routes.filter(_.destination != source).map {
        incoming =>
          ForwardTo(incoming.destination, RoutesLost(lostDestinations, entityId))
      } :+ RoutingTableUpdated()
    } else {
      Nil
    }

    val replacementCommands = if (routesWeCanOffer.nonEmpty) {
      if (routesWeLost.isEmpty) {
        //send right away, we can satisfy all routes
        List(ForwardTo(source, RegisterRouteTableUpdate(entityId, routesWeCanOffer.toSeq, "full replacement")))
      } else {
        List(ScheduleUpdate(ForwardTo(source, RegisterRouteTableUpdate(entityId, routesWeCanOffer.toSeq, "partial replacement"))))
      }
    } else {
      Nil
    }
    forwardLostCommands ++ replacementCommands
  }

  def deregisterRoute(to: EntityId): List[RoutingCommand] = {
    logger.info(s"deregistering:route to $to at $entityId")
    val lostRoutes = storedRoutingTable.routes.filter { case route => route.nextNetworkId.toLong == to }
    storedRoutingTable.remove(lostRoutes.map(_.destination.toLong))
    changedDestinations --= lostRoutes.map(_.destination.toLong)
    routes = routes.filter(_.destination != to)
    if (lostRoutes.nonEmpty) {
      val lostDestinations: Set[EntityId] = lostRoutes.map(_.destination.toLong).toSet
      routes.filter(_.destination != to).map { incoming =>
        ForwardTo(incoming.destination, RoutesLost(lostDestinations, entityId))
      }
    } else {
      Nil
    } :+ RoutingTableUpdated()
  }

  def registerRoute(routeId: EntityId, destination: EntityId): List[RoutingCommand] = {
    routes = RouteData(routeId, destination) +: routes
    val newEntry = RoutingTableEntry(destination)
    storedRoutingTable.addEntry(newEntry)
    changedDestinations += destination
    reschedule = true
    ForwardTo(destination, RegisterRouteTableUpdate(entityId, storedRoutingTable.routes, "incoming")) +: RoutingTableUpdated() +: scheduleRoutingTableSend("new route")
  }

  def destroy(): List[RoutingCommand] = {
    List(Destroy())
  }

  def scheduleRoutingTableSend(msg: String): List[RoutingCommand] = {
    if (!pending) {
      pending = true
      List(ScheduleUpdate(SendRoutingTables(msg)))
    } else {
      reschedule = true
      Nil
    }
  }

  def sendRoutingTables(msg: String): List[RoutingCommand] = {
    assert(pending, s"$msg $entityId")
    pending = false
    reschedule = false
    if (changedDestinations.nonEmpty) {
      val filteredTable = changedDestinations.toSeq.flatMap(c => storedRoutingTable.get(c)) // storedRoutingTable.filter(c => changedDestinations.contains(c._1))
      val commands = routes.flatMap(routeInfo => {
          val withoutDest = filteredTable.filter(_.nextNetworkId != routeInfo.destination)
          if (withoutDest.nonEmpty) {
            Some(ForwardTo(routeInfo.destination, RegisterRouteTableUpdate(entityId, withoutDest, msg)))
          } else {
            None
          }
        })
      changedDestinations = mutable.Set.empty[EntityId]
      commands
    } else {
      Nil
    }
  }
}
