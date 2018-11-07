package org.pfcoperez

import io.circe.{Decoder, Encoder, HCursor}
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveEncoder
import custom.circe.semiauto.deriveDecoder
import org.pfcoperez.containers.Sensitive

object Models {

  sealed trait Foo

  case class Bar(xs: Vector[String]) extends Foo

  case class Qux(i: Int, d: Option[Double]) extends Foo

  case class UserDetails(name: String, password: String, other: Sensitive[String, String], maybeFriend: Option[UserDetails]) extends Foo

  case class Contract(id: String, customer: Sensitive[UserDetails, String]) extends Foo

  case class A(x: Int)

  trait Protocol extends containers.Sensitive.Protocol {

    implicit lazy val cfg: Configuration = Configuration.default.withSnakeCaseConstructorNames.withSnakeCaseMemberNames

    implicit lazy val fooEncoder: Encoder[Foo] = deriveEncoder[Foo]
    implicit lazy val barEncoder: Encoder[Bar] = deriveEncoder[Bar]
    implicit lazy val quxEncoder: Encoder[Qux] = deriveEncoder[Qux]
    implicit lazy val userEncoder: Encoder[UserDetails] = deriveEncoder[UserDetails]
    implicit lazy val contractEncoder: Encoder[Contract] = deriveEncoder[Contract]


    implicit lazy val aDecoder: Decoder[A] = deriveDecoder
  }

}
