package improbable.natures

import improbable.behaviours.internet._
import improbable.corelib.entity.Prefab
import improbable.corelib.natures.visual.VisualNature
import improbable.corelib.natures.{NatureApplication, NatureDescription}
import improbable.entity.physical.{Position, TagsData}
import improbable.internet.{NetworkBools, NetworkFlows, NetworkId, NetworkScale}
import improbable.math.Coordinates
import improbable.papi.EntityId
import improbable.papi.entity.{Entity, EntityBehaviour}
import improbable.papi.world.World
import improbable.unity.fabric.VisualEngineConstraint

object NetworkNature extends NatureDescription {

  override val dependencies = Set[NatureDescription]()

  override def activeBehaviours = {
    Set(descriptorOf[RouteUpdateBehaviour], descriptorOf[NetworkFlowBehaviour], descriptorOf[NetworkBoolsBehaviour], descriptorOf[MakeVisible])
  }

  def apply(initialPosition: Coordinates, generationRate: Map[EntityId, Float], scale: Float, asnumber: Long = -1l): NatureApplication = {
    application(
      natures = Seq(),
      states = Seq(Prefab("Network"), Position(0, initialPosition.copy(y = -5)), TagsData(tags = List("Network")),
        NetworkScale(scale), NetworkFlows(generationRate, Map.empty, false, 0.0f), NetworkBools(false, false, false, false, -1), NetworkId(asnumber))
    )
  }
}

object RouteNature extends NatureDescription {

  override val dependencies = Set[NatureDescription](VisualNature)

  override def activeBehaviours = {
    Set(descriptorOf[RouteRegistrationBehaviour], descriptorOf[RouteFlowBehaviour], descriptorOf[MakeVisible])
  }

  def apply(world: World, src: EntityId, srcPos: Coordinates, dest: EntityId, destPos: Coordinates, size: Float): NatureApplication = {
    val midPosition = (srcPos.toVector3d + destPos.toVector3d) * 0.5
    application(
      natures = Seq(VisualNature(true)),
      states = Seq(Prefab("Route"), Position(0, midPosition.toCoordinates), TagsData(List("Route")),
        improbable.internet.Route(src, dest, size, 0.0f, 0.0f, srcPos, destPos))
    )
  }
}

class MakeVisible(entity: Entity) extends EntityBehaviour {

  entity.addEngineConstraint(VisualEngineConstraint)

}