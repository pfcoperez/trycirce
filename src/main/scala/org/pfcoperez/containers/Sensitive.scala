package org.pfcoperez.containers

import java.util.Base64

import io.circe.generic.extras.Configuration
import io.circe.{Decoder, Encoder, Json}
import io.circe.parser._
import javax.crypto.Cipher
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}
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

    private case class Encrypted(_ciphered: String, _redactedAsJsonString: String)

    private object Encrypted {

      lazy val globalKey: String = {
        Random.setSeed(System.currentTimeMillis()*(new Object).hashCode().toLong) // A simple source of entropy
        Random.nextString(32)
      }

      val charset = "UTF-8"

      implicit def keySpec(key: String): SecretKeySpec =
        new SecretKeySpec(key.getBytes(charset).take(32), "AES")

      def cipher(key: String, mode: Int) = {
        // Inspired by https://gist.github.com/alexandru/ac1c01168710786b54b0
        val cipherInstance = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipherInstance.init(mode, key, new IvParameterSpec(Array.fill[Byte](16)(0)))
        cipherInstance
      }

      def encrypt[T, RedactedT](x: T, redacted: RedactedT, key: String)(
        implicit encoderT: Encoder[T],
        encoderRedactedT: Encoder[RedactedT]
      ): Encrypted = {

        val jsonEncoded = encoderT(x).noSpaces

        val cipheredBytes = Base64.getEncoder.encode {
          cipher(key, Cipher.ENCRYPT_MODE).doFinal(jsonEncoded.getBytes(charset))
        }

        Encrypted(new String(cipheredBytes, charset), encoderRedactedT(redacted).toString)
      }

      def decryptRaw[RedactedT](x: Encrypted)(key: String): String =
        new String(
          cipher(key, Cipher.DECRYPT_MODE).doFinal(Base64.getDecoder.decode(x._ciphered.getBytes(charset))),
          charset
        )

      def decryptRawWithGlobalKey[RedactedT](x: Encrypted): String =
        decryptRaw(x)(globalKey)

      def decrypt[T, RedactedT](x: Encrypted)(key: String)(implicit decoder: Decoder[T]): Option[T] = {
        val rawJson = decryptRaw(x)(key)
        parse(rawJson) flatMap { json =>
          decoder.decodeJson(json)
        } toOption
      }

    }

    private val encryptedEncoder = io.circe.generic.extras.semiauto.deriveEncoder[Encrypted]
    private val encryptedDecoder = io.circe.generic.extras.semiauto.deriveDecoder[Encrypted]

    implicit def sensitiveEncoder[T, RedactedT](
      implicit unredactedEncoderEvidence: Encoder[T],
      redactedEncoderEvidence: Encoder[RedactedT]
    ): Encoder[Sensitive[T, RedactedT]] = {
      encryptedEncoder.contramap[Sensitive[T, RedactedT]] { sensitive =>
        Encrypted.encrypt(sensitive.value, sensitive.redacted, Encrypted.globalKey)
      }
    }

    def postProcess(json: Json, redact : Boolean): Json = {
      def postProcess(json: Json): Json = {
        encryptedDecoder.decodeJson(json).flatMap { encrypted =>
          parse {
            if (redact) encrypted._redactedAsJsonString
            else Encrypted.decryptRawWithGlobalKey(encrypted)
          }
        } getOrElse {
          json.mapObject {
            _.mapValues(postProcess)
          }
        }
      }
      postProcess(json)
    }

  }

}
