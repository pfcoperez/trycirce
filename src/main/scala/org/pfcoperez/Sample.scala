package org.pfcoperez

import io.circe.Encoder
//import io.circe.generic.auto._
import io.circe.generic.encoding.DerivedObjectEncoder
import io.circe.syntax._
import io.circe.generic.semiauto.deriveEncoder
import shapeless.Lazy

object Sample extends App {

  import Models._

  val foo: Foo = UserDetails("pablo", "dadada", Sensitive("Context sensitive value", "-"))

  val json = foo.asJson

  println(json)

  //val decodedFoo = decode[UserDetails](json)
  //println(decodedFoo)
}
