package com.github.dedkovva.accounts

import akka.http.scaladsl.model.{HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.model.ContentTypes._
import org.scalatest.{BeforeAndAfterEach, FreeSpec, Matchers}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.github.dedkovva.accounts.rest.model._
import com.github.dedkovva.accounts.domain.Account
import com.github.dedkovva.accounts.rest.{JsonTransformer, Routes}
import com.github.dedkovva.accounts.util.EmbeddedRedisServer
import org.slf4j.LoggerFactory

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.Random

class RoutesSpec extends FreeSpec with Matchers with ScalatestRouteTest with JsonTransformer with BeforeAndAfterEach {
  private val log = LoggerFactory.getLogger(this.getClass)

  override def beforeEach(): Unit = {
    EmbeddedRedisServer.start()
  }

  override def afterEach(): Unit = {
    EmbeddedRedisServer.stop()
  }

  private val api = "accountsRestApi"
  private val addAccount = s"/$api/addAccount"
  private val fetchAllAccounts = s"/$api/fetchAllAccounts"
  private def callTransferFunds(from: String, to: String, amount: Double): RouteTestResult =
    Put(s"/$api/transferFunds?accountFromId=$from&accountToId=$to&amount=$amount") ~> Routes.paths
  private implicit def accountToJson(account: Account): String = accountFormat.writes(account).toString()

  "add account spec" in {
    val account = Account("a", 100)

    Post(s"$addAccount", HttpEntity(`application/json`, account)) ~> Routes.paths ~> check {
      val response = responseAs[HttpResponse]
      response.status shouldBe StatusCodes.OK
    }

    Get(fetchAllAccounts) ~> Routes.paths ~> check {
      val accountsR = responseAs[Seq[Account]]
      accountsR.length shouldBe 1
      accountsR.head shouldBe account
    }

    Get(s"/$api/findAccount/a") ~> Routes.paths ~> check {
      val accountOption = responseAs[Option[Account]]
      accountOption shouldBe Option(account)
    }

    Get(s"/$api/findAccount/b") ~> Routes.paths ~> check {
      val accountOption = responseAs[Option[Account]]
      accountOption shouldBe None
    }
  }

  "transfer funds spec" in {
    val accountFrom = Account("a", 100)
    val accountTo = Account("b", 100)

    val accounts = Vector(accountFrom, accountTo)

    Post(s"$addAccount", HttpEntity(`application/json`, accountFrom)) ~> Routes.paths
    Post(s"$addAccount", HttpEntity(`application/json`, accountTo)) ~> Routes.paths

    callTransferFunds("a1", "b", 0) ~> check {
      val response = responseAs[TransferFundsResult]
      response.success shouldBe None
      response.failure shouldBe Option(TransferFundsFailure(
        wrongAmount = true,
        cannotFindAccount = CannotFindAccount(from = true)
      ))
    }

    callTransferFunds("a1", "a1", 0) ~> check {
      val response = responseAs[TransferFundsResult]
      response.success shouldBe None
      response.failure shouldBe Option(TransferFundsFailure(
        wrongAmount = true,
        cannotFindAccount = CannotFindAccount(from = true, to = true),
        cannotTransferFundsOneself = true
      ))
    }

    callTransferFunds("a", "b", 101) ~> check {
      val response = responseAs[TransferFundsResult]
      response.success shouldBe None
      response.failure shouldBe Option(TransferFundsFailure(
        notEnoughMoney = Option(100)
      ))
    }

    callTransferFunds("a", "b", 40) ~> check {
      val response = responseAs[TransferFundsResult]
      response.success shouldBe Option(TransferFundsSuccess(
        accountFrom = AccountUpd("a", 100, 60),
        accountTo = AccountUpd("b", 100, 140)
      ))
      response.failure shouldBe None
    }

    callTransferFunds("b", "a", 60) ~> check {
      val response = responseAs[TransferFundsResult]
      response.success shouldBe Option(TransferFundsSuccess(
        accountFrom = AccountUpd("b", 140, 80),
        accountTo = AccountUpd("a", 60, 120)
      ))
      response.failure shouldBe None
    }

    callTransferFunds("a", "b", 120) ~> check {
      val response = responseAs[TransferFundsResult]
      response.success shouldBe Option(TransferFundsSuccess(
        accountFrom = AccountUpd("a", 120, 0),
        accountTo = AccountUpd("b", 80, 200)
      ))
      response.failure shouldBe None
    }

    Get(fetchAllAccounts) ~> Routes.paths ~> check {
      val accountsR = responseAs[Seq[Account]]
      val updatedAccountsStr = accountsR.mkString(System.lineSeparator())
      log.info(
        s"""Updated accounts:
           |$updatedAccountsStr
         """.stripMargin)
      accountsR.forall(_.amount >= 0) shouldBe true
      accountsR.map(_.amount).sum shouldBe accounts.map(_.amount).sum
      accountsR.size shouldBe accounts.length
      accountsR.map(_.id).forall(accounts.map(_.id).contains) shouldBe true
    }
  }

  "concurrent transfers spec" in {
    val accounts = Vector(Account("a", 100), Account("b", 100), Account("c", 100), Account("d", 100))

    Post(s"$addAccount", HttpEntity(`application/json`, accounts(0))) ~> Routes.paths
    Post(s"$addAccount", HttpEntity(`application/json`, accounts(1))) ~> Routes.paths
    Post(s"$addAccount", HttpEntity(`application/json`, accounts(2))) ~> Routes.paths
    Post(s"$addAccount", HttpEntity(`application/json`, accounts(3))) ~> Routes.paths

    val accountIds = Vector("a", "b", "c", "d", "e")
    def randomId(): String = accountIds(Random.nextInt(accountIds.length))
    def randomAmount(): Double = Random.nextInt(150) + Random.nextDouble() - 10

    val transfers = for (i <- 0 to 100) yield {
      val rq = s"/$api/transferFunds?accountFromId=${randomId()}&accountToId=${randomId()}&amount=${randomAmount()}"
      Future {
        Put(rq) ~> Routes.paths
        log.info(s"num iter: $i, request: $rq")
      }
    }

    Await.result(Future.sequence(transfers), 5 minutes)

    Get(fetchAllAccounts) ~> Routes.paths ~> check {
      val accountsR = responseAs[Seq[Account]]
      val updatedAccountsStr = accountsR.mkString(System.lineSeparator())
      log.info(
        s"""Updated accounts:
           |$updatedAccountsStr
         """.stripMargin)
      accountsR.forall(_.amount >= 0) shouldBe true
      accountsR.map(_.amount).sum shouldBe accounts.map(_.amount).sum
      accountsR.size shouldBe accounts.length
      accountsR.map(_.id).forall(accountIds.contains) shouldBe true
    }
  }
}
