package org.pfcoperez.containers

import io.circe.Encoder
import org.pfcoperez.SerdesContext

case class Sensitive[T, RedactedT](value: T, redacted: RedactedT)

object Sensitive {

  case class Protocol(context: SerdesContext) {

    implicit def sensitiveEncoder[T, RedactedT](
      implicit redactedEncoderEvidence: Encoder[RedactedT],
      encoderEvidence: Encoder[T],
    ): Encoder[Sensitive[T, RedactedT]] =
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
