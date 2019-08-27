package com.github.dedkovva.accounts.repository

import com.github.dedkovva.accounts.domain.Account

trait Repository {
  def lockAccount(acountId: String): Boolean
  def unLockAccount(acountId: String): Boolean
  def addAccount(account: Account): Option[BigDecimal]
  def findAccount(accountId: String): Option[Account]
  def fetchAccount(accountId: String): Account
  def transferFunds(accountFromId: String, accountToId: String, amount: BigDecimal): Unit
  def updateAccount(account: Account): Unit
  def fetchAllAccounts(): Seq[Account]
}
