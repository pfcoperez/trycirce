package org.pfcoperez
import io.circe.Encoder
import io.circe.generic.extras._
import io.circe.generic.semiauto.deriveEncoder

object Models {

  implicit def config: Configuration = Configuration.default.withSnakeCaseConstructorNames.withSnakeCaseMemberNames

  case class SecurityContext(redactSecrets: Boolean)

  sealed trait Foo

  case class Bar(xs: Vector[String]) extends Foo
  case class Qux(i: Int, d: Option[Double]) extends Foo
  case class UserDetails(name: String, password: String, other: Sensitive[String]) extends Foo
  case class Sensitive[T](value: T, redacted: T)

  implicit lazy val barEncoder: Encoder[Bar] = deriveEncoder[Bar]
  implicit lazy val fooEncoder: Encoder[Foo] = deriveEncoder[Foo]
  implicit lazy val quxEncoder: Encoder[Qux] = deriveEncoder[Qux]
  implicit lazy val userEncoder: Encoder[UserDetails] = deriveEncoder[UserDetails]

  implicit def sensitiveEncoder[T](implicit containedEncoderEvidence: Encoder[T]): Encoder[Sensitive[T]] = {
    val sc: SecurityContext = SecurityContext(false)
    containedEncoderEvidence.contramap[Sensitive[T]] {
      if(sc.redactSecrets) _.redacted
      else _.value
    }
  }
}
