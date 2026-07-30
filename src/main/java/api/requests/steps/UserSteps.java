package api.requests.steps;

import api.models.*;
import api.requests.skeleton.Endpoint;
import api.requests.skeleton.requesters.ValidatedCrudRequester;
import api.specs.ResponseSpecs;
import api.specs.RequestSpecs;
import io.restassured.specification.RequestSpecification;

import java.util.List;

public class UserSteps {
    private final RequestSpecification userSpec;

    public UserSteps(String username, String password) {
        this.userSpec = RequestSpecs.authAsUserSpec(
                username,
                password
        );
    }

    public List<CreateAccountResponse> getAllAccounts() {
        return new ValidatedCrudRequester<CreateAccountResponse>(
                userSpec,
                Endpoint.CUSTOMER_ACCOUNTS,
                ResponseSpecs.requestReturnsOk()
        ).getAll(CreateAccountResponse[].class);
    }

    public CustomerProfileResponse getProfile() {
        return ProfileSteps.getProfile(userSpec);
    }

    public int createAccount() {
        return AccountSteps.createAccount(userSpec);
    }

    public AccountResponse getAccountById(int accountId) {
        return AccountSteps.getAccountById(userSpec, accountId);
    }

    public void prepareAccountForTransfer(int accountId) {
        AccountSteps.prepareAccountForTransfer(
                userSpec,
                accountId
        );
    }

    public TransferResponse transfer(TransferRequest transferRequest) {
        return new ValidatedCrudRequester<TransferResponse>(
                userSpec,
                Endpoint.TRANSFER,
                ResponseSpecs.requestReturnsOk()
        ).post(transferRequest);
    }
}
