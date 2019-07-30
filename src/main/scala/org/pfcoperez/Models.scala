package org.pfcoperez

import cats.Show
import io.circe.{Decoder, Encoder, Json, JsonObject, ObjectEncoder}
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveEncoder
import io.circe.generic.extras.semiauto.deriveDecoder
import org.pfcoperez.Models.LazyProtocol.PostprocessingContainer.{Discard, Keep}
//import custom.circe.semiauto.deriveDecoder
import io.circe.generic.extras.encoding.ConfiguredObjectEncoder
import org.pfcoperez.EncodingAndDecodingStaticProtocol.Model.{Address, UserDetails}
import org.pfcoperez.containers.Sensitive
import shapeless.Lazy

object Models {

  sealed trait Foo

  case class Bar(xs: Vector[String]) extends Foo

  case class Qux(i: Int, d: Option[Double], a: A) extends Foo

  case class UserDetails(name: String, password: String, other: Sensitive[String, String], maybeFriend: Option[UserDetails]) extends Foo

  case class Contract(id: String, customer: Sensitive[UserDetails, String]) extends Foo

  case class A(x: Int)

  case class Address(city: String)

  case class User(name: String, password: String, address: Sensitive[Address, String])

  case class SometimesNotThere[T](x: T)

  case class Special(address: SometimesNotThere[Address])

  object Protocol {
    import Sensitive.Protocol._

    implicit lazy val cfg: Configuration = Configuration.default.withSnakeCaseConstructorNames.withSnakeCaseMemberNames

    implicit def applyContextParam[T, F[_]](implicit f: SerdesContext => F[T], serdesContext: SerdesContext): F[T] =
      f(serdesContext)

    implicit val fooEncoder: SerdesContext => Encoder[Foo] = {
      implicit serdesContext => deriveEncoder[Foo]
    }
    implicit val barEncoder: SerdesContext => Encoder[Bar] = {
      implicit serdesContext => deriveEncoder[Bar]
    }
    implicit val quxEncoder: SerdesContext => Encoder[Qux] = {
      implicit serdesContext => deriveEncoder[Qux]
    }
    implicit val aEncoder: SerdesContext => Encoder[A] = {
      implicit serdesContext => deriveEncoder[A]
    }
    implicit val userEncoder: SerdesContext => Encoder[UserDetails] = {
      implicit serdesContext => deriveEncoder[UserDetails]
    }
    implicit val contractEncoder: SerdesContext => Encoder[Contract] = {
      implicit serdesContext => deriveEncoder[Contract]
    }

    implicit val aDecoder: SerdesContext => Decoder[A] = {
      implicit serdesContext => deriveDecoder
    }
    implicit val quxDecoder: SerdesContext => Decoder[Qux] = {
      implicit serdesContext => deriveDecoder
    }
  }

  object StaticProtocol {
    import Sensitive.StaticProtocol.sensitiveEncoder
    implicit lazy val cfg: Configuration = Configuration.default.withSnakeCaseConstructorNames.withSnakeCaseMemberNames

/*    implicit val fooDecoder: Decoder[Foo] = deriveDecoder
    implicit val barDecoder: Decoder[Bar] = deriveDecoder
    implicit val quxDecoder: Decoder[Qux] = deriveDecoder
    implicit val aDecoder: Decoder[A] = deriveDecoder
    implicit val userDecoder: Decoder[UserDetails] = deriveDecoder
    implicit val contractDecoder: Decoder[Contract] = deriveDecoder
    implicit val addressDecoder: Decoder[Address] = deriveDecoder
    implicit val userDetailsDecoder: Decoder[User] = deriveDecoder*/

    implicit val fooEncoder: Encoder[Foo] = deriveEncoder
    implicit val barEncoder: Encoder[Bar] = deriveEncoder
    implicit val quxEncoder: Encoder[Qux] = deriveEncoder
    implicit val aEncoder: Encoder[A] = deriveEncoder
    implicit val userEncoder: Encoder[UserDetails] = deriveEncoder
    implicit val contractEncoder: Encoder[Contract] = deriveEncoder
    implicit val addressEncoder: Encoder[Address] = deriveEncoder
    implicit val userDetailsEncoder: Encoder[User] = deriveEncoder

  }

  object LazyProtocol {

    import shapeless.Lazy

    trait ContextualEncoder[T] {
      def genEncoder(implicit serdesContext: Lazy[SerdesContext]): Encoder[T]
    }

    object ContextualEncoder {
      def apply[T](f: SerdesContext => Encoder[T]): ContextualEncoder[T] = new ContextualEncoder[T] {
        override def genEncoder(implicit serdesContext: Lazy[SerdesContext]): Encoder[T] = f(serdesContext.value)
      }
    }

    implicit def contextualEncoderAsEncoder[T](
      implicit serdesContext: SerdesContext,
      cEncoder: Lazy[ContextualEncoder[T]]
    ): Encoder[T] = cEncoder.value.genEncoder

    implicit lazy val cfg: Configuration = Configuration.default.withSnakeCaseConstructorNames.withSnakeCaseMemberNames
    import Sensitive.LazyProtocol.sensitiveEncoder

    implicit def fooEncoder(implicit serdesContext: Lazy[SerdesContext]): Encoder[Foo] = deriveEncoder[Foo]

    implicit val barEncoder: Encoder[Bar] = deriveEncoder[Bar]
    implicit val quxEncoder: Encoder[Qux] = deriveEncoder[Qux]
    implicit val aEncoder: Encoder[A] = deriveEncoder[A]

    implicit def userDetailsEncoder(implicit serdesContext: Lazy[SerdesContext]): Encoder[UserDetails] = deriveEncoder
    implicit def userDetailsEncoder = ContextualEncoder[UserDetails](implicit serdesCtx => deriveEncoder)
    implicit def contractEncoder(implicit serdesContext: Lazy[SerdesContext]): Encoder[Contract] = deriveEncoder
    implicit def addressEncoder = ContextualEncoder[Address](implicit serdesCtx => deriveEncoder)
    implicit val userEncoder = ContextualEncoder[User](implicit serdesCtx => deriveEncoder)

    trait PostprocessingContainer[T] {
      import PostprocessingContainer._

      val containerName: String
      val postAction: PostprocessAction

      final def genPostContainer[Content : Encoder](content: Content): Json = {
        Json.obj(
          nestedField -> implicitly[Encoder[Content]].apply(content),
          containerField -> Json.fromString(containerName),
          actionField -> postprocessActionEncoder(postAction)
        )
      }

    }

    object PostprocessingContainer {

      private val nestedField = "__post_processing_nested"
      private val containerField = "__post_processing_container"
      private val actionField = "__post_processing_action"

      sealed trait PostprocessAction {
        val tag: String
      }
      object Discard extends PostprocessAction {
        override val tag: String = "DISCARD"
      }
      object Keep extends PostprocessAction {
        override val tag: String = "KEEP"
      }

      private implicit def postprocessActionEncoder: Encoder[PostprocessAction] =
        new Encoder[PostprocessAction] {
          override def apply(a: PostprocessAction): Json = Json.fromString(a.tag)
        }

      def postProcess(json: Json): Json = {

        def filtered(obj: JsonObject): Json = {

          val updatedIterable = obj.toIterable.flatMap { case (key, json) =>
              val maybeBoxed = for {
                jsonObject <- json.asObject
                nestedJson <- jsonObject.apply(nestedField)
                actionJson <- jsonObject.apply(containerField)
                _ <- jsonObject.apply(actionField)
                action <- actionJson.asString
              } yield {
                (action == Keep.tag, nestedJson)
              }

              val notBoxed = Some(true -> json)

            (maybeBoxed orElse notBoxed) collect {
              case (true, result: Json) => key -> result.asObject.map(filtered).getOrElse(result)
            }
          }

          Json.fromJsonObject(
            JsonObject.fromIterable(updatedIterable)
          )

        }

        json.asObject.map { root =>
          filtered(root)
        } getOrElse json
      }

      def postProcessBoxEncoder[T : ContextualEncoder, Box[T]](extractor: Box[T] => T)(
        implicit postContainerOps: PostprocessingContainer[Box[T]]
      ) = ContextualEncoder[Box[T]] { serdesContext =>
        implicit val innerFormat = implicitly[ContextualEncoder[T]].genEncoder(serdesContext)
        new Encoder[Box[T]] {
          override def apply(a: Box[T]): Json = {
            val content = extractor(a)
            if (serdesContext.hideStuff) postContainerOps.genPostContainer(content)
            else innerFormat(content)
          }
        }
      }

    }

    implicit def sometimesNotThereEncoder[T : ContextualEncoder] = {
      implicit val ev = new PostprocessingContainer[SometimesNotThere[T]] {
        override val containerName: String = classOf[SometimesNotThere[T]].getSimpleName
        override val postAction: PostprocessingContainer.PostprocessAction = Discard
      }
      PostprocessingContainer.postProcessBoxEncoder[T, SometimesNotThere](_.x)
    }

    implicit val specialFormat = ContextualEncoder[Special](implicit serdesCtx => deriveEncoder)

  }
}
