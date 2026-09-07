package request.steps;

import io.restassured.specification.RequestSpecification;
import models.AccountResponse;
import models.DepositRequest;
import request.CreateAccountRequester;
import request.CustomerAccountsRequester;
import request.DepositRequester;
import specs.ResponseSpecs;

import java.util.List;

import static TestData.AccountTestData.EMPTY_ACCOUNT_BALANCE;
import static TestData.AccountTestData.PREPARED_SENDER_BALANCE;
import static TestData.AccountTestData.SMALL_SENDER_BALANCE;

public final class AccountSteps {

    private AccountSteps() {
    }

    public static int createAccount(
            RequestSpecification userSpec
    ) {
        return new CreateAccountRequester(
                userSpec,
                ResponseSpecs.entityWasCreated()
        )
                .post(null)
                .extract()
                .path("id");
    }

    public static List<AccountResponse> getAccounts(
            RequestSpecification userSpec
    ) {
        return new CustomerAccountsRequester(
                userSpec,
                ResponseSpecs.requestReturnsOk()
        ).getAccounts();
    }

    public static AccountResponse getAccountById(
            RequestSpecification userSpec,
            int accountId
    ) {
        return getAccounts(userSpec)
                .stream()
                .filter(account -> account.getId() == accountId)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Account with id " + accountId + " was not found"
                ));
    }

    public static void depositMoneyOnAccount(
            RequestSpecification userSpec,
            int accountId,
            float depositAmount,
            float expectedBalanceAfterDeposit
    ) {
        DepositRequest depositRequest = DepositRequest.builder()
                .id(accountId)
                .balance(depositAmount)
                .build();

        new DepositRequester(
                userSpec,
                ResponseSpecs.successfulDepositResponse(
                        accountId,
                        expectedBalanceAfterDeposit
                )
        ).post(depositRequest);
    }

    public static void prepareAccountForTransfer(RequestSpecification userSpec, int accountId) {
        float expectedBalance = EMPTY_ACCOUNT_BALANCE;

        while (expectedBalance < PREPARED_SENDER_BALANCE) {
            expectedBalance += SMALL_SENDER_BALANCE;

            depositMoneyOnAccount(
                    userSpec,
                    accountId,
                    SMALL_SENDER_BALANCE,
                    expectedBalance
            );
        }
    }

    public static void prepareAccountWithSmallBalance(
            RequestSpecification userSpec,
            int accountId
    ) {
        depositMoneyOnAccount(
                userSpec,
                accountId,
                SMALL_SENDER_BALANCE,
                SMALL_SENDER_BALANCE
        );
    }
}
