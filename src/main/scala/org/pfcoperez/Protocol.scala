package org.pfcoperez

import io.circe.{Encoder, JsonObject}
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.encoding.ConfiguredObjectEncoder
import org.pfcoperez.Models.Contract
import shapeless.Lazy
//import io.circe.generic.semiauto.deriveEncoder
import io.circe.generic.extras.semiauto._
import org.pfcoperez.Models.{Bar, Foo, Qux, Sensitive, UserDetails}

case class Protocol(context: SecurityContext) {

  implicit lazy val cfg: Configuration = Configuration.default.withSnakeCaseConstructorNames.withSnakeCaseMemberNames

  implicit lazy val fooEncoder: Encoder[Foo] = deriveEncoder[Foo]
  implicit lazy val barEncoder: Encoder[Bar] = deriveEncoder[Bar]
  implicit lazy val quxEncoder: Encoder[Qux] = deriveEncoder[Qux]
  implicit lazy val userEncoder: Encoder[UserDetails] = deriveEncoder[UserDetails]
  implicit lazy val contractEncoder: Encoder[Contract] = deriveEncoder[Contract]

  implicit def sensitiveEncoder[T, RedactedT](
    implicit redactedEncoderEvidence: Encoder[RedactedT],
    encoderEvidence: Encoder[T],
  ): Encoder[Sensitive[T, RedactedT]] = {
    if (context.redactSecrets) {
      redactedEncoderEvidence.contramap[Sensitive[T, RedactedT]] {
        _.redacted
      }
    } else {
      encoderEvidence.contramap[Sensitive[T, RedactedT]] {
        _.value
      }
    }
  }

}
