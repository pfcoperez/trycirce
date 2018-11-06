package org.pfcoperez

import io.circe.Json
import io.circe.syntax._
import io.circe.literal._
import org.pfcoperez.containers.Sensitive

object Sample extends App with Models.Protocol {
  import Models._
  override implicit val context: SerdesContext = SerdesContext(redactSecrets = false, strictDeser = true)

  val aFriend: UserDetails = UserDetails("someone", "dududu", Sensitive("secret", "-"), None)
  val user: UserDetails = UserDetails("pablo", "dadada", Sensitive("Context sensitive value", "-"), Some(aFriend))
  val foo: Foo = user

  val contract = Contract(
    id = "42424242",
    customer = Sensitive[UserDetails, String](
      user,
      "*****"
    )
  )

  //val json = foo.asJson
  val json1 = contract.asJson
  println(json1)


  val json2: Json =
    json""" { "x": 1 , "y": 2} """
    //json""" { "x": 1 } """

  val decodedA = aDecoder.decodeJson(json2)
  println(decodedA)
}
