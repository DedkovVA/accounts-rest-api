package com.github.dedkovva.accounts.util

import redis.embedded.RedisServer

object EmbeddedRedisServer {
  val host: String = AppConfig.Redis.host
  val port: Int = AppConfig.Redis.port

  private lazy val server = new RedisServer(port)

  def start(): Unit = {
    server.start()
  }

  def stop(): Unit = {
    server.stop()
  }
}
