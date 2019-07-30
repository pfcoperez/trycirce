package org.pfcoperez.webserver

import io.circe.Encoder
import org.pfcoperez.Models.{Address, User}
import shapeless.Lazy
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives._
import org.pfcoperez.containers.Sensitive
import akka.http.scaladsl.marshalling._
import ServerLazyFormats.{entityToEntityMarshaller, withSerdesContext}
import org.pfcoperez.Models.LazyProtocol.{ContextualEncoder, contextualEncoderAsEncoder}
import org.pfcoperez.SerdesContext

object RoutesForLazyFormatsServer {

  def contextualRoute(implicit contextualizedEncoder: ContextualEncoder[User]): Route = {
      implicit val serdesContext = SerdesContext(true, true)
      path("cow2") {
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
