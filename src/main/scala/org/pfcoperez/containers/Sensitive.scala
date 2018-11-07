package org.pfcoperez.containers

import io.circe.Encoder
import org.pfcoperez.SerdesContext

case class Sensitive[T, RedactedT](value: T, redacted: RedactedT)

object Sensitive {

  object Protocol {

    implicit def sensitiveEncoder[T, RedactedT](
      implicit redactedEncoderEvidence: Encoder[RedactedT],
      encoderEvidence: Encoder[T],
      context: SerdesContext
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
