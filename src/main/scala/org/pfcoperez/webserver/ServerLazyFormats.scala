package org.pfcoperez.webserver

import akka.NotUsed
import akka.actor.{Actor, ActorRef, ActorSystem, Props, Terminated}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshaller.fromEntityStreamingSupportAndEntityMarshaller
import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive1, RequestContext, RouteResult}
import akka.stream.ActorMaterializer
import io.circe.{Encoder, Json, JsonObject}
import org.pfcoperez.Models.{A, Address, SometimesNotThere, Special, User}
import org.pfcoperez.Models.LazyProtocol._
import org.pfcoperez.SerdesContext
import org.pfcoperez.containers.Sensitive
import akka.http.scaladsl.server.directives.{
  ContentTypeResolver,
  DebuggingDirectives,
  LoggingMagnet
}
import java.io.{File, FileOutputStream}
import java.util.zip.{ZipEntry, ZipOutputStream}

import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.ContentType.Binary
import akka.http.scaladsl.model.ws.{TextMessage, UpgradeToWebSocket}
import akka.http.scaladsl.server.directives.BasicDirectives.extractRequest
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.stream.scaladsl._

import scala.concurrent.Future
import scala.concurrent.duration._

object ServerLazyFormats extends App {

  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  // system.eventStream.setLogLevel(Logging.DebugLevel)

  implicit def entityToEntityMarshaller[T: Encoder]: ToEntityMarshaller[T] = {
    Marshaller.opaque { x: T =>
      val wrappedKey = "__wrapped"

      import io.circe.syntax._
      val outer = Json.fromJsonObject(JsonObject(wrappedKey -> x.asJson))

      val maybeProcessed = for {
        outerObj <- PostprocessingContainer.postProcess(outer).asObject
        wrapped <- outerObj(wrappedKey)
      } yield wrapped

      val result = maybeProcessed.getOrElse(Json.obj())

      result.toString
    }
  }

  val withSerdesContext: Directive1[SerdesContext] = {
    val rqContextToSerdesContext: RequestContext => SerdesContext =
      _ =>
        SerdesContext(redactSecrets = false, strictDeser = true) //TODO: Logic to build context from request
    extract(rqContextToSerdesContext)
  }

  val aCow = User(
    "Happy Cow",
    "moooo",
    Sensitive(Address(city = "Kobe"), "You stalker! what do you want it for?")
  )

  val loggingFunction: HttpRequest => RouteResult => Unit = { request =>
    println(s"LOGGING REQUEST: $request");
    { result: RouteResult =>
      println(s"LOGGING RESULT: $result")
    }
  }

  val customLogging =
    DebuggingDirectives.logRequestResult(LoggingMagnet(_ => loggingFunction))

  case class AppEvent(msg: String)

  system.scheduler.schedule(1 seconds, 1 seconds) {
    val str = DateTime.now.toString
    system.eventStream.publish(AppEvent(str))
  }

  import akka.http.scaladsl.common.EntityStreamingSupport
  implicit val entityStreamingSupport = EntityStreamingSupport.csv()

  val watcherActor = system.actorOf {
    Props {
      new Actor {
        override def receive: Receive = {
          case ref: ActorRef =>
            context.watch(ref)
          case termination: Terminated =>
            context.unwatch(termination.actor)
            println(s"WATCHER> TERMINATED ${termination.actor}")
        }
      }
    }
  }

  val dir = parameter("a".as[Boolean] ? true).tflatMap { a =>
    parameter("b".as[String] ? "dontknow").tflatMap { b =>
      println(s"$a $b")
      tprovide(a, b)
    }
  }

  val dir2 = parameter("b".as[String] ? "dontknow").tflatMap { a =>
    parameter("a".as[Boolean] ? true).tflatMap { b =>
      println(s"$a $b")
      tprovide(a, b)
    }
  }

  val route = customLogging {
    path("alive") {
      get {
        complete(StatusCodes.OK)
      }
    } ~ pathPrefix("entities") {
      withSerdesContext { implicit context =>
        path("a") {
          get {
            val aValue = A(42)
            complete(aValue)
          }
        } ~ path("cow") {
          get {
            complete(aCow)
          }
        } ~ RoutesForLazyFormatsServer.contextualRoute
      }
    } ~ path("stream") {
      get {
        val (streamActor, source) = SourceActor.source[AnyRef].preMaterialize()
        system.eventStream.subscribe(streamActor, classOf[AppEvent])

        watcherActor ! streamActor

        implicit val StringMarshaller: ToEntityMarshaller[String] =
          Marshaller.stringMarshaller(`text/csv`)

        complete {
          source.collect {
            case AppEvent(msg) => msg
          }
        }
      }
    } ~ path("file") {
      get {
        onSuccess(generateFile()) { file =>
          getFromFile(file, `application/zip`)
        }
      }
    } ~ path("presence") {
      parameter('hide.as[Boolean] ? false) { hide =>
        val addr = Address("Madrid")
        val special = Special(SometimesNotThere(addr))

        implicit val serdesContext = SerdesContext(false, false, hide)

        get {
          //complete(special)
          complete(SometimesNotThere(addr))
        }
      }
    }
  } ~ path("boom") {
    complete {
      System.exit(0)
      StatusCodes.OK
    }
  } ~ path("prms") {
    dir {
      case (x, y) =>
        println(s"$x $y")
        complete(StatusCodes.OK)
    }
  } ~ path("ws") {
    val plansChangesStream = Source
      .repeat(())
      .delay(1 second)
      .map(_ => TextMessage(Source.single("hello")))
    get {
      extractRequest { request =>
        request.header[UpgradeToWebSocket] match {
          case Some(upgrade) =>
            complete(
              upgrade
                .handleMessagesWithSinkSource(Sink.ignore, plansChangesStream)
            )
          case _ =>
            complete(
              HttpResponse(
                StatusCodes.BadRequest,
                entity = "Invalid WS request"
              )
            )
        }
      }
    }
  }

  val bindingFuture: Future[ServerBinding] =
    Http().bindAndHandle(route, "localhost", 8080)

  def downloadExampleFile(): Future[List[String]] = Future {
    scala.io.Source
      .fromURL(
        "https://memory-beta.fandom.com/wiki/Ferengi_Rules_of_Acquisition"
      )
      .getLines
      .toList
  }

  def generateFile(): Future[File] = {
    downloadExampleFile().map { lines =>
      val tmpFile = File.createTempFile("generated", ".zip")
      val zipOutputStream = new ZipOutputStream(new FileOutputStream(tmpFile))
      zipOutputStream.putNextEntry(
        new ZipEntry("Ferengi_Rules_of_Acquisition.html")
      )

      lines.foreach { line =>
        val bytes = s"$line\n".toCharArray.map(_.toByte)
        zipOutputStream.write(bytes)
      }

      zipOutputStream.finish()
      zipOutputStream.close()

      println(s"Generated file: ${tmpFile.getName} (${tmpFile.length()} bytes)")

      tmpFile
    }
  }

}
