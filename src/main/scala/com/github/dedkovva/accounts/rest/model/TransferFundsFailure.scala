package com.github.dedkovva.accounts.rest.model

case class TransferFundsFailure(wrongAmount: Boolean = false,
                                cannotFindAccount: CannotFindAccount = CannotFindAccount(),
                                cannotLockAccount: CannotLockAccount = CannotLockAccount(),
                                cannotTransferFundsOneself: Boolean = false,
                                notEnoughMoney: Option[BigDecimal] = None,
                                error: Boolean = false) {
  def isFail: Boolean = wrongAmount ||
    cannotFindAccount.from || cannotFindAccount.to ||
    cannotLockAccount.from || cannotLockAccount.to ||
    cannotTransferFundsOneself || notEnoughMoney.nonEmpty || error
}
