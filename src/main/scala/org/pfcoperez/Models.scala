package org.pfcoperez

object Models {

  sealed trait Foo

  case class Bar(xs: Vector[String]) extends Foo

  case class Qux(i: Int, d: Option[Double]) extends Foo

  case class User(name: String, password: String) extends Foo

}
