package iteration2.api;

import generators.RandomModelGenerator;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.AccountResponse;
import models.DepositRequest;
import models.InvalidDepositRequest;
import models.comparison.ModelAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.skeleton.Endpoint;
import requests.skeleton.requesters.CrudRequester;
import requests.skeleton.requesters.ValidatedCrudRequester;
import requests.steps.AccountSteps;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.stream.Stream;

import static assertions.AccountAssertions.assertAccountAfterSuccessfulDeposit;
import static assertions.AccountAssertions.assertAccountIsEmpty;
import static factories.DepositRequestFactory.depositRequestWithAmount;
import static factories.DepositRequestFactory.validDepositRequest;

public class DepositTests extends BaseTest {
    @Test
    public void userCanDepositWithRandomCorrectAmount() {
        RequestSpecification userSpec = createUserSpecForTest();
        int accountId = AccountSteps.createAccount(userSpec);

        assertAccountIsEmpty(softy, userSpec, accountId);

        DepositRequest depositRequest = validDepositRequest(accountId);

        AccountResponse depositResponse = new ValidatedCrudRequester<AccountResponse>(
                userSpec,
                Endpoint.DEPOSIT,
                ResponseSpecs.requestReturnsOk()
        ).post(depositRequest);

        ModelAssertions.assertThatModels(depositRequest, depositResponse).match();

        assertAccountAfterSuccessfulDeposit(softy, userSpec, depositRequest);
    }

    public static Stream<Arguments> correctBoundaryDepositData() {
        return Stream.of(
                Arguments.of(0.01f),
                Arguments.of(4999.99f),
                Arguments.of(5000f)
        );
    }

    @MethodSource("correctBoundaryDepositData")
    @ParameterizedTest
    public void userCanDepositWithBoundaryCorrectAmount(float depositAmount) {
        RequestSpecification userSpec = createUserSpecForTest();
        int accountId = AccountSteps.createAccount(userSpec);

        assertAccountIsEmpty(softy, userSpec, accountId);

        DepositRequest depositRequest = depositRequestWithAmount(accountId, depositAmount);

        AccountResponse depositResponse = new ValidatedCrudRequester<AccountResponse>(
                userSpec,
                Endpoint.DEPOSIT,
                ResponseSpecs.requestReturnsOk()
        ).post(depositRequest);

        ModelAssertions.assertThatModels(depositRequest, depositResponse).match();

        assertAccountAfterSuccessfulDeposit(softy, userSpec, depositRequest);
    }

    public static Stream<Arguments> incorrectBoundaryDepositData() {
        return Stream.of(
                Arguments.of(0f, ResponseSpecs.depositAmountLessThanMin()),
                Arguments.of(5000.01f, ResponseSpecs.depositAmountMoreThanMax())
        );
    }

    @MethodSource("incorrectBoundaryDepositData")
    @ParameterizedTest
    public void userCannotDepositWithBoundaryIncorrectAmount(float depositAmount,
                                                             ResponseSpecification responseSpecification) {
        RequestSpecification userSpec = createUserSpecForTest();
        int accountId = AccountSteps.createAccount(userSpec);

        assertAccountIsEmpty(softy, userSpec, accountId);

        DepositRequest depositRequest = depositRequestWithAmount(accountId, depositAmount);

        new CrudRequester(
                userSpec,
                Endpoint.DEPOSIT,
                responseSpecification
        ).post(depositRequest);

        assertAccountIsEmpty(softy, userSpec, accountId);
    }

    public static Stream<Arguments> incorrectRandomDepositData() {
        return Stream.of(
                Arguments.of(
                        RandomModelGenerator.generateNegativeDepositAmount(),
                        ResponseSpecs.depositAmountLessThanMin()
                ),
                Arguments.of(
                        RandomModelGenerator.generateDepositAmountMoreThanMax(),
                        ResponseSpecs.depositAmountMoreThanMax()
                )
        );
    }

    @MethodSource("incorrectRandomDepositData")
    @ParameterizedTest
    public void userCannotDepositWithRandomIncorrectAmount(float depositAmount,
                                                           ResponseSpecification responseSpecification) {
        RequestSpecification userSpec = createUserSpecForTest();
        int accountId = AccountSteps.createAccount(userSpec);

        assertAccountIsEmpty(softy, userSpec, accountId);

        DepositRequest depositRequest = depositRequestWithAmount(accountId, depositAmount);

        new CrudRequester(
                userSpec,
                Endpoint.DEPOSIT,
                responseSpecification
        ).post(depositRequest);

        assertAccountIsEmpty(softy, userSpec, accountId);
    }

    @Test
    public void userCannotDepositWithNonExistingAccount() {
        RequestSpecification userSpec = createUserSpecForTest();
        int accountId = AccountSteps.createAccount(userSpec);

        assertAccountIsEmpty(softy, userSpec, accountId);

        int nonExistingAccountId =
                RandomModelGenerator.generateNonExistingAccountIdBasedOn(accountId);

        DepositRequest depositRequest = validDepositRequest(nonExistingAccountId);

        new CrudRequester(
                userSpec,
                Endpoint.DEPOSIT,
                ResponseSpecs.unauthorizedAccessToAccount()
        ).post(depositRequest);

        assertAccountIsEmpty(softy, userSpec, accountId);
    }

    @Test
    public void userCannotDepositToAnotherUserAccount() {
        RequestSpecification firstUserSpec = createUserSpecForTest();

        RequestSpecification secondUserSpec = createUserSpecForTest();
        int secondUserAccountId = AccountSteps.createAccount(secondUserSpec);

        assertAccountIsEmpty(softy, secondUserSpec, secondUserAccountId);

        DepositRequest depositRequest = validDepositRequest(secondUserAccountId);

        new CrudRequester(
                firstUserSpec,
                Endpoint.DEPOSIT,
                ResponseSpecs.unauthorizedAccessToAccount()
        ).post(depositRequest);

        assertAccountIsEmpty(softy, secondUserSpec, secondUserAccountId);
    }

    @Test
    public void userCannotDepositWithStringIdInBody() {
        RequestSpecification userSpec = createUserSpecForTest();
        int accountId = AccountSteps.createAccount(userSpec);

        assertAccountIsEmpty(softy, userSpec, accountId);

        InvalidDepositRequest depositRequest = InvalidDepositRequest.builder()
                .id(RandomModelGenerator.generateStringValue())
                .balance(RandomModelGenerator.generateValidDepositAmount())
                .build();

        new CrudRequester(
                userSpec,
                Endpoint.DEPOSIT,
                ResponseSpecs.internalServerErrorForEndpoint(Endpoint.DEPOSIT)
        ).post(depositRequest);

        assertAccountIsEmpty(softy, userSpec, accountId);
    }

    @Test
    public void userCannotDepositWithStringBalanceInBody() {
        RequestSpecification userSpec = createUserSpecForTest();
        int accountId = AccountSteps.createAccount(userSpec);

        assertAccountIsEmpty(softy, userSpec, accountId);

        InvalidDepositRequest depositRequest = InvalidDepositRequest.builder()
                .id(accountId)
                .balance(RandomModelGenerator.generateStringValue())
                .build();

        new CrudRequester(
                userSpec,
                Endpoint.DEPOSIT,
                ResponseSpecs.internalServerErrorForEndpoint(Endpoint.DEPOSIT)
        ).post(depositRequest);

        assertAccountIsEmpty(softy, userSpec, accountId);
    }

    @Test
    public void userCannotDepositWithoutIdInBody() {
        RequestSpecification userSpec = createUserSpecForTest();
        int accountId = AccountSteps.createAccount(userSpec);

        assertAccountIsEmpty(softy, userSpec, accountId);

        InvalidDepositRequest depositRequest = InvalidDepositRequest.builder()
                .balance(RandomModelGenerator.generateValidDepositAmount())
                .build();

        new CrudRequester(
                userSpec,
                Endpoint.DEPOSIT,
                ResponseSpecs.internalServerErrorForEndpoint(Endpoint.DEPOSIT)
        ).post(depositRequest);

        assertAccountIsEmpty(softy, userSpec, accountId);
    }

    @Test
    public void userCannotDepositWithoutBalanceInBody() {
        RequestSpecification userSpec = createUserSpecForTest();
        int accountId = AccountSteps.createAccount(userSpec);

        assertAccountIsEmpty(softy, userSpec, accountId);

        InvalidDepositRequest depositRequest = InvalidDepositRequest.builder()
                .id(accountId)
                .build();

        new CrudRequester(
                userSpec,
                Endpoint.DEPOSIT,
                ResponseSpecs.internalServerErrorForEndpoint(Endpoint.DEPOSIT)
        ).post(depositRequest);

        assertAccountIsEmpty(softy, userSpec, accountId);
    }

    @Test
    public void userCannotDepositWithWrongAuthorizationToken() {
        RequestSpecification userSpec = createUserSpecForTest();
        int accountId = AccountSteps.createAccount(userSpec);

        assertAccountIsEmpty(softy, userSpec, accountId);

        DepositRequest depositRequest = validDepositRequest(accountId);

        new CrudRequester(
                RequestSpecs.brokenAuthSpec(),
                Endpoint.DEPOSIT,
                ResponseSpecs.unauthorized()
        ).post(depositRequest);

        assertAccountIsEmpty(softy, userSpec, accountId);
    }

    @Test
    public void userCannotDepositWithoutAuthorization() {
        RequestSpecification userSpec = createUserSpecForTest();
        int accountId = AccountSteps.createAccount(userSpec);

        assertAccountIsEmpty(softy, userSpec, accountId);

        DepositRequest depositRequest = validDepositRequest(accountId);

        new CrudRequester(
                RequestSpecs.unauthSpec(),
                Endpoint.DEPOSIT,
                ResponseSpecs.unauthorized()
        ).post(depositRequest);

        assertAccountIsEmpty(softy, userSpec, accountId);
    }
}

