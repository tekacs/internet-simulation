package improbable.util

/**
 * Copyright (c) 2016 Improbable Worlds Ltd.
 * All Rights Reserved
 * Date: 17/02/2016
 * Time: 10:07
 */


trait RouterTestTools extends {

  def line(n: Int): (Int, Long) = {
    val networks = new RouterWorld()
    (0 until n - 1).foreach(k =>
      networks.link(k, k + 1, k*10)
    )
    networks.run()
  }


  def circle(n: Int): (Int, Long) = {
    val networks = new RouterWorld()
    (0 until n - 1).foreach(k =>
      networks.link(k, k + 1, k)
    )
    networks.link(0, n - 1, n)
    networks.run()
  }

  def fullyConnected(n: Int): (Int, Long) = {
    val networks = new RouterWorld()
    var t = 0
    for {
      i <- 0 until n
      j <- 0 until i
      if i != j
    } {
      t += 1
      networks.link(i, j, t)
    }
    networks.run()
  }

  def connected(n: Int, overlap: Int): (Int, Long) = {
    val networks = new RouterWorld()
    var t = 0
    for {
      i <- 0 until n
      j <- 0 until overlap
    } {
      t += 1
      networks.link(i, (i + j + 1) % n, t)
    }
    networks.run()
  }


  def pinWheel(size: Int, debug: Boolean = false): (Int, Long) = {
    val networks = new RouterWorld(debug)
    (0 until size - 1).foreach(k =>
      networks.link(k, k + 1, k)
    )

    (0 until size - 1).foreach(k =>
      networks.link(k, size, k)
    )
    val (connections_commands, connections_time) = networks.run()

    (0 until size - 1).foreach(k =>
      networks.unlink(k, size, k)
    )
    val (disconnections_commands, disconnections_time) = networks.run()

    (connections_commands + disconnections_commands, connections_time + disconnections_time)
  }

  def lolipop(length: Int, loopLength: Int): (Int, Long) ={
    val networks = new RouterWorld()
    (0 until loopLength*2 + length).foreach{ n =>
      networks.link(n, n+1)
    }
    networks.link(0,loopLength)
    networks.link(loopLength+length, loopLength*2 + length)

    val (t0, c0) = networks.run("connecting")

    networks.unlink(loopLength + length/2, loopLength + length/2 + 1)

    val (t1, c1) = networks.run("disconnecting")

    (t0 + t1, c0+ c1)
  }

}

object RouterDebugger extends RouterTestTools with App {

  val networks = new RouterWorld(true)
  val n = 10
  (0 until n - 1).foreach(k =>
    networks.link(k, k + 1, k*50)
  )

  networks.run("Linking")
  val route = networks.getRoute(0, 4)

  println(route)
  networks.link(4, 0)
  networks.run("Circularising")
  networks.unlink(0, 4)
  networks.run("unlinking")


    println("\nPinwheel:")
    pinWheel(5, true)

//  println(lolipop(1000,2))
}


object RouterPerfTest extends RouterTestTools with App {


  def run[A](f: => (A, Long), runs: Int): (A, Long) = {
    (0 until runs).map(_ => f).minBy(_._2)
  }


  val runs = 8
  println(s"running best of $runs runs:")
  val (lineActions, lineTime) = run(line(400), runs)
  println(s"line took ${lineTime}ms for $lineActions actions")

  val (connectedActions, connectedTime) = run(connected(300, 10), runs)
  println(s"connected took ${connectedTime}ms for $connectedActions actions")

  val (pinwheelActions, pinwheelTime) = run(pinWheel(100), runs)
  println(s"pinwheel took ${pinwheelTime}ms for $pinwheelActions actions")



  val totalTime: Long = lineTime + connectedTime + pinwheelTime
  val totalActions = lineActions + connectedActions + pinwheelActions
  println(s"total took ${totalTime}ms for $totalActions actions")

}

object RouterMemoryTest extends  RouterTestTools with App {

  System.gc()
  val runtime = Runtime.getRuntime()


  val n = 2000
  val overlap = 2

  val networks = new RouterWorld()
  var t = 0
  for {
      i <- 0 until n
      j <- 0 until overlap
  } {
    t += 1
    networks.link(i, (i + j + 1) % n, t)
  }

  System.gc()
  val mem0 = runtime.totalMemory() - runtime.freeMemory()

  networks.run()

  System.gc()
  val mem1 = runtime.totalMemory() - runtime.freeMemory()

  private val memUsed = mem1 - mem0
  val mbUsed = memUsed/(1024*1024)
  val perEntry = memUsed/(n*n)
  println(s"${mbUsed}MB used, ${perEntry}B per entry")

}