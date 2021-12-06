package com.jsoizo.api

import cats.effect.IO
import sttp.tapir._
import sttp.tapir.server.ServerEndpoint

trait ApiRoute {
  val pingEndPoint: ServerEndpoint[Any, IO] = endpoint.get
    .in("api" / "ping")
    .out(stringBody)
    .serverLogicSuccess(_ => IO.pure("pong!"))
}
