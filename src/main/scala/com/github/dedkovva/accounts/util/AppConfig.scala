package com.github.dedkovva.accounts.util

import com.typesafe.config.ConfigFactory

/**
  * Created by dedkov-va on 07.04.18.
  */
object AppConfig {
  val config = ConfigFactory.load()

  object Http {
    private val http = config.getConfig("http")

    val host = http.getString("host")
    val port = http.getInt("port")
  }
  object Redis {
    private val redis = config.getConfig("redis")
    private val distributedCache = redis.getConfig("distributedCache")

    val host = redis.getString("host")
    val port = redis.getInt("port")

    object DistributedCache {
      val leaseTimeInSec = distributedCache.getInt("leaseTimeInSec")
      val waitTimeInSec = distributedCache.getInt("waitTimeInSec")
    }
  }
}
