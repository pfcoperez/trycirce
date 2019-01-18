package org.pfcoperez

import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveEncoder
import org.pfcoperez.EncodingAndDecodingStaticProtocol.Model.{Address, UserDetails}
import org.pfcoperez.containers.Sensitive
import org.pfcoperez.containers.Sensitive.StaticProtocol.{postProcess, sensitiveEncoder}

object EncodingAndDecodingStaticProtocol extends App {

  object Model {
    case class Address(city: String)
    case class UserDetails(name: String, password: String, address: Sensitive[Address, String])
  }

  object StaticProtocol {
    implicit lazy val cfg: Configuration = Configuration.default.withSnakeCaseConstructorNames.withSnakeCaseMemberNames

    implicit val addressFormat = deriveEncoder[Address]
    implicit val userDetailsFormat = deriveEncoder[UserDetails]
  }

  import StaticProtocol._

  val ex01 = UserDetails("Happy Cow", "Moo", Sensitive(Address("Kobe"), "-"))

  val beforePostProcessing = userDetailsFormat(ex01)

  println(beforePostProcessing)

  Seq(true, false).foreach { redact =>
    println {
      postProcess(beforePostProcessing, redact)
    }
  }

}
