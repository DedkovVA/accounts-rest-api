package com.github.dedkovva.accounts.rest.model

case class TransferFundsResult(failure: Option[TransferFundsFailure], success: Option[TransferFundsSuccess]) {
  assert(!(failure.isEmpty && success.isEmpty), "Failure and success are empty at the same time")
  assert(!(failure.nonEmpty && success.nonEmpty), "Failure and success are not empty at the same time")
}

object TransferFundsResult {
  def apply(failure: TransferFundsFailure): TransferFundsResult = {
    TransferFundsResult(Option(failure), None)
  }

  def apply(success: TransferFundsSuccess): TransferFundsResult = {
    TransferFundsResult(None, Option(success))
  }
}
