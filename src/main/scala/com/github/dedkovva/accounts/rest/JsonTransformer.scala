package com.github.dedkovva.accounts.rest

import com.github.dedkovva.accounts.rest.model._
import com.github.dedkovva.accounts.domain.Account
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import play.api.libs.json.Json


/**
  * Created by dedkov-va on 12.04.18.
  */
trait JsonTransformer extends PlayJsonSupport {
  implicit val accountFormat = Json.format[Account]
  implicit val accountUpdFormat = Json.format[AccountUpd]
  implicit val transferFundsSuccessFormat = Json.format[TransferFundsSuccess]
  implicit val cannotFindAccountFormat = Json.format[CannotFindAccount]
  implicit val cannotLockAccountFormat = Json.format[CannotLockAccount]
  implicit val transferFundsFailureFormat = Json.format[TransferFundsFailure]
  implicit val transferFundsResultFormat = Json.format[TransferFundsResult]
}
