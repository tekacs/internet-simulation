package improbable.apps

import java.nio.file.{Files, Paths}

import com.typesafe.scalalogging.Logger
import improbable.behaviours.internet.GenerationRateUpdate
import improbable.launch.LocalFlag
import improbable.math.Coordinates
import improbable.natures.{NetworkNature, RouteNature}
import improbable.papi._
import improbable.papi.world.AppWorld
import improbable.papi.world.messaging.CustomMsg
import improbable.papi.worldapp.{WorldApp, WorldAppLifecycle}
import org.flagz.{ScalaFlagz, FlagInfo, FlagContainer}

import scala.io.{BufferedSource, Source}
import scala.util.Random

import scala.concurrent.duration._

case class NetworkDetails(networkId: EntityId, coordinates: Coordinates)

case class TransientFlow(src: EntityId, dest: EntityId, rate: Float = 5.0f)

case class RouteTo(id: EntityId) extends CustomMsg

case class BuildNetwork(position: Coordinates) extends CustomMsg

class NetworkSpawner(val world: AppWorld,
                     val logger: Logger,
                     val lifecycle: WorldAppLifecycle) extends WorldApp {


  def buildFlow(nodes: List[NetworkDetails]): Option[TransientFlow] = {
    val n1 = randomNode(nodes)
    val close = nodes.filter { n =>
      val distance = n.coordinates.distanceTo(n1.coordinates)
      distance > 0 && (Random.nextDouble * distance) < 0.1
    }

    if (close.isEmpty) {
      None
    } else {
      val n2 = randomNode(close)
      Some(TransientFlow(n1.networkId, n2.networkId))
    }
  }
  private def randomNode(ids: Seq[NetworkDetails]): NetworkDetails = {
    ids(Math.floor(ids.size * Math.random()).toInt)
  }

  private def activateFlow(n: TransientFlow) = {
    var netFlow: Map[EntityId, Float] = Map.empty
    netFlow = netFlow.updated(n.dest, 5f)
    world.messaging.sendToEntity(n.src, GenerationRateUpdate(netFlow))
  }

  private def removeFlow(n: TransientFlow) = {
    world.messaging.sendToEntity(n.src, GenerationRateUpdate(Map.empty))
  }

  private def buildNetwork(coordinates: Coordinates, scale: Float, asnumber: Long) = {
    new NetworkDetails(world.entities.spawnEntity(NetworkNature(coordinates, Map.empty, scale, asnumber)), coordinates)
  }

  def findFile(name: String): BufferedSource = {

    val path1 = Paths.get(".\\src\\main\\resources")
    val path2 = Paths.get(".\\workers\\gsim\\src\\main\\resources\\")

    if (Files.exists(path1)) {
      Source.fromFile(path1.resolve(name).toFile)
    } else if (Files.exists(path2)) {
      Source.fromFile(path2.resolve(name).toFile)
    } else {
      val path = Paths.get(".").toAbsolutePath

      val files = path.toFile.listFiles()
      logger.error(s"Cannot find csvs in ${path.toString}. Available files are: ${files.mkString(",")}")
      logger.error("")
      Source.fromInputStream(getClass.getClassLoader.getResourceAsStream(name))
    }
  }

  val onlyAmerica = true

  val asPositions = findFile("networks.csv").getLines().flatMap(n => {
    val fields = n.split(",")
    val id = fields(0).toLong

    val latitude = fields(1).toFloat
    val longditude = fields(2).toFloat

    if (onlyAmerica && inAmerica(latitude, longditude)) {
      val r = Random.nextGaussian() * 0.06f
      val theta = Random.nextDouble() * Math.PI * 2
      val z = (latitude + r * Math.sin(theta)) * 20
      val x = (longditude + r * Math.cos(theta)) * 20
      val coordinate = Coordinates(x + 1900, 0.0f, z - 782) * 4
      Some((id, coordinate))
    } else if (!onlyAmerica) {
      val z = latitude * 20
      val x = longditude * 20
      val coordinate = Coordinates(x, 0.0f, z)
      Some((id, coordinate))
    } else {
      None
    }
  }).toMap

  logger.info(s"there are ${asPositions.size} nodes")

  def inAmerica(lat: Float, long: Float): Boolean = {
    if (LocalFlag.running_locally.get()) {
      lat < 55 && lat > 20 && long > -80 && long < -47
    } else {
      lat < 55 && lat > 20 && long > -137 && long < -47
    }
  }

  var routeDefs: List[((Long, Coordinates), (Long, Coordinates))] = findFile("routes.csv").getLines().flatMap { n =>
    val fields = n.split(",")
    val n1 = fields(0).toLong
    val n2 = fields(1).toLong
    for {
      p1 <- asPositions.get(n1)
      p2 <- asPositions.get(n2)
    } yield {
      ((n1, p1), (n2, p2))
    }
  }.toList

//
  private val fakeRoutes: Seq[((Long, Coordinates), (Long, Coordinates))] = for {
    n1 <- asPositions.toSeq
    n2 <- asPositions.filter { n => val d = n._2.distanceTo(n1._2); d > 1 && d < 10 }.toList.sortBy(_._2.distanceTo(n1._2)).take(3)
    if n1._1 < n2._1
  } yield {
      (n1, n2)
    }

  private val sortedRoutes = (fakeRoutes.toList. take(5000) ++ routeDefs).sortBy { case ((_, p1), (_, p2)) => p1.distanceTo(p2) }


  ///// Flows
  var active: Seq[TransientFlow] = Seq.empty


  var spawnedNetworks = Map.empty[Long, NetworkDetails]
  var networkEntityIdsToIds = Map.empty[EntityId, Long]
  var networkDetails = List.empty[NetworkDetails]

  def spawnRoute(idA: Long, posA: Coordinates, idB: Long, posB: Coordinates): Unit = {
    val n1 = getNetwork(idA, posA)
    val n2 = getNetwork(idB, posB)
    world.entities.spawnEntity(RouteNature(world, n1.networkId, posA, n2.networkId, posB, 300))
  }

  def getNetwork(id: Long, pos: Coordinates): NetworkDetails = {
    spawnedNetworks.getOrElse(id, {
      val network = buildNetwork(pos, 20, id)
      spawnedNetworks = spawnedNetworks.updated(id, network)
      networkEntityIdsToIds = networkEntityIdsToIds.updated(network.networkId, id)
      networkDetails = network +: networkDetails
      network
    })
  }

  def addFlows(n: Int) {
    val newFlows = Range(0, n).flatMap(_ => {
      val flow = buildFlow(networkDetails)
      flow.foreach(activateFlow)
      flow
    })
    active = newFlows ++ active
  }

  var allRoutes = sortedRoutes
  var routesSpawned = 0
  val totalRoutes = allRoutes.length

  def spawnStuff(n: Int): Unit = {
    val toSpawn = allRoutes.take(n)
    allRoutes = allRoutes.drop(n)
    toSpawn.foreach {
      case (idA, idB) =>
        routesSpawned += 1
        spawnRoute(idA._1, idA._2, idB._1, idB._2)
    }
    addFlows(n / 100)
  }

  spawnStuff(5000)
  //  asPositions.map(c => getNetwork(c._1, c._2))

  var i = 0
  world.timing.every(500.millis) {
    if (active.nonEmpty) {
      (0 until NetworkFlags.flow_update_rate.get()).foreach { n =>
        val oldFlow = active(i)
        val newFlow = buildFlow(networkDetails)
        removeFlow(oldFlow)
        newFlow.foreach { flow =>
          active = active.updated(i, flow)
          activateFlow(flow)
        }
        i = (i + 1) % active.length
      }
    }
  }

  world.messaging.onReceive {
    case RouteTo(dest) =>
      networkEntityIdsToIds.keys.foreach { source => activateFlow(TransientFlow(source, dest)) }

    case BuildNetwork(center) =>
      spawnStuff(1500)
      logger.info(s"spawned $routesSpawned/$totalRoutes routes, ${allRoutes.length} remaining, spawned ${spawnedNetworks.size} network")
    case NetworkStats(distance, routes, lengthOfRoutes) =>
      for {
        fromId <- networkEntityIdsToIds.get(distance.from)
        toId <- networkEntityIdsToIds.get(distance.to)
      } {
        logger.info(s"Longest route is from $fromId to $toId of length ${distance.length}")
      }

      logger.info(f"Average length of route is ${lengthOfRoutes.length}%1.3f")

      networkEntityIdsToIds.get(routes.network).foreach {
        id =>
          logger.info(s"The network with most connections is $id with ${routes.count} routes")
      }
  }
}

object NetworkFlags extends FlagContainer{

  @FlagInfo(help = "How many routes should be updated each tick")
  val flow_update_rate = ScalaFlagz.valueOf(100)

}
