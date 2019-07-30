package org.pfcoperez.webserver

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{Directive1, RequestContext}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.ParameterDirectives.ParamDef
import akka.stream.ActorMaterializer
import io.circe.Encoder
import org.pfcoperez.SerdesContext
import org.pfcoperez.Models.A
import org.pfcoperez.Models.Protocol._

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

  val withSerdesContext: Directive1[SerdesContext] = {
    val rqContextToSerdesContext: RequestContext => SerdesContext =
      _ => SerdesContext(redactSecrets = false, strictDeser = true) //TODO: Logic to build context from request
    extract(rqContextToSerdesContext)
  }


  lazy val route: Route = path("alive") {
    get {
      complete(StatusCodes.OK)
    }
  } ~ pathPrefix("entities") {
    withSerdesContext { implicit context =>
      path("a") {
        get {
          implicitly[ParamDef[String]]
          parameters("a") { _ =>
            val aValue = A(42)
            complete("a")
          }
        }
      }
    }
  } ~ pathPrefix("recursive")(route)

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

}
