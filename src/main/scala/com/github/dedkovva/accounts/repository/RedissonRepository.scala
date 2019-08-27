package com.github.dedkovva.accounts.repository

import java.util.concurrent.TimeUnit

import com.github.dedkovva.accounts.domain.Account
import com.github.dedkovva.accounts.util.EmbeddedRedisServer
import org.redisson.api.RMap
import org.redisson.config.Config
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import com.github.dedkovva.accounts.util.AppConfig.Redis.DistributedCache._
import org.redisson.Redisson
import org.redisson.api.RedissonClient

object RedissonRepository extends Repository {
  private val log = LoggerFactory.getLogger(this.getClass)

  private val redissonConfig = new Config()
  import EmbeddedRedisServer._
  redissonConfig.useSingleServer.setAddress(s"redis://$host:$port")

  private val redissonClient: RedissonClient = Redisson.create(redissonConfig)

  private val rMap: RMap[String, java.math.BigDecimal] = redissonClient.getMap("accounts-rest-api")

  override def lockAccount(accountId: String): Boolean = {
    doActionOnLock(accountId) {
      rMap.getLock(accountId).tryLock(waitTimeInSec, leaseTimeInSec, TimeUnit.SECONDS)
    }
  }

  override def unLockAccount(accountId: String): Boolean = {
    doActionOnLock(accountId) {
      rMap.getLock(accountId).unlock()
      true
    }
  }

  override def findAccount(accountId: String): Option[Account] = {
    if (rMap.containsKey(accountId)) Option(Account(accountId, BigDecimal(rMap.get(accountId)))) else None
  }

  override def transferFunds(accountFromId: String, accountToId: String, amount: BigDecimal): Unit = {
    val accountFrom = fetchAccount(accountFromId)
    val accountTo = fetchAccount(accountToId)
    rMap.put(accountFromId, (accountFrom.amount - amount).bigDecimal)
    rMap.put(accountToId, (accountTo.amount + amount).bigDecimal)
  }

  override def addAccount(account: Account): Option[BigDecimal] = {
    val prevValue = rMap.putIfAbsent(account.id, account.amount.bigDecimal)
    if (prevValue == null) {
      None
    } else {
      Option(BigDecimal(prevValue))
    }
  }

  override def fetchAccount(accountId: String): Account = {
    val amount = rMap.get(accountId)
    Account(accountId, amount)
  }

  override def updateAccount(account: Account): Unit = {
    rMap.put(account.id, account.amount.bigDecimal)
  }

  override def fetchAllAccounts(): Seq[Account] = {
    rMap.entrySet().asScala.toSeq.map(e => Account(e.getKey, e.getValue))
  }

  private def doActionOnLock(accountId: String)(action: => Boolean): Boolean = {
    try {
      if (rMap.containsKey(accountId)) {
        action
      } else {
        log.warn(s"Couldn't find account with id [$accountId]")
        false
      }
    } catch {
      case t: Throwable =>
        log.error(s"Error occurred while attempt to acquire lock on account [$accountId]", t)
        false
    }
  }
}
