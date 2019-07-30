package org.pfcoperez.webserver

import akka.actor.ActorRef
import akka.stream.OverflowStrategy
import akka.stream.scaladsl._

object SourceActor {

  def source[T]: Source[T, ActorRef] = Source.actorRef[T](
    bufferSize = 200, // No elements
    overflowStrategy = OverflowStrategy.dropNew
  )

}
