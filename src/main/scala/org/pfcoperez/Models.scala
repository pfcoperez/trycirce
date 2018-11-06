package org.pfcoperez

import io.circe.{Decoder, Encoder, HCursor}
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.decoding.ConfiguredDecoder
import io.circe.generic.extras.semiauto.{deriveEncoder, deriveDecoder => origDeriveDecoder}
import org.pfcoperez.containers.Sensitive
import shapeless._
import shapeless.ops.hlist
import shapeless.ops.record._

object Models {

  sealed trait Foo

  case class Bar(xs: Vector[String]) extends Foo

  case class Qux(i: Int, d: Option[Double]) extends Foo

  case class UserDetails(name: String, password: String, other: Sensitive[String, String], maybeFriend: Option[UserDetails]) extends Foo

  case class Contract(id: String, customer: Sensitive[UserDetails, String]) extends Foo

  case class A(x: Int)

  trait Protocol extends containers.Sensitive.Protocol {

    implicit lazy val cfg: Configuration = Configuration.default.withSnakeCaseConstructorNames.withSnakeCaseMemberNames

    implicit lazy val fooEncoder: Encoder[Foo] = deriveEncoder[Foo]
    implicit lazy val barEncoder: Encoder[Bar] = deriveEncoder[Bar]
    implicit lazy val quxEncoder: Encoder[Qux] = deriveEncoder[Qux]
    implicit lazy val userEncoder: Encoder[UserDetails] = deriveEncoder[UserDetails]
    implicit lazy val contractEncoder: Encoder[Contract] = deriveEncoder[Contract]

    def deriveDecoder[T, Repr <: HList, KeysRepr <: HList](
      implicit
      lgen: LabelledGeneric.Aux[T, Repr],
      keys: Keys.Aux[Repr, KeysRepr],
      toTraversable: hlist.ToTraversable.Aux[KeysRepr, List, Symbol],
      configuration: Configuration,
      context: SerdesContext,
      decoderEvidence: Lazy[ConfiguredDecoder[T]]
    ): Decoder[T] = {
      lazy val expectedFields = Keys[Repr]
        .apply()
        .toList
        .map(fieldSymbol => configuration.transformMemberNames(fieldSymbol.name))
        .toSet
      lazy val predicate = { c: HCursor =>
        val maybeOk = for {
          json <- c.focus
          jsonKeys <- json.hcursor.keys
        } yield jsonKeys.toSet == expectedFields
        maybeOk.getOrElse(false)
      }
      if(context.strictDeser)
        origDeriveDecoder[T].validate(predicate, s"Unexpected fields. Valid fields: ${expectedFields.mkString(",")}.")
      else
        origDeriveDecoder[T]
    }

    implicit lazy val aDecoder: Decoder[A] = deriveDecoder
  }

}
