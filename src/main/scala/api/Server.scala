package api

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route

import scala.concurrent.ExecutionContextExecutor

object Server {
  def apply(route: Route, host: String, port: Int)(implicit system:  ActorSystem[_]) {
    implicit val executionContext: ExecutionContextExecutor = system.executionContext

    Http().newServerAt(host, port).bind(route)
  }
}
