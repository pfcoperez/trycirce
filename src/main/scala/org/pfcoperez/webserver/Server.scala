package org.pfcoperez.webserver

import akka.actor.ActorSystem
import akka.http.scaladsl.server.RequestContext
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import io.circe.Encoder
import org.pfcoperez.Models.{A, ContextualProtocol}
import org.pfcoperez.SerdesContext

object Server extends App {

  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  implicit def entityToEntityMarshaller[T : Encoder]: ToEntityMarshaller[T] = {
    //Extreme simplification.
    import io.circe.syntax._
    Marshaller.opaque { x: T =>
      x.asJson.toString
    }
  }

  def withContextualProtocol[P <: ContextualProtocol](protocolGen: SerdesContext => P): Directive1[P] = {
    val rqContextToSerdesContext: RequestContext => SerdesContext =
      _ => SerdesContext(redactSecrets = false, strictDeser = true) //TODO: Logic to build context from request
    extract(rqContextToSerdesContext.andThen(protocolGen))
  }

  val route = path("alive") {
    get {
      complete(StatusCodes.OK)
    }
  } ~ pathPrefix("entities") {
    path("a") {
      get {
        withContextualProtocol(org.pfcoperez.Models.ModelsProtocol) { protocol =>
          import protocol._

          val aValue = A(42)
          complete(aValue)
        }

      }
    }
  }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

}
