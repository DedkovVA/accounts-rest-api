package com.github.dedkovva.accounts.domain

case class Account(id: String, amount: BigDecimal) {
  assert(id.trim.nonEmpty, "Empty account id")
}
