package org.pfcoperez

import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveEncoder
import org.pfcoperez.EncodingAndDecodingStaticProtocol.Model.{Address, UserDetails}
import org.pfcoperez.containers.Sensitive
import org.pfcoperez.containers.Sensitive.StaticProtocol.{Encrypted, EncryptedString}

object EncodingAndDecodingStaticProtocol extends App {

  object Model {
    //sealed trait Foo

    case class Address(city: String)
    case class UserDetails(name: String, password: String, address: Sensitive[Address, Encrypted[String]]) //extends Foo
  }

  case class A()

  object StaticProtocol {
    implicit lazy val cfg: Configuration = Configuration.default.withSnakeCaseConstructorNames.withSnakeCaseMemberNames

    //import Sensitive.StaticProtocol.sensitiveEncoder
    implicit val addressFormat = deriveEncoder[Address]
    implicit val userDetailsFormat = deriveEncoder[UserDetails]
  }

  import StaticProtocol._
  import EncryptedString.encryptedSensitive

  val ex01 = UserDetails("aaa", "bbb", Address("Kobe"))

  println {
    userDetailsFormat(ex01).toString
  }

}
