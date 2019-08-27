package com.github.dedkovva.accounts.service

import com.github.dedkovva.accounts.rest.model._
import com.github.dedkovva.accounts.domain.Account
import com.github.dedkovva.accounts.repository.Repository
import org.slf4j.LoggerFactory

class Service(repository: Repository) {
  private val log = LoggerFactory.getLogger(this.getClass)

  private implicit def failureToResult(failure: TransferFundsFailure) = TransferFundsResult(failure)
  private implicit def successToResult(success: TransferFundsSuccess) = TransferFundsResult(success)

  def transferFunds(accountFromId: String, accountToId: String, amount: BigDecimal): TransferFundsResult = {
    val transferFundsFailure: TransferFundsFailure = checkBeforeLock(accountFromId, accountToId, amount)

    if (transferFundsFailure.isFail) {
      transferFundsFailure
    } else {
      lockAndTransferFunds(accountFromId, accountToId, amount)
    }
  }

  private def checkBeforeLock(accountFromId: String, accountToId: String, amount: BigDecimal): TransferFundsFailure = {
    val wrongAmount = amount <= 0
    if (wrongAmount) {
      log.warn(
        s"""Wrong amount [$amount] while transferring from [$accountFromId] to [$accountToId].
           |Amount should be only a positive number
         """.stripMargin)
    }

    val cannotTransferFundsOneself = accountFromId == accountToId
    if (cannotTransferFundsOneself) {
      log.warn(s"The same account [$accountFromId] for transferring [$amount]")
    }

    val accountFromOption = repository.findAccount(accountFromId)
    val accountToOption = repository.findAccount(accountToId)
    val cannotFindAccount =
      CannotFindAccount(from = accountFromOption.isEmpty, to = accountToOption.isEmpty)
    if (cannotFindAccount.from || cannotFindAccount.to) {
      log.warn(
        s"""Cannot find account(s).
           |Account 'from': [$accountFromId]/[$accountFromOption], account 'to': [$accountToId]/[$accountToOption].
           |Amount: [$amount].""".stripMargin)
    }

    TransferFundsFailure(
      wrongAmount = wrongAmount,
      cannotTransferFundsOneself = cannotTransferFundsOneself,
      cannotFindAccount = cannotFindAccount)
  }

  private def lockAndTransferFunds(accountFromId: String, accountToId: String, amount: BigDecimal): TransferFundsResult = {
    var lockFrom = false; var lockTo = false

    try {
      lockFrom = repository.lockAccount(accountFromId)
      lockTo = repository.lockAccount(accountToId)

      (lockFrom, lockTo) match {
        case (true, true) =>
          val accountFrom = repository.fetchAccount(accountFromId)
          val accountTo = repository.fetchAccount(accountToId)

          if (accountFrom.amount < amount) {
            log.warn(s"Not enough money on account [$accountFrom] for transferring [$amount] on account [$accountTo]")
            TransferFundsFailure(notEnoughMoney = Option(accountFrom.amount))
          } else {
            transferFundsWithResult(accountFrom, accountTo, amount)
          }
        case _ =>
          log.warn(
            s"""Cannot lock account(s).
               |Account 'from': [$accountFromId], account 'to': [$accountToId]. Amount: [$amount].
               |Lock 'from': [$lockFrom], lock 'to': [$lockTo]""".stripMargin)
          TransferFundsFailure(cannotLockAccount = CannotLockAccount(from = !lockFrom, to = !lockTo))
      }
    } finally {
      unLockAccount(accountFromId, lockFrom)
      unLockAccount(accountToId, lockTo)
    }
  }

  private def transferFundsWithResult(accountFrom: Account, accountTo: Account, amount: BigDecimal): TransferFundsResult = {
    val success = transferFundsAndRevertIfFailure(accountFrom, accountTo, amount)

    if (success) {
      val accountFromUpd = repository.fetchAccount(accountFrom.id)
      val accountToUpd = repository.fetchAccount(accountTo.id)

      TransferFundsSuccess(
        AccountUpd(accountFrom.id, accountFrom.amount, accountFromUpd.amount),
        AccountUpd(accountTo.id, accountTo.amount, accountToUpd.amount)
      )
    } else {
      TransferFundsFailure(error = true)
    }
  }

  private def transferFundsAndRevertIfFailure(accountFrom: Account, accountTo: Account, amount: BigDecimal): Boolean = {
    try {
      repository.transferFunds(accountFrom.id, accountTo.id, amount)
      log.info(s"Funds [$amount] were transferred successfully " +
        s"from account [$accountFrom] to account [$accountTo]")
      true
    } catch {
      case t: Throwable =>
        log.error(
          s"""Error occurred while transfer amount [$amount]
             |from account [$accountFrom] to account [$accountTo].
             |Will try to revert operation""".stripMargin, t)
        revertFunds(accountFrom, "'from'")
        revertFunds(accountFrom, "'to'")
        false
    }
  }

  private def unLockAccount(accountId: String, locked: Boolean): Unit = {
    if (locked) {
      try {
        repository.unLockAccount(accountId)
      } catch {
        case t: Throwable =>
          log.error(s"Cannot unlock lock for accountId = [$accountId]", t)
      }
    }
  }

  private def revertFunds(account: Account, direction: String): Unit = {
    try {
      repository.updateAccount(account)
      log.info(s"Account $direction reverted successfully to initial value: [$account]")
    } catch {
      case t: Throwable =>
        log.error(s"Couldn't revert funds for account $direction [$account]", t)
    }
  }

  /**@return Some(_) if account with such id already exists*/
  def addAccount(account: Account): Option[BigDecimal] = {
    repository.addAccount(account)
  }

  def fetchAllAccounts(): Seq[Account] = {
    repository.fetchAllAccounts()
  }

  def findAccount(id: String): Option[Account] = {
    repository.findAccount(id)
  }
}
