package org.pfcoperez.custom.circe

import io.circe.{Decoder, HCursor}
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.decoding.ConfiguredDecoder
import org.pfcoperez.SerdesContext
import shapeless.{HList, LabelledGeneric, Lazy}
import shapeless.ops.hlist
import shapeless.ops.record.Keys

import io.circe.generic.extras.semiauto.{deriveDecoder => origDeriveDecoder}

object semiauto {

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
      } yield jsonKeys.forall(expectedFields.contains)
      maybeOk.getOrElse(false)
    }
    if(context.strictDeser)
      origDeriveDecoder[T].validate(predicate, s"Unexpected fields found! Valid fields: ${expectedFields.mkString(",")}.")
    else
      origDeriveDecoder[T]
  }

}
