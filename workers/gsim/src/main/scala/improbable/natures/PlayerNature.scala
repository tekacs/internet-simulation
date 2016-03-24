package improbable.natures

import improbable.behaviours.player.controls.{DelegateLocalPlayerCheckToOwnerBehaviour, DelegatePlayerControlsToOwnerBehaviour, PlayerBehaviour}
import improbable.corelib.natures._
import improbable.corelib.natures.base.BaseComposedTransformNature
import improbable.corelib.util.EntityOwner
import improbable.math.Coordinates
import improbable.papi.engine.EngineId
import improbable.papi.entity.EntityPrefab
import improbable.papi.entity.behaviour.EntityBehaviourDescriptor
import improbable.player.LocalPlayerCheckState
import improbable.player.controls.PlayerControlsState

object PlayerNature extends NatureDescription {

  override val dependencies = Set[NatureDescription](BaseComposedTransformNature)

  override def activeBehaviours: Set[EntityBehaviourDescriptor] = {
    Set(
      descriptorOf[PlayerBehaviour],
      descriptorOf[DelegatePlayerControlsToOwnerBehaviour],
      descriptorOf[DelegateLocalPlayerCheckToOwnerBehaviour]
    )
  }

  def apply(engineId: EngineId): NatureApplication = {
    application(
      states = Seq(
        EntityOwner(ownerId = Some(engineId)),
        PlayerControlsState(),
        LocalPlayerCheckState()
      ),
      natures = Seq(
        BaseComposedTransformNature(entityPrefab = EntityPrefab("Player"), initialPosition = Coordinates(0, 0.5, 0), isPhysical = false, isVisual = true)
      )
    )
  }

}
