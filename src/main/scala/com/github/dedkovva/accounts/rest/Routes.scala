package com.github.dedkovva.accounts.rest

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, Route}
import com.github.dedkovva.accounts.domain.Account
import com.github.dedkovva.accounts.repository.RedissonRepository
import com.github.dedkovva.accounts.service.Service
import org.slf4j.LoggerFactory

/**
  * Created by dedkov-va on 07.04.18.
  */
object Routes extends Directives with JsonTransformer {
  private val log = LoggerFactory.getLogger(Routes.getClass)

  private val service = new Service(RedissonRepository)

  val paths: Route = pathPrefix("accountsRestApi") {
    path("info") {
      get {
        complete(StatusCodes.OK)
      }
    } ~ path("addAccount") {
      post {
        entity(as[Account]) { account =>
          complete(service.addAccount(account))
        }
      }
    } ~ path("transferFunds") {
      put {
        parameters('accountFromId, 'accountToId, 'amount.as[Double]) { (accountFromId, accountToId, amount) =>
          complete(service.transferFunds(accountFromId, accountToId, amount))
        }
      }
    } ~ path("fetchAllAccounts") {
      get {
        complete(service.fetchAllAccounts())
      }
    } ~ path("findAccount" / Segment) { accountId =>
      get {
        complete(service.findAccount(accountId))
      }
    }
  }
}
