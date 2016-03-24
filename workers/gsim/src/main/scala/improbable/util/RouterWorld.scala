package improbable.util

import improbable.behaviours.internet.Router
import improbable.behaviours.internet.commands._
import improbable.papi.EntityId

import scala.collection.mutable

class RouterWorld(debug: Boolean = false) {

  var networks = Map.empty[EntityId, Router]
  var routes = 20000

  var time = 0

  var scheduled = new mutable.PriorityQueue[(Int, EntityId, RoutingCommand)]()(Ordering.by(-_._1))

  private def interpretCommand(networkId: EntityId, command: RoutingCommand): Int = {
    networks.get(networkId) match {
      case None =>
        println(s"unknown network $networkId")
        0
      case Some(network) =>
        command match {
          case Destroy() =>
            networks -= networkId
            network.routes.foreach(r =>
              scheduleIncoming(r.destination, DeregisterRoute(r.destination))
            )
            0
          case ScheduleUpdate(msg, delay) =>
            schedule(networkId, msg, delay)
            0
          case ForwardTo(id, msg) =>
            interpretIncoming(id, msg)
          case SendRoutingTables(msg) if network.reschedule =>
            interpretIncoming(networkId, SendRoutingTables(msg), false)
            0
          case SendRoutingTables(msg) =>
            interpretIncoming(networkId, SendRoutingTables(msg))
          case RoutingTableUpdated() =>
            0
        }
    }
  }

  private def interpretIncoming(networkId: EntityId, command: IncomingCommand, debug: Boolean = this.debug): Int = {
    networks.get(networkId) match {
      case None =>
        println(s"unknown network $networkId")
        0
      case Some(network) =>
        if (debug) {
          println(s"$time: $networkId <- $command")
        }
        time += 1
        network.interpret(command).map(interpretCommand(networkId, _)).sum + 1
    }
  }


  def run(msg: String = ""): (Int, Long) = {
    var totalTime = 0l
    if (debug && msg.nonEmpty) {
      println(msg)
    }
    var total = 0
    while (scheduled.nonEmpty) {
      val (t, id, command) = scheduled.dequeue()
      time = t.max(time)
      val t0 = System.nanoTime()
      total += interpretCommand(id, command)
      val t1 = System.nanoTime()
      totalTime += (t1 - t0)

    }
    (total, totalTime / 1000000)
  }

  def schedule(id: EntityId, msg: RoutingCommand, delay: Int = 0): Unit = {
    scheduled += ((time + delay, id, msg))
  }

  def scheduleIncoming(id: EntityId, msg: IncomingCommand, delay: Int = 0): Unit = {
    scheduled += ((time + delay, id, ForwardTo(id, msg)))
  }

  def link(n1: EntityId, n2: EntityId, delay: Int = 0): Unit = {
    addNetwork(n1)
    addNetwork(n2)

    scheduleIncoming(n1, RegisterRoute(routes, n2), delay)
    scheduleIncoming(n2, RegisterRoute(routes, n1), delay)
    routes += 1
  }

  private def addNetwork(n1: EntityId): Unit = {
    if (!networks.contains(n1)) {
      networks = networks.updated(n1, Router(n1))
    }
  }

  def unlink(n1: EntityId, n2: EntityId, delay: Int = 0): Unit = {
    scheduleIncoming(n1, DeregisterRoute(n2), delay)
    scheduleIncoming(n2, DeregisterRoute(n1), delay)
  }

  def getRoute(source: EntityId, dest: EntityId): List[EntityId] = {
    if (source == dest) {
      List(source)
    } else {
      val next = for {
        network <- networks.get(source)
        routingEntry <- network.storedRoutingTable.get(dest)
      } yield {
          routingEntry.nextNetworkId
        }
      next.map(n => source +: getRoute(n, dest)).getOrElse(Nil)
    }
  }
}
