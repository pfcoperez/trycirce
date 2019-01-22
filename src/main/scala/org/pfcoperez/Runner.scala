package org.pfcoperez

import io.circe.Encoder
import org.pfcoperez.Models.{Contract, Foo, UserDetails}
import org.pfcoperez.containers.Sensitive
import io.circe.syntax._

object Runner extends App {

  val model = {
    val aFriend: UserDetails =
      UserDetails("someone", "dududu", Sensitive("secret", "-"), None)
    val user: UserDetails = UserDetails(
      "pablo",
      "dadada",
      Sensitive("Context sensitive value", "-"),
      Some(aFriend))
    val foo: Foo = user

    val contract = Contract(
      id = "42424242",
      customer = Sensitive[UserDetails, String](
        user,
        "*****"
      )
    )

    contract
  }

  def serializeStatic = {
    import Models.StaticProtocol._
    import Sensitive.StaticProtocol.postProcess
    postProcess(model.asJson, false)
  }

  def serializeLazy = {
    import Models.LazyProtocol._
    implicit val context = SerdesContext(false, false)
    model.asJson
  }

  val precomputedStaticSerializer = {
    import Models.StaticProtocol._
    implicit val context = SerdesContext(false, false)
    implicitly[Encoder[Models.Contract]]
  }

  def serializePrecomputedStatic = {
    import Sensitive.StaticProtocol.postProcess
    implicit val s = precomputedStaticSerializer
    postProcess(model.asJson, false)
  }

  val precomputedLazySerializer = {
    import Models.LazyProtocol._
    implicit val context = SerdesContext(false, false)
    implicitly[Encoder[Models.Contract]]
  }

  def serializePrecomputedLazy = {
    implicit val s = precomputedLazySerializer
    model.asJson
  }

  val cachedSerializer = {
    import Models.LazyCachedProtocol._
    implicit val context = SerdesContext(false, false)
    implicitly[Encoder[Models.Contract]]
  }

  def serializeCached = {
    implicit val s = cachedSerializer
    model.asJson
  }

  def time[R](label: String, f: => R, n: Int = 1) = {
    val preTime = System.nanoTime
    (1 to n) foreach { _ =>
      f
    }
    val postTime = System.nanoTime()
    println(label + "\tAvg: " + (postTime - preTime) / n + " ns")
  }

  println(serializeStatic)
  println(serializeLazy)
  println(serializePrecomputedStatic)
  println(serializePrecomputedLazy)
  println(serializeCached)

  time("static", serializeStatic, 1000)
  time("lazy", serializeLazy, 1000)
  time("static precomputed", serializePrecomputedStatic, 1000)
  time("lazy precomputed", serializePrecomputedLazy, 1000)
  time("cached", serializeCached, 1000)
}
