package improbable.apps

import com.typesafe.scalalogging.Logger
import improbable.papi.EntityId
import improbable.papi.world.AppWorld
import improbable.papi.world.messaging.CustomMsg
import improbable.papi.worldapp.{WorldApp, WorldAppDescriptor, WorldAppLifecycle}

import scala.concurrent.duration._

case class MaxDistance(from: EntityId, to: EntityId, length: Int)

case class NumberOfRoutes(network: EntityId, count: Int)

case class AverageLengthOfRoute(network: EntityId, length: Double)

case class NetworkStats(distance: MaxDistance, routes: NumberOfRoutes, lengthOfRoute: AverageLengthOfRoute) extends CustomMsg

class StatsApp(val world: AppWorld,
               val logger: Logger,
               val lifecycle: WorldAppLifecycle) extends WorldApp {


  var maxDistance = 0
  var distanceFromTo = (-1l, -1l)

  var maxNumberOfRoutes = 0
  var maxRoutesId = -1l

  var averageLengthOfRoutes = Map.empty[EntityId, Double]

  world.messaging.onReceive {
    case NetworkStats(distance, routes, lengths) =>
      averageLengthOfRoutes = averageLengthOfRoutes.updated(lengths.network, lengths.length)

      if (distance.length > maxDistance) {
        maxDistance = distance.length
        distanceFromTo = (distance.from, distance.to)
      }

      if (routes.count > maxNumberOfRoutes) {
        maxNumberOfRoutes = routes.count
        maxRoutesId = routes.network
      }
  }


  world.timing.every(1.minute) {

    val averageLength = averageLengthOfRoutes.values.sum / averageLengthOfRoutes.size

    val stats = NetworkStats(
      MaxDistance(distanceFromTo._1, distanceFromTo._2, maxDistance)
      , NumberOfRoutes(maxRoutesId, maxNumberOfRoutes)
      , AverageLengthOfRoute(-1l, averageLength)
    )

    maxDistance = 0 // it will probably get shorter eventually

    world.messaging.sendToApp(WorldAppDescriptor.forClass[NetworkSpawner].name, stats)
  }


}
