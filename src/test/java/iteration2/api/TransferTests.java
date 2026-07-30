package iteration2.api;

import api.generators.RandomModelGenerator;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import api.models.InvalidTransferRequest;
import api.models.TransferRequest;
import api.models.TransferResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import api.requests.skeleton.Endpoint;
import api.requests.skeleton.requesters.CrudRequester;
import api.requests.skeleton.requesters.ValidatedCrudRequester;
import api.requests.steps.AccountSteps;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;

import java.util.stream.Stream;

import static api.assertions.AccountAssertions.assertAccountBalance;
import static api.assertions.TransferAssertions.assertSuccessfulTransferResponse;
import static api.factories.TransferRequestFactory.transferRequest;
import static api.testdata.AccountTestData.EMPTY_ACCOUNT_BALANCE;
import static api.testdata.AccountTestData.PREPARED_SENDER_BALANCE;
import static api.testdata.AccountTestData.SMALL_SENDER_BALANCE;

public class TransferTests extends BaseTest {
    @Test
    public void userCanTransferToAnotherUserAccountWithRandomCorrectAmount() {
        RequestSpecification senderUserSpec = createUserSpecForTest();
        int senderAccountId = AccountSteps.createAccount(senderUserSpec);
        AccountSteps.prepareAccountForTransfer(
                senderUserSpec,
                senderAccountId
        );

        RequestSpecification receiverUserSpec = createUserSpecForTest();
        int receiverAccountId =
                AccountSteps.createAccount(receiverUserSpec);

        float amount =
                RandomModelGenerator.generateValidTransferAmount();

        float senderExpectedBalance =
                PREPARED_SENDER_BALANCE - amount;

        float receiverExpectedBalance =
                EMPTY_ACCOUNT_BALANCE + amount;

        TransferRequest transferRequest = transferRequest(
                senderAccountId,
                receiverAccountId,
                amount
        );

        TransferResponse transferResponse =
                new ValidatedCrudRequester<TransferResponse>(
                        senderUserSpec,
                        Endpoint.TRANSFER,
                        ResponseSpecs.requestReturnsOk()
                ).post(transferRequest);

        assertSuccessfulTransferResponse(
                softy,
                transferRequest,
                transferResponse
        );

        assertAccountBalance(
                softy,
                senderUserSpec,
                senderAccountId,
                senderExpectedBalance
        );

        assertAccountBalance(
                softy,
                receiverUserSpec,
                receiverAccountId,
                receiverExpectedBalance
        );
    }

    public static Stream<Arguments> correctBoundaryTransferData() {
        return Stream.of(
                Arguments.of(0.01f),
                Arguments.of(9999.99f),
                Arguments.of(10000f)
        );
    }

    @MethodSource("correctBoundaryTransferData")
    @ParameterizedTest
    public void userCanTransferToAnotherUserAccountWithBoundaryCorrectAmount(
            float amount
    ) {
        RequestSpecification senderUserSpec = createUserSpecForTest();
        int senderAccountId = AccountSteps.createAccount(senderUserSpec);

        AccountSteps.prepareAccountForTransfer(
                senderUserSpec,
                senderAccountId
        );

        RequestSpecification receiverUserSpec = createUserSpecForTest();
        int receiverAccountId =
                AccountSteps.createAccount(receiverUserSpec);

        float senderExpectedBalance =
                PREPARED_SENDER_BALANCE - amount;

        float receiverExpectedBalance =
                EMPTY_ACCOUNT_BALANCE + amount;

        TransferRequest transferRequest = transferRequest(
                senderAccountId,
                receiverAccountId,
                amount
        );

        TransferResponse transferResponse =
                new ValidatedCrudRequester<TransferResponse>(
                        senderUserSpec,
                        Endpoint.TRANSFER,
                        ResponseSpecs.requestReturnsOk()
                ).post(transferRequest);

        assertSuccessfulTransferResponse(
                softy,
                transferRequest,
                transferResponse
        );

        assertAccountBalance(
                softy,
                senderUserSpec,
                senderAccountId,
                senderExpectedBalance
        );

        assertAccountBalance(
                softy,
                receiverUserSpec,
                receiverAccountId,
                receiverExpectedBalance
        );
    }

    @Test
    public void userCanTransferBetweenOwnAccountsWithRandomCorrectAmount() {
        RequestSpecification userSpec = createUserSpecForTest();

        int senderAccountId =
                AccountSteps.createAccount(userSpec);

        int receiverAccountId =
                AccountSteps.createAccount(userSpec);

        AccountSteps.prepareAccountForTransfer(
                userSpec,
                senderAccountId
        );

        float amount =
                RandomModelGenerator.generateValidTransferAmount();

        float senderExpectedBalance =
                PREPARED_SENDER_BALANCE - amount;

        float receiverExpectedBalance =
                EMPTY_ACCOUNT_BALANCE + amount;

        TransferRequest transferRequest = transferRequest(
                senderAccountId,
                receiverAccountId,
                amount
        );

        TransferResponse transferResponse =
                new ValidatedCrudRequester<TransferResponse>(
                        userSpec,
                        Endpoint.TRANSFER,
                        ResponseSpecs.requestReturnsOk()
                ).post(transferRequest);

        assertSuccessfulTransferResponse(
                softy,
                transferRequest,
                transferResponse
        );

        assertAccountBalance(
                softy,
                userSpec,
                senderAccountId,
                senderExpectedBalance
        );

        assertAccountBalance(
                softy,
                userSpec,
                receiverAccountId,
                receiverExpectedBalance
        );
    }

    public static Stream<Arguments> incorrectBoundaryTransferAmountData() {
        return Stream.of(
                Arguments.of(0f, ResponseSpecs.transferAmountLessThanMin()),
                Arguments.of(10000.01f, ResponseSpecs.transferAmountMoreThanMax())
        );
    }

    @MethodSource("incorrectBoundaryTransferAmountData")
    @ParameterizedTest
    public void userCannotTransferWithBoundaryIncorrectAmount(float amount,
                                                              ResponseSpecification responseSpecification) {
        RequestSpecification userSpec = createUserSpecForTest();

        int senderAccountId = AccountSteps.createAccount(userSpec);
        int receiverAccountId = AccountSteps.createAccount(userSpec);

        AccountSteps.prepareAccountWithSmallBalance(userSpec, senderAccountId);

        TransferRequest transferRequest = transferRequest(
                senderAccountId,
                receiverAccountId,
                amount
        );

        new CrudRequester(
                userSpec,
                Endpoint.TRANSFER,
                responseSpecification
        ).post(transferRequest);

        assertAccountBalance(softy, userSpec, senderAccountId, SMALL_SENDER_BALANCE);
        assertAccountBalance(softy, userSpec, receiverAccountId, EMPTY_ACCOUNT_BALANCE);
    }

    public static Stream<Arguments> incorrectRandomTransferAmountData() {
        return Stream.of(
                Arguments.of(
                        RandomModelGenerator.generateNegativeTransferAmount(),
                        ResponseSpecs.transferAmountLessThanMin()
                ),
                Arguments.of(
                        RandomModelGenerator.generateTransferAmountMoreThanMax(),
                        ResponseSpecs.transferAmountMoreThanMax()
                )
        );
    }

    @MethodSource("incorrectRandomTransferAmountData")
    @ParameterizedTest
    public void userCannotTransferWithRandomIncorrectAmount(float amount,
                                                            ResponseSpecification responseSpecification) {
        RequestSpecification userSpec = createUserSpecForTest();

        int senderAccountId = AccountSteps.createAccount(userSpec);
        int receiverAccountId = AccountSteps.createAccount(userSpec);

        AccountSteps.prepareAccountWithSmallBalance(userSpec, senderAccountId);

        TransferRequest transferRequest = transferRequest(
                senderAccountId,
                receiverAccountId,
                amount
        );

        new CrudRequester(
                userSpec,
                Endpoint.TRANSFER,
                responseSpecification
        ).post(transferRequest);

        assertAccountBalance(softy, userSpec, senderAccountId, SMALL_SENDER_BALANCE);
        assertAccountBalance(softy, userSpec, receiverAccountId, EMPTY_ACCOUNT_BALANCE);
    }

    @Test
    public void userCannotTransferWhenBalanceIsNotEnough() {
        RequestSpecification userSpec = createUserSpecForTest();

        int senderAccountId = AccountSteps.createAccount(userSpec);
        int receiverAccountId = AccountSteps.createAccount(userSpec);

        AccountSteps.prepareAccountWithSmallBalance(userSpec, senderAccountId);

        float amount = RandomModelGenerator.generateTransferAmountMoreThanBalance(
                SMALL_SENDER_BALANCE
        );

        TransferRequest transferRequest = transferRequest(
                senderAccountId,
                receiverAccountId,
                amount
        );

        new CrudRequester(
                userSpec,
                Endpoint.TRANSFER,
                ResponseSpecs.invalidTransfer()
        ).post(transferRequest);

        assertAccountBalance(softy, userSpec, senderAccountId, SMALL_SENDER_BALANCE);
        assertAccountBalance(softy, userSpec, receiverAccountId, EMPTY_ACCOUNT_BALANCE);
    }

    @Test
    public void userCannotTransferToNonExistingAccount() {
        RequestSpecification userSpec = createUserSpecForTest();

        int senderAccountId = AccountSteps.createAccount(userSpec);
        AccountSteps.prepareAccountForTransfer(userSpec, senderAccountId);

        int nonExistingAccountId =
                RandomModelGenerator.generateNonExistingAccountIdBasedOn(senderAccountId);

        float amount = RandomModelGenerator.generateValidTransferAmount();

        TransferRequest transferRequest = transferRequest(
                senderAccountId,
                nonExistingAccountId,
                amount
        );

        new CrudRequester(
                userSpec,
                Endpoint.TRANSFER,
                ResponseSpecs.invalidTransfer()
        ).post(transferRequest);

        assertAccountBalance(softy, userSpec, senderAccountId, PREPARED_SENDER_BALANCE);
    }

    @Test
    public void userCannotTransferFromNonExistingAccount() {
        RequestSpecification userSpec = createUserSpecForTest();

        int receiverAccountId = AccountSteps.createAccount(userSpec);

        int nonExistingAccountId =
                RandomModelGenerator.generateNonExistingAccountIdBasedOn(receiverAccountId);

        float amount = RandomModelGenerator.generateValidTransferAmount();

        TransferRequest transferRequest = transferRequest(
                nonExistingAccountId,
                receiverAccountId,
                amount
        );

        new CrudRequester(
                userSpec,
                Endpoint.TRANSFER,
                ResponseSpecs.unauthorizedAccessToAccount()
        ).post(transferRequest);

        assertAccountBalance(softy, userSpec, receiverAccountId, EMPTY_ACCOUNT_BALANCE);
    }

    @Test
    public void userCannotTransferFromAnotherUserAccount() {
        RequestSpecification senderUserSpec = createUserSpecForTest();
        int senderAccountId = AccountSteps.createAccount(senderUserSpec);
        AccountSteps.prepareAccountForTransfer(senderUserSpec, senderAccountId);

        RequestSpecification receiverUserSpec = createUserSpecForTest();
        int receiverAccountId = AccountSteps.createAccount(receiverUserSpec);

        float amount = RandomModelGenerator.generateValidTransferAmount();

        TransferRequest transferRequest = transferRequest(
                senderAccountId,
                receiverAccountId,
                amount
        );

        new CrudRequester(
                receiverUserSpec,
                Endpoint.TRANSFER,
                ResponseSpecs.unauthorizedAccessToAccount()
        ).post(transferRequest);

        assertAccountBalance(softy, senderUserSpec, senderAccountId, PREPARED_SENDER_BALANCE);
        assertAccountBalance(softy, receiverUserSpec, receiverAccountId, EMPTY_ACCOUNT_BALANCE);
    }

    @Test
    public void userCannotTransferWithoutAuthorization() {
        RequestSpecification senderUserSpec = createUserSpecForTest();
        int senderAccountId = AccountSteps.createAccount(senderUserSpec);
        AccountSteps.prepareAccountForTransfer(senderUserSpec, senderAccountId);

        RequestSpecification receiverUserSpec = createUserSpecForTest();
        int receiverAccountId = AccountSteps.createAccount(receiverUserSpec);

        float amount = RandomModelGenerator.generateValidTransferAmount();

        TransferRequest transferRequest = transferRequest(
                senderAccountId,
                receiverAccountId,
                amount
        );

        new CrudRequester(
                RequestSpecs.unauthSpec(),
                Endpoint.TRANSFER,
                ResponseSpecs.unauthorized()
        ).post(transferRequest);

        assertAccountBalance(softy, senderUserSpec, senderAccountId, PREPARED_SENDER_BALANCE);
        assertAccountBalance(softy, receiverUserSpec, receiverAccountId, EMPTY_ACCOUNT_BALANCE);
    }

    @Test
    public void userCannotTransferWithWrongAuthorizationToken() {
        RequestSpecification senderUserSpec = createUserSpecForTest();
        int senderAccountId = AccountSteps.createAccount(senderUserSpec);
        AccountSteps.prepareAccountForTransfer(senderUserSpec, senderAccountId);

        RequestSpecification receiverUserSpec = createUserSpecForTest();
        int receiverAccountId = AccountSteps.createAccount(receiverUserSpec);

        float amount = RandomModelGenerator.generateValidTransferAmount();

        TransferRequest transferRequest = transferRequest(
                senderAccountId,
                receiverAccountId,
                amount
        );

        new CrudRequester(
                RequestSpecs.brokenAuthSpec(),
                Endpoint.TRANSFER,
                ResponseSpecs.unauthorized()
        ).post(transferRequest);

        assertAccountBalance(softy, senderUserSpec, senderAccountId, PREPARED_SENDER_BALANCE);
        assertAccountBalance(softy, receiverUserSpec, receiverAccountId, EMPTY_ACCOUNT_BALANCE);
    }
}