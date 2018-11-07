package org.pfcoperez.webserver

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import io.circe.Encoder
import org.pfcoperez.Models.A
import org.pfcoperez.{Models, SerdesContext}

import scala.concurrent.{ExecutionContext, Future}

object Server extends App {

  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  implicit def entityToEntityMarshaller[T : Encoder]: ToEntityMarshaller[T] = {
    import io.circe.syntax._
    Marshaller.opaque { x: T =>
      x.asJson.toString
    }
  }


  val route = path("alive") {
    get {
      complete(StatusCodes.OK)
    }
  } ~ pathPrefix("entities") {
    path("a") {
      get {
        import org.pfcoperez.Models.A
        implicit val context: SerdesContext = SerdesContext(redactSecrets = false, strictDeser = true)
        val protocol = org.pfcoperez.Models.Protocol()
        import protocol._

        val aValue = A(42)
        //complete("a")
        complete(aValue)
      }
    }
  }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

}
