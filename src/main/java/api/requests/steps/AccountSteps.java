package api.requests.steps;

import common.helpers.StepLogger;
import io.restassured.common.mapper.TypeRef;
import io.restassured.specification.RequestSpecification;
import api.models.AccountResponse;
import api.models.CreateAccountResponse;
import api.models.DepositRequest;
import api.requests.skeleton.Endpoint;
import api.requests.skeleton.requesters.CrudRequester;
import api.requests.skeleton.requesters.ValidatedCrudRequester;
import api.specs.ResponseSpecs;
import io.qameta.allure.Step;

import java.util.List;

public class AccountSteps {

    @Step("Create account")
    public static int createAccount(RequestSpecification userSpec) {
        return StepLogger.log("User creates a new account", () -> {
            CreateAccountResponse account =
                    new ValidatedCrudRequester<CreateAccountResponse>(
                            userSpec,
                            Endpoint.ACCOUNTS,
                            ResponseSpecs.entityWasCreated()
                    ).post();

            return (int) account.getId();
        });
    }

    @Step("Get customer accounts")
    public static List<AccountResponse> getAccounts(RequestSpecification userSpec) {
        return StepLogger.log("User gets all customer accounts", () ->
                new CrudRequester(
                        userSpec,
                        Endpoint.CUSTOMER_ACCOUNTS,
                        ResponseSpecs.requestReturnsOk()
                )
                        .get()
                        .extract()
                        .as(new TypeRef<List<AccountResponse>>() {
                        })
        );
    }

    @Step("Get account by id")
    public static AccountResponse getAccountById(RequestSpecification userSpec, int accountId) {
        return StepLogger.log(
                "User gets account with id " + accountId,
                () -> getAccounts(userSpec)
                        .stream()
                        .filter(account -> account.getId() == accountId)
                        .findFirst()
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Account with id " + accountId + " was not found"
                                )
                        )
        );
    }

    @Step("Deposit money on account")
    public static void depositMoneyOnAccount(RequestSpecification userSpec, int accountId,
                                             float depositAmount, float expectedBalanceAfterDeposit) {
        StepLogger.log(
                "User deposits " + depositAmount
                        + " to account " + accountId,
                () -> {
                    DepositRequest depositRequest = DepositRequest.builder()
                            .id(accountId)
                            .balance(depositAmount)
                            .build();

                    new CrudRequester(
                            userSpec,
                            Endpoint.DEPOSIT,
                            ResponseSpecs.successfulDepositResponse(
                                    accountId,
                                    expectedBalanceAfterDeposit
                            )
                    ).post(depositRequest);
                }
        );
    }

    @Step("Prepare account for transfer")
    public static void prepareAccountForTransfer(RequestSpecification userSpec, int accountId) {
        StepLogger.log(
                "Prepare account " + accountId
                        + " for transfer with balance 15000",
                () -> {
                    float expectedBalance = 0f;

                    for (int i = 0; i < 3; i++) {
                        expectedBalance += 5000f;

                        depositMoneyOnAccount(
                                userSpec,
                                accountId,
                                5000f,
                                expectedBalance
                        );
                    }
                }
        );
    }

    @Step("Prepare account with small balance")
    public static void prepareAccountWithSmallBalance(RequestSpecification userSpec, int accountId) {
        StepLogger.log(
                "Prepare account " + accountId + " with balance 5000",
                () -> depositMoneyOnAccount(
                        userSpec,
                        accountId,
                        5000f,
                        5000f
                )
        );
    }
}