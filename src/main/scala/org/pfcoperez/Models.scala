package org.pfcoperez

import io.circe.{Decoder, Encoder}
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveEncoder
import custom.circe.semiauto.deriveDecoder
import org.pfcoperez.EncodingAndDecodingStaticProtocol.Model.{Address, UserDetails}
import org.pfcoperez.containers.Sensitive

object Models {

  sealed trait Foo

  case class Bar(xs: Vector[String]) extends Foo

  case class Qux(i: Int, d: Option[Double], a: A) extends Foo

  case class UserDetails(name: String, password: String, other: Sensitive[String, String], maybeFriend: Option[UserDetails]) extends Foo

  case class Contract(id: String, customer: Sensitive[UserDetails, String]) extends Foo

  case class A(x: Int)

  case class Address(city: String)

  case class User(name: String, password: String, address: Sensitive[Address, String])

  object Protocol {
    import Sensitive.Protocol._

    implicit lazy val cfg: Configuration = Configuration.default.withSnakeCaseConstructorNames.withSnakeCaseMemberNames

    implicit def applyContextParam[T, F[_]](implicit f: SerdesContext => F[T], serdesContext: SerdesContext): F[T] =
      f(serdesContext)

    implicit val fooEncoder: SerdesContext => Encoder[Foo] = {
      implicit serdesContext => deriveEncoder[Foo]
    }
    implicit val barEncoder: SerdesContext => Encoder[Bar] = {
      implicit serdesContext => deriveEncoder[Bar]
    }
    implicit val quxEncoder: SerdesContext => Encoder[Qux] = {
      implicit serdesContext => deriveEncoder[Qux]
    }
    implicit val aEncoder: SerdesContext => Encoder[A] = {
      implicit serdesContext => deriveEncoder[A]
    }
    implicit val userEncoder: SerdesContext => Encoder[UserDetails] = {
      implicit serdesContext => deriveEncoder[UserDetails]
    }
    implicit val contractEncoder: SerdesContext => Encoder[Contract] = {
      implicit serdesContext => deriveEncoder[Contract]
    }

    implicit val aDecoder: SerdesContext => Decoder[A] = {
      implicit serdesContext => deriveDecoder
    }
    implicit val quxDecoder: SerdesContext => Decoder[Qux] = {
      implicit serdesContext => deriveDecoder
    }
  }

  object StaticProtocol {
    import Sensitive.StaticProtocol.sensitiveEncoder
    implicit lazy val cfg: Configuration = Configuration.default.withSnakeCaseConstructorNames.withSnakeCaseMemberNames

    implicit val fooEncoder: Encoder[Foo] = deriveEncoder[Foo]
    implicit val barEncoder: Encoder[Bar] = deriveEncoder[Bar]
    implicit val quxEncoder: Encoder[Qux] = deriveEncoder[Qux]
    implicit val aEncoder: Encoder[A] = deriveEncoder[A]
    implicit val userEncoder: Encoder[UserDetails] = deriveEncoder[UserDetails]
    implicit val contractEncoder: Encoder[Contract] = deriveEncoder[Contract]
    implicit val addressEncoder = deriveEncoder[Address]
    implicit val userDetailsEncoder = deriveEncoder[User]

  }

  object LazyProtocol {
    import shapeless.Lazy
    implicit lazy val cfg: Configuration = Configuration.default.withSnakeCaseConstructorNames.withSnakeCaseMemberNames
    import Sensitive.LazyProtocol.sensitiveEncoder

    implicit def fooEncoder(implicit serdesContext: Lazy[SerdesContext]): Encoder[Foo] = deriveEncoder[Foo]

    implicit val barEncoder: Encoder[Bar] = deriveEncoder[Bar]
    implicit val quxEncoder: Encoder[Qux] = deriveEncoder[Qux]
    implicit val aEncoder: Encoder[A] = deriveEncoder[A]

    implicit def userEncoder(implicit serdesContext: Lazy[SerdesContext]): Encoder[UserDetails] = deriveEncoder[UserDetails]
    implicit def contractEncoder(implicit serdesContext: Lazy[SerdesContext]): Encoder[Contract] = deriveEncoder[Contract]
    implicit def addressEncoder(implicit serdesContext: Lazy[SerdesContext]): Encoder[Address] = deriveEncoder[Address]
    implicit def userDetailsEncoder(implicit serdesContext: Lazy[SerdesContext]): Encoder[User] = deriveEncoder[User]
  }
}
