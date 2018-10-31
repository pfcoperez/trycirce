package org.pfcoperez

import io.circe.generic.auto._, io.circe.syntax._

object Sample extends App {

  import Models._

  //val foo: Foo = Qux(13, Some(14.0))
  val foo: Foo = User("pablo", "dadada")
  //val foo = Qux(13, Some(14.0))


  val json = foo.asJson //.noSpaces

  //json.hcursor.

  println(json)

  //val decodedFoo = decode[Foo](json)
  //val decodedFoo = decode[Qux](json)
  
  //println(decodedFoo)

}
