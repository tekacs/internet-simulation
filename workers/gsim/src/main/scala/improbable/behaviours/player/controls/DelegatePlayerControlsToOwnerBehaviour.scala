package improbable.behaviours.player.controls

import improbable.corelib.util.EntityOwnerDelegation.entityOwnerDelegation
import improbable.papi.entity.{Entity, EntityBehaviour}
import improbable.player.controls.PlayerControlsState

class DelegatePlayerControlsToOwnerBehaviour(entity: Entity) extends EntityBehaviour {

  entity.delegateStateToOwner[PlayerControlsState]

}
