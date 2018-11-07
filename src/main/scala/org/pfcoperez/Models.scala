package org.pfcoperez

import io.circe.{Decoder, Encoder, HCursor}
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveEncoder
import custom.circe.semiauto.deriveDecoder
import org.pfcoperez.containers.Sensitive
import shapeless.LabelledGeneric

object Models {

  sealed trait Foo

  case class Bar(xs: Vector[String]) extends Foo

  case class Qux(i: Int, d: Option[Double], a: A) extends Foo

  case class UserDetails(name: String, password: String, other: Sensitive[String, String], maybeFriend: Option[UserDetails]) extends Foo

  case class Contract(id: String, customer: Sensitive[UserDetails, String]) extends Foo

  case class A(x: Int)

  trait ContextualProtocol {
    implicit val context: SerdesContext
  }

  case class ModelsProtocol(context: SerdesContext) extends ContextualProtocol {
    import Sensitive.Protocol._

    implicit lazy val ctx: SerdesContext = context

    implicit lazy val cfg: Configuration = Configuration.default.withSnakeCaseConstructorNames.withSnakeCaseMemberNames

    implicit val fooEncoder: Encoder[Foo] = deriveEncoder[Foo]
    implicit val barEncoder: Encoder[Bar] = deriveEncoder[Bar]
    implicit val quxEncoder: Encoder[Qux] = deriveEncoder[Qux]
    implicit val aEncoder: Encoder[A] = deriveEncoder[A]
    implicit val userEncoder: Encoder[UserDetails] = deriveEncoder[UserDetails]
    implicit val contractEncoder: Encoder[Contract] = deriveEncoder[Contract]

    implicit val aDecoder: Decoder[A] = deriveDecoder
    implicit val quxDecoder: Decoder[Qux] = deriveDecoder
  }

}
