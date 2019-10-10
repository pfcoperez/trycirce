package org.pfcoperez.webclient

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl._
import akka.stream.ActorMaterializer

import scala.concurrent.{Await, Future}
import scala.util._
import concurrent.duration._

object ClientApp extends App {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val delay = 4 seconds
  val delayBetweenRequests = 2 seconds
  val awaitFor = 10 seconds
  val nTries = 10

  val slowUrl =
    s"http://slowwly.robertomurray.co.uk/delay/${delay.toMillis}/url/http://www.google.es"

  (1 to nTries).foreach { _ =>
    val fa = Http()
      .singleRequest(HttpRequest(uri = slowUrl))
    fa.onComplete {
      case Success(res) =>
        res.discardEntityBytes()
        println(res)
      case Failure(_) => sys.error("something wrong")
    }

    Await.ready(fa, awaitFor)

    Thread.sleep(delay.toMillis)
  }

  system.terminate()

}
