package com.jsoizo

import cats.effect._
import com.jsoizo.api.ApiRoute
import org.http4s.blaze.server.BlazeServerBuilder
import sttp.tapir.server.http4s.Http4sServerInterpreter

object LocalServerApp extends IOApp with ApiRoute {

  override def run(args: List[String]): IO[ExitCode] = {
    BlazeServerBuilder[IO]
      .bindHttp(8080, "localhost")
      .withHttpApp(Http4sServerInterpreter[IO]().toRoutes(pingEndPoint).orNotFound)
      .resource
      .use(_ => IO.never)
      .as(ExitCode.Success)
  }

}
