package improbable.behaviours.player.controls

import improbable.apps.{BuildNetwork, NetworkSpawner}
import improbable.behaviours.internet.GenerationRateUpdate
import improbable.behaviours.internet.commands.PlayerRequestDestory
import improbable.corelib.util.EntityOwnerDelegation._
import improbable.entity.physical.{Position, PositionController}
import improbable.internet.NetworkFlows
import improbable.math.Coordinates
import improbable.natures.{NetworkNature, RouteNature}
import improbable.papi.entity.{Entity, EntityBehaviour, EntitySnapshot}
import improbable.papi.world.World
import improbable.papi.worldapp.WorldAppDescriptor
import improbable.player.controls.{CreateNetwork, PlayerControlsState}

class PlayerBehaviour(entity: Entity, world: World,
                      position: PositionController
                       ) extends EntityBehaviour {

  entity.delegateStateToOwner[Position]

  entity.watch[PlayerControlsState].onKillNetwork {
    pos =>
      world.entities.find(pos.position, 50).filterNot(_.entityId == entity.entityId).sortBy(_.position.distanceTo(pos.position)).headOption.foreach {
        snapshot =>
          println(s"\nkilling ${snapshot.entityId}")
          world.messaging.sendToEntity(snapshot.entityId, PlayerRequestDestory())
      }
  }

  entity.watch[PlayerControlsState].onBuildTheInternet {
    internetType =>
      world.messaging.sendToApp(WorldAppDescriptor.forClass[NetworkSpawner].name, BuildNetwork(entity.position))

  }

  entity.watch[PlayerControlsState].onCreateRoute {
    positions =>
      if (positions.pos1.distanceTo(positions.pos2) > 5) {
        for {
          n1 <- nearestNetwork(positions.pos1)
          n2 <- nearestNetwork(positions.pos2)
          if n1.entityId != n2.entityId
        } {
          world.entities.spawnEntity(RouteNature(world, n1.entityId, n1.position, n2.entityId, n2.position, 300))
          println(s"Creating routes between ${n1.entityId} and ${n2.entityId}")
        }
      } else {
        println(s"Creating network at ${positions.pos1}")
        if (positions.pos1.distanceTo(Coordinates.zero) < 4000) {
          world.entities.spawnEntity(NetworkNature(positions.pos1, Map.empty, 18.0f))
        }
      }
  }

  entity.watch[PlayerControlsState].onCreateFlow {
    positions =>
      for {
        n1 <- nearestNetwork(positions.pos1)
        n2 <- nearestNetwork(positions.pos2)
        if n1.entityId != n2.entityId
        flows <- n1.get[NetworkFlows]
      } {
        println(s"Generating flows route from ${n1.entityId} and ${n2.entityId}")
        val newFlows = flows.generationRate.updated(n2.entityId, 10f)
        world.messaging.sendToEntity(n1.entityId, GenerationRateUpdate(newFlows))
      }
  }

  def nearestNetwork(position: Coordinates): Option[EntitySnapshot] = {
    world.entities.find(position, 200, Set("Network")).sortBy(_.position.distanceTo(position)).headOption
  }

  entity.watch[PlayerControlsState].onCreateNetwork {
    detail: CreateNetwork =>
      // spawn a network node
      val newPosition = entity.position
      world.entities.spawnEntity(NetworkNature(newPosition, Map.empty, 5.0f))
  }

}
