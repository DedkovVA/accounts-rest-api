package com.github.dedkovva.accounts

import akka.http.scaladsl.server._
import akka.http.scaladsl.Http
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.dedkovva.accounts.rest.Routes
import com.github.dedkovva.accounts.util.{AppConfig, EmbeddedRedisServer}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

/**
  * Created by dedkov-va on 07.04.18.
  */
object Boot extends App {

  EmbeddedRedisServer.start()

  private implicit val system: ActorSystem = ActorSystem("accounts-rest-api", AppConfig.config)
  private implicit val materializer: ActorMaterializer = ActorMaterializer()

  private val routes = Route.handlerFlow(Routes.paths)

  private val http = Http(system)

  http.bindAndHandle(routes, AppConfig.Http.host, AppConfig.Http.port)
}
