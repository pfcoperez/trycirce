package org.pfcoperez

import io.circe.syntax._

object Sample extends App {

  import Models._
  val protocol = new Protocol(SecurityContext(System.currentTimeMillis()/1000 % 2 == 0))
  import protocol._

  val user: UserDetails = UserDetails("pablo", "dadada", Sensitive("Context sensitive value", "-"))
  val foo: Foo = user

  val contract = Contract(
    id = "42424242",
    customer = Sensitive[UserDetails, String](
      user,
      "*****"
    )
  )

  //val json = foo.asJson
  val json = contract.asJson

  println(json)

  //val decodedFoo = decode[UserDetails](json)
  //println(decodedFoo)
}
