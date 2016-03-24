package improbable.behaviours.internet

import improbable.behaviours.internet.commands._
import improbable.corelib.visual.VisualControl
import improbable.internet._
import improbable.papi._
import improbable.papi.entity.{Entity, EntityBehaviour}
import improbable.papi.world.World
import improbable.papi.world.messaging.CustomMsg


case class NetFlowUpdate(routeId: EntityId, flows: Map[EntityId, Float]) extends CustomMsg

import scala.concurrent.duration._

case class GenerationRateUpdate(flows: Map[EntityId, Float]) extends CustomMsg

class NetworkFlowBehaviour(world: World, entity: Entity, networkFlows: NetworkFlowsWriter, router: RouterInterface) extends EntityBehaviour {

  var pending = false

  override def onReady(): Unit = {
    world.messaging.onReceive {
      case NetFlowUpdate(routeId: EntityId, flows: Map[EntityId, Float]) =>
        val onwardFlows = NetworkFlow(flows)
        // store our updated state
        networkFlows.update
          .flows(networkFlows.flows.updated(routeId, onwardFlows))
          .finishAndSend()
      case GenerationRateUpdate(flows: Map[EntityId, Float]) =>
        networkFlows.update
          .generationRate(flows)
          .finishAndSend()
      case Update() =>
        world.timing.after(100.millis) {
          pending = false
          updateRouteFlows()
        }
      case RoutingTableUpdated() => scheduleRouteFlows()
    }

    entity.watch[NetworkFlows].bind.flows {
      _ => scheduleRouteFlows()
    }

    entity.watch[NetworkFlows].bind.generationRate {
      _ => scheduleRouteFlows()
    }
  }

  def scheduleRouteFlows(): Unit = {
    if (!pending) {
      pending = true
      world.messaging.sendToEntity(entity.entityId, Update())
    }
  }

  def updateRouteFlows(): Unit = {
    var flow = Map.empty[EntityId, Float]
    var sunkFlow: Boolean = false
    var failedFlows: Float = 0.0f
    networkFlows.flows.values.foreach(routesFlow => {
      routesFlow.destinationVolumes.foreach {
        case (destination, volume) =>
          if (destination == entity.entityId && volume > 0) {
            sunkFlow = true
          } else {
            val current = flow.getOrElse(destination, 0f)
            flow = flow.updated(destination, current + volume)
          }
      }
    })
    // add in locally generated flows
    val generationRate = networkFlows.generationRate
    generationRate.foreach {
      case (destination: EntityId, volume: Float) =>
        val current = flow.getOrElse(destination, 0f)
        flow = flow.updated(destination, current + volume)
    }
    // flow is now the total traffic
    val routeTable = router.routingTable()
    var routeFlows = Map.empty[EntityId, Map[EntityId, Float]]
    flow.filterKeys(_ != entity.entityId).filter(_._2 > 0.0f).foreach(destinationFlow => {
      routeTable.get(destinationFlow._1) match {
        case None =>
          failedFlows += destinationFlow._2; // never been able to route
        case Some(entry: RoutingTableEntry) =>
          var current = routeFlows.getOrElse(entry.nextNetworkId, Map.empty[EntityId, Float])
          val currentDest = current.getOrElse(destinationFlow._1, 0f)
          current = current.updated(destinationFlow._1, currentDest + destinationFlow._2)
          routeFlows = routeFlows.updated(entry.nextNetworkId, current)
      }
    })
    // now we need to send messages to all outgoing routes
    val outgoingRoutes = router.routes()
    outgoingRoutes.foreach { info =>
      val thisRoutesFlows = routeFlows.getOrElse(info.destination, Map.empty)
      world.messaging.sendToEntity(info.routeId, NetFlowUpdate(entity.entityId, thisRoutesFlows))
    }
    networkFlows.update.failedFlows(failedFlows).sunkFlows(sunkFlow).finishAndSend()
  }

  case class Update() extends CustomMsg

}

class RouteFlowBehaviour(world: World, entity: Entity, route: RouteWriter) extends EntityBehaviour {

  override def onReady(): Unit = {
    world.messaging.onReceive {
      case NetFlowUpdate(sourceNetwork: EntityId, flows: Map[EntityId, Float]) =>
        val capacity = route.capacity
        val volume = flows.values.sum
        //println(s" Route ${entity.entityId} received $volume")
        val throughput = volume / capacity
        val rescaled = if (throughput > 1) flows.mapValues(_ / throughput) else flows
        val fromSource = sourceNetwork == route.source
        if (fromSource) {
          world.messaging.sendToEntity(route.dest, NetFlowUpdate(entity.entityId, rescaled))
          route.update.utilisationUp(volume).finishAndSend()
        } else {
          assert(sourceNetwork == route.dest)
          world.messaging.sendToEntity(route.source, NetFlowUpdate(entity.entityId, rescaled))
          route.update.utilisationDown(volume).finishAndSend()
        }
    }
  }
}

class VisibleOnlyWhenFlowing(entity: Entity, visual: VisualControl) extends EntityBehaviour {

  entity.watch[improbable.internet.Route].bind {
    util =>
      val up = entity.watch[improbable.internet.Route].utilisationUp.getOrElse(0f)
      val down = entity.watch[improbable.internet.Route].utilisationDown.getOrElse(0f)
      visual.setVisual(up + down > 0)
  }
}

class NetworkBoolsBehaviour(world: World, entity: Entity, networkBools: NetworkBoolsWriter) extends EntityBehaviour {
  networkBools.update.id(entity.entityId).finishAndSend()

  entity.watch[NetworkFlows].bind.generationRate {
    rate => networkBools.update.generating(rate.values.exists(_ > 0)).finishAndSend()
  }
  entity.watch[NetworkFlows].bind.flows {
    flows => networkBools.update.
      routing(flows.values.exists(_.destinationVolumes.values.exists(_ > 0))).finishAndSend()
  }
  entity.watch[NetworkFlows].bind.failedFlows {
    failedFlows => networkBools.update.
      broken(failedFlows > 0.0f).finishAndSend()
  }
  entity.watch[NetworkFlows].bind.sunkFlows {
    sunkFlows => networkBools.update.
      sinking(sunkFlows).finishAndSend()
  }
}
