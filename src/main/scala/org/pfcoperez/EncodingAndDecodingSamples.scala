package org.pfcoperez

import io.circe.{Decoder, Json}
import io.circe.syntax._
import io.circe.literal._
import org.pfcoperez.containers.Sensitive

object EncodingAndDecodingSamples extends App {
  import Models._
  import Protocol._

  implicit val context: SerdesContext = SerdesContext(redactSecrets = false, strictDeser = true)


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

  val decodedA = applyContextParam(aDecoder, context).decodeJson(json2)
  println(decodedA)

  val json3: Json =
    json""" {
            "i": 1,
            "a": {
              "x": 42,
              "y": 42
            }
          }"""

  val decodedQux = applyContextParam(quxDecoder, context).decodeJson(json3)
  println(decodedQux)
}
