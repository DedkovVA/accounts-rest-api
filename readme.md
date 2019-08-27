1. Open cmd line
2. cd in root project directory `accounts-rest-api`
3. print command `sbt run`
4. send REST requests to `http://localhost:9085/accountsRestApi`:

    4.1. Add account
    * request:
        * example:
        
            ```
            curl --header "Content-Type: application/json" --request POST --data '{"id":"accountA","amount":100.5}' http://localhost:9085/accountsRestApi/addAccount
            ```
      
        * explanation:
        
          `--data` of type `Account`
    
    * response:
    
        nothing if added or value of already existing account with the same Id

    4.2. Fetch all accounts:
    * request:
        * example:
        
            ```
            curl --request GET http://localhost:9085/accountsRestApi/fetchAllAccounts
            ```
    
    * response:
        * example:
            ```
            [ {
              "id" : "accountA",
              "amount" : 100.5
            }, {
              "id" : "accountB",
              "amount" : 100
            } ]
            ```
        * explanation:
        
            Response of type `Array[Account]`
            
    4.3. Find account by Id
    
    * request:
        * example:
            ```
            curl --request GET http://localhost:9085/accountsRestApi/findAccount/accountA
            ```
    * response:
        * example:
            ```
            {
              "id" : "accountA",
              "amount" : 100.5
            }
            ```
        * explanation:
            
            response of type `[Option[Account]]`: `Account` if found or nothing
    
    4.4. Transfer funds between accounts:
    
    * request:
        * example:
            ```
            curl --request PUT "http://localhost:9085/accountsRestApi/transferFunds?accountFromId=accountA&accountToId=accountB&amount=100"
            ```
        * explanation: keep `amount` positive and `accountFromId` different from `accountToId`
    * response:
        * example:
            ```
            {
              "success" : {
                "accountFrom" : {
                  "id" : "accountA",
                  "amountBefore" : 100.5,
                  "amountAfter" : 0.5
                },
                "accountTo" : {
                  "id" : "accountB",
                  "amountBefore" : 100,
                  "amountAfter" : 200
                }
              }
            }
            ```
        * explanation:
        
            response of type `TransferFundsResult`

5. JSON types

    5.1. Request JSON types:
    ```
    Account(id: String, amount: Double)
    ```
    5.2. Response JSON types:
    ```
    TransferFundsResult(
        failure: Option[TransferFundsFailure], 
        success: Option[TransferFundsSuccess]
    )
    
    TransferFundsFailure(
        wrongAmount: Boolean,
        cannotFindAccount: CannotFindAccount,
        cannotLockAccount: CannotLockAccount,
        cannotTransferFundsOneself: Boolean,
        notEnoughMoney: Option[Double],
        error: Boolean
    )
    CannotFindAccount(from: Boolean, to: Boolean)
    CannotLockAccount(from: Boolean, to: Boolean)
                                
    TransferFundsSuccess(accountFrom: AccountUpd, accountTo: AccountUpd)
    AccountUpd(id: String, amountBefore: Double, amountAfter: Double)
    ```



