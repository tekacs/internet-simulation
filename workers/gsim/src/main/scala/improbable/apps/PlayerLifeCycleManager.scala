package improbable.apps

import com.typesafe.scalalogging.Logger
import improbable.apps.PlayerLifeCycleManager._
import improbable.natures.PlayerNature
import improbable.papi._
import improbable.papi.engine.EngineId
import improbable.papi.world.AppWorld
import improbable.papi.world.messaging.{EngineConnected, EngineDisconnected}
import improbable.papi.worldapp.{WorldApp, WorldAppLifecycle}

class PlayerLifeCycleManager(val world: AppWorld,
                             val logger: Logger,
                             val lifecycle: WorldAppLifecycle) extends WorldApp {

  private var userIdToEntityIdMap = Map[EngineId, EntityId]()

  world.messaging.subscribe {
    case engineConnectedMsg: EngineConnected =>
      engineConnected(engineConnectedMsg)

    case engineDisconnectedMsg: EngineDisconnected =>
      engineDisconnected(engineDisconnectedMsg)
  }

  private def engineConnected(msg: EngineConnected): Unit = {
    msg match {
      // For now use the engineName as the userId.
      case EngineConnected(userId, UNITY_CLIENT, _) =>
        addEntity(userId)
      case EngineConnected(userId, JAVASCRIPT, _) =>
        addEntity(userId)
      case _ =>
    }
  }

  private def addEntity(userId: String): Unit = {
    val playerEntityId = world.entities.spawnEntity(PlayerNature(engineId = userId))
    logger.info(s"Spawning Player with userId $userId and entityId $playerEntityId")
    userIdToEntityIdMap += userId -> playerEntityId
  }

  private def engineDisconnected(msg: EngineDisconnected): Unit = {
    msg match {
      case EngineDisconnected(userId, UNITY_CLIENT) =>
        removeUserIdToEntityIdEntry(userId)
      case _ =>
    }
  }

  private def removeUserIdToEntityIdEntry(userId: EngineId) = {
    userIdToEntityIdMap.get(userId) match {
      case Some(id) =>
        world.entities.destroyEntity(id)
        logger.info(s"Destroying player: $userId with entityId $id")
      case None =>
        logger.warn(s"User disconnected but could not find entity id for player: $userId")
    }
  }
}

object PlayerLifeCycleManager {
  private val UNITY_CLIENT = "UnityClient"
  private val JAVASCRIPT = "JavaScript"
}
