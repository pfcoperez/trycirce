package org.pfcoperez.containers

import java.util.Base64

import io.circe.generic.extras.Configuration
import io.circe.{Decoder, Encoder, Json}
import io.circe.parser._
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import org.pfcoperez.SerdesContext

import scala.util.Random

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

  object StaticProtocol {

    implicit lazy val cfg: Configuration = Configuration.default.withSnakeCaseConstructorNames.withSnakeCaseMemberNames

    case class Encrypted[Ciphered](_ciphered: Ciphered)

    lazy val globalKey: String = {
      Random.setSeed(System.currentTimeMillis()*(new Object).hashCode().toLong) // A simple source of entropy
      Random.nextString(32)
    }

    object EncryptedString {
      val charset = "UTF-8"

      implicit def keySpec(key: String): SecretKeySpec =
        new SecretKeySpec(key.getBytes(charset).take(32), "AES")

      def cipher(key: String, mode: Int) = {
        // Inspired by https://gist.github.com/alexandru/ac1c01168710786b54b0
        val cipherInstance = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipherInstance.init(mode, key)
        cipherInstance
      }

      def encrypt[T](x: T, key: String)(implicit encoder: Encoder[T]): Encrypted[String] = {

        val jsonEncoded = encoder(x).noSpaces

        val cipheredBytes = Base64.getEncoder.encode {
          cipher(key, Cipher.ENCRYPT_MODE).doFinal(jsonEncoded.getBytes(charset))
        }

        Encrypted(new String(cipheredBytes, charset))
      }

      def decrypt[T](x: Encrypted[String])(key: String)(implicit decoder: Decoder[T]): Option[T] = {
        val rawJson = new String(
          cipher(key, Cipher.DECRYPT_MODE).doFinal(Base64.getDecoder.decode(x._ciphered.getBytes(charset))),
          charset
        )
        parse(rawJson) flatMap { json =>
          decoder.decodeJson(json)
        } toOption
      }

      implicit def encryptedSensitive[T : Encoder](x: T): Sensitive[T, Encrypted[String]] =
        Sensitive(x, EncryptedString.encrypt(x, globalKey))
    }

    implicit def sensitiveEncoder[T](
      implicit encoderEvidence: Encoder[T]
    ): Encoder[Sensitive[T, Encrypted[String]]] = {
      implicit val encryptedEncoder = io.circe.generic.extras.semiauto.deriveEncoder[Encrypted[String]]
      encryptedEncoder.contramap[Sensitive[T, Encrypted[String]]] { sensitive =>
        EncryptedString.encrypt(sensitive.value, globalKey)
      }
    }

  }

}
