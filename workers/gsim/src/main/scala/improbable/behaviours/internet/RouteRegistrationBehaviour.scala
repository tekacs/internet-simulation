package improbable.behaviours.internet

import improbable.Cancellable
import improbable.behaviours.internet.commands._
import improbable.papi.entity.{Entity, EntityBehaviour}
import improbable.papi.world.World

class RouteRegistrationBehaviour(world: World, entity: Entity) extends EntityBehaviour {

  override def onReady(): Unit = {
    val sourceId = entity.watch[improbable.internet.Route].source.get
    val destId = entity.watch[improbable.internet.Route].dest.get

    world.messaging.sendToEntity(sourceId, RegisterRoute(entity.entityId, destId))
    world.messaging.sendToEntity(destId, RegisterRoute(entity.entityId, sourceId))

    world.messaging.onReceive {
      case PlayerRequestDestory() =>
        world.messaging.sendToEntity(sourceId, DeregisterRoute(destId))
        world.messaging.sendToEntity(destId, DeregisterRoute(sourceId))
        world.messaging.sendToEntity(destId, NetFlowUpdate(entity.entityId, Map.empty))

        entity.destroy()
    }

    var over = false
    var disconnect = Cancellable()
    var connect = Cancellable()

    //    entity.watch[Route].bind { _ =>
    //      val up = entity.watch[Route].utilisationUp.get
    //      val down = entity.watch[Route].utilisationDown.get
    //      val capacity = entity.watch[Route].capacity.get
    //
    //      if (up + down > capacity) {
    //        connect.cancel()
    //        if (!over) {
    //          disconnect = world.timing.after(5.seconds) {
    //            world.messaging.sendToEntity(sourceId, DeregisterRoute(destId))
    //            world.messaging.sendToEntity(destId, DeregisterRoute(sourceId))
    //            world.messaging.sendToEntity(destId, NetFlowUpdate(entity.entityId, Map.empty))
    //            world.messaging.sendToEntity(entity.entityId, NetFlowUpdate(sourceId, Map.empty))
    //            world.messaging.sendToEntity(entity.entityId, NetFlowUpdate(destId, Map.empty))
    //          }
    //        }
    //        over = true
    //      } else {
    //        disconnect.cancel()
    //
    //        if (over) {
    //          connect = world.timing.after(5.seconds) {
    //            world.messaging.sendToEntity(sourceId, RegisterRoute(entity.entityId, destId))
    //            world.messaging.sendToEntity(destId, RegisterRoute(entity.entityId, sourceId))
    //          }
    //        }
    //        over = false
    //      }
    //
    //    }
    //  }
  }
}
