package org.pfcoperez.webserver

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive1, RequestContext}
import akka.stream.ActorMaterializer
import io.circe.Encoder
import org.pfcoperez.Models.{A, Address, User}
import org.pfcoperez.Models.LazyProtocol._
import org.pfcoperez.SerdesContext
import org.pfcoperez.containers.Sensitive
import Sensitive.LazyProtocol.sensitiveEncoder

object ServerLazyFormats extends App {

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


  val route = path("alive") {
    get {
      complete(StatusCodes.OK)
    }
  } ~ pathPrefix("entities") {
    withSerdesContext { implicit context =>
      path("a") {
        get {
          val aValue = A(42)
          complete(aValue)
        }
      } ~ path("cow") {
        get {
          val aCow = User(
            "Happy Cow",
            "moooo",
            Sensitive(
              Address(city = "Kobe"),
              "You stalker! what do you want it for?"
            )
          )
          complete(aCow)
        }
      }
    }
  }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

}
