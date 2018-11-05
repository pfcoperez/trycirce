package org.pfcoperez

object Models {

  sealed trait Foo

  case class Bar(xs: Vector[String]) extends Foo

  case class Qux(i: Int, d: Option[Double]) extends Foo

  case class UserDetails(name: String, password: String, other: Sensitive[String, String]) extends Foo

  case class Contract(id: String, customer: Sensitive[UserDetails, String]) extends Foo

  case class Sensitive[T, RedactedT](value: T, redacted: RedactedT)
}
