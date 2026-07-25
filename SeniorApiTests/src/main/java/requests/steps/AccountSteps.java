package requests.steps;

import io.restassured.common.mapper.TypeRef;
import io.restassured.specification.RequestSpecification;
import models.AccountResponse;
import models.CreateAccountResponse;
import models.DepositRequest;
import requests.skeleton.Endpoint;
import requests.skeleton.requesters.CrudRequester;
import requests.skeleton.requesters.ValidatedCrudRequester;
import specs.ResponseSpecs;
import io.qameta.allure.Step;

import java.util.List;

public class AccountSteps {
    @Step("Create account")
    public static int createAccount(RequestSpecification userSpec) {
        CreateAccountResponse account = new ValidatedCrudRequester<CreateAccountResponse>(
                userSpec,
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated()
        ).post();

        return (int) account.getId();
    }

    @Step("Get account by id")
    public static List<AccountResponse> getAccounts(RequestSpecification userSpec) {
        return new CrudRequester(
                userSpec,
                Endpoint.CUSTOMER_ACCOUNTS,
                ResponseSpecs.requestReturnsOk()
        )
                .get()
                .extract()
                .as(new TypeRef<List<AccountResponse>>() {});
    }

    @Step("Get customer accounts")
    public static AccountResponse getAccountById(RequestSpecification userSpec, int accountId) {
        return getAccounts(userSpec)
                .stream()
                .filter(account -> account.getId() == accountId)
                .findFirst()
                .orElseThrow();
    }

    public static void depositMoneyOnAccount(RequestSpecification userSpec,
                                             int accountId,
                                             float depositAmount,
                                             float expectedBalanceAfterDeposit) {
        DepositRequest depositRequest = DepositRequest.builder()
                .id(accountId)
                .balance(depositAmount)
                .build();

        new CrudRequester(
                userSpec,
                Endpoint.DEPOSIT,
                ResponseSpecs.successfulDepositResponse(accountId, expectedBalanceAfterDeposit)
        ).post(depositRequest);
    }

    @Step("Prepare account for transfer")
    public static void prepareAccountForTransfer(
            RequestSpecification userSpec,
            int accountId
    ) {
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

    @Step("Prepare account with small balance")
    public static void prepareAccountWithSmallBalance(RequestSpecification userSpec, int accountId) {
        depositMoneyOnAccount(userSpec, accountId, 5000f, 5000f);
    }
}
