package api.requests.steps;

import api.models.*;
import api.requests.skeleton.Endpoint;
import api.requests.skeleton.requesters.ValidatedCrudRequester;
import api.specs.ResponseSpecs;
import api.specs.RequestSpecs;
import common.helpers.StepLogger;
import io.qameta.allure.Step;
import io.restassured.specification.RequestSpecification;

import java.util.List;

public class UserSteps {

    private final RequestSpecification userSpec;

    public UserSteps(String username, String password) {
        this.userSpec = RequestSpecs.authAsUser(
                username,
                password
        );
    }

    @Step("Get all user accounts")
    public List<CreateAccountResponse> getAllAccounts() {
        return StepLogger.log("User gets all accounts", () ->
                new ValidatedCrudRequester<CreateAccountResponse>(
                        userSpec,
                        Endpoint.CUSTOMER_ACCOUNTS,
                        ResponseSpecs.requestReturnsOk()
                ).getAll(CreateAccountResponse[].class)
        );
    }

    @Step("Get user profile")
    public CustomerProfileResponse getProfile() {
        return ProfileSteps.getProfile(userSpec);
    }

    @Step("Create user account")
    public int createAccount() {
        return AccountSteps.createAccount(userSpec);
    }

    @Step("Get user account by id")
    public AccountResponse getAccountById(int accountId) {
        return AccountSteps.getAccountById(
                userSpec,
                accountId
        );
    }

    @Step("Prepare user account for transfer")
    public void prepareAccountForTransfer(int accountId) {
        AccountSteps.prepareAccountForTransfer(
                userSpec,
                accountId
        );
    }

    @Step("Transfer money")
    public TransferResponse transfer(
            TransferRequest transferRequest
    ) {
        return StepLogger.log("User performs money transfer", () ->
                new ValidatedCrudRequester<TransferResponse>(
                        userSpec,
                        Endpoint.TRANSFER,
                        ResponseSpecs.requestReturnsOk()
                ).post(transferRequest)
        );
    }
}
