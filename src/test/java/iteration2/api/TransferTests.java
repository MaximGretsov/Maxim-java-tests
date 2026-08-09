package iteration2.api;

import api.dao.AccountDao;
import api.dao.comparison.DaoAndModelAssertions;
import api.generators.RandomModelGenerator;
import api.models.AccountResponse;
import api.requests.steps.DataBaseSteps;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
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

        AccountSteps.prepareAccountForTransfer(senderUserSpec, senderAccountId);

        RequestSpecification receiverUserSpec = createUserSpecForTest();

        int receiverAccountId = AccountSteps.createAccount(receiverUserSpec);

        float amount = RandomModelGenerator.generateValidTransferAmount();

        float senderExpectedBalance = PREPARED_SENDER_BALANCE - amount;

        float receiverExpectedBalance = EMPTY_ACCOUNT_BALANCE + amount;

        TransferRequest transferRequest = transferRequest(senderAccountId, receiverAccountId, amount);

        TransferResponse transferResponse =
                new ValidatedCrudRequester<TransferResponse>(
                        senderUserSpec,
                        Endpoint.TRANSFER,
                        ResponseSpecs.requestReturnsOk()
                ).post(transferRequest);

        assertSuccessfulTransferResponse(softy, transferRequest, transferResponse);

        AccountResponse senderAccount = AccountSteps.getAccountById(senderUserSpec, senderAccountId);

        assertAccountBalance(softy, senderAccount, senderAccountId, senderExpectedBalance);

        AccountDao senderAccountDao = DataBaseSteps.getAccountById((long) senderAccountId);

        DaoAndModelAssertions
                .assertThat(senderAccount, senderAccountDao)
                .match();

        AccountResponse receiverAccount = AccountSteps.getAccountById(receiverUserSpec, receiverAccountId);

        assertAccountBalance(softy, receiverAccount, receiverAccountId, receiverExpectedBalance);

        AccountDao receiverAccountDao = DataBaseSteps.getAccountById((long) receiverAccountId);

        DaoAndModelAssertions
                .assertThat(receiverAccount, receiverAccountDao)
                .match();
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

        AccountSteps.prepareAccountForTransfer(senderUserSpec, senderAccountId);

        RequestSpecification receiverUserSpec = createUserSpecForTest();

        int receiverAccountId = AccountSteps.createAccount(receiverUserSpec);

        float senderExpectedBalance = PREPARED_SENDER_BALANCE - amount;

        float receiverExpectedBalance = EMPTY_ACCOUNT_BALANCE + amount;

        TransferRequest transferRequest = transferRequest(senderAccountId, receiverAccountId, amount);

        TransferResponse transferResponse =
                new ValidatedCrudRequester<TransferResponse>(
                        senderUserSpec,
                        Endpoint.TRANSFER,
                        ResponseSpecs.requestReturnsOk()
                ).post(transferRequest);

        assertSuccessfulTransferResponse(softy, transferRequest, transferResponse);

        AccountResponse senderAccount = AccountSteps.getAccountById(senderUserSpec, senderAccountId);

        assertAccountBalance(softy, senderAccount, senderAccountId, senderExpectedBalance);

        AccountDao senderAccountDao = DataBaseSteps.getAccountById((long) senderAccountId);

        DaoAndModelAssertions
                .assertThat(senderAccount, senderAccountDao)
                .match();

        AccountResponse receiverAccount =
                AccountSteps.getAccountById(
                        receiverUserSpec,
                        receiverAccountId
                );

        assertAccountBalance(softy, receiverAccount, receiverAccountId, receiverExpectedBalance);

        AccountDao receiverAccountDao = DataBaseSteps.getAccountById((long) receiverAccountId);

        DaoAndModelAssertions
                .assertThat(receiverAccount, receiverAccountDao)
                .match();
    }

    @Test
    public void userCanTransferBetweenOwnAccountsWithRandomCorrectAmount() {
        RequestSpecification userSpec = createUserSpecForTest();

        int senderAccountId = AccountSteps.createAccount(userSpec);

        int receiverAccountId = AccountSteps.createAccount(userSpec);

        AccountSteps.prepareAccountForTransfer(userSpec, senderAccountId);

        float amount = RandomModelGenerator.generateValidTransferAmount();

        float senderExpectedBalance = PREPARED_SENDER_BALANCE - amount;

        float receiverExpectedBalance = EMPTY_ACCOUNT_BALANCE + amount;

        TransferRequest transferRequest = transferRequest(senderAccountId, receiverAccountId, amount);

        TransferResponse transferResponse =
                new ValidatedCrudRequester<TransferResponse>(
                        userSpec,
                        Endpoint.TRANSFER,
                        ResponseSpecs.requestReturnsOk()
                ).post(transferRequest);

        assertSuccessfulTransferResponse(softy, transferRequest, transferResponse);

        AccountResponse senderAccount = AccountSteps.getAccountById(userSpec, senderAccountId);

        assertAccountBalance(softy, senderAccount, senderAccountId, senderExpectedBalance);

        AccountDao senderAccountDao = DataBaseSteps.getAccountById((long) senderAccountId);

        DaoAndModelAssertions
                .assertThat(senderAccount, senderAccountDao)
                .match();

        AccountResponse receiverAccount = AccountSteps.getAccountById(userSpec, receiverAccountId);

        assertAccountBalance(softy, receiverAccount, receiverAccountId, receiverExpectedBalance);

        AccountDao receiverAccountDao = DataBaseSteps.getAccountById((long) receiverAccountId);

        DaoAndModelAssertions
                .assertThat(receiverAccount, receiverAccountDao)
                .match();
    }

    public static Stream<Arguments> incorrectBoundaryTransferAmountData() {
        return Stream.of(
                Arguments.of(
                        0f,
                        ResponseSpecs.transferAmountLessThanMin()
                ),
                Arguments.of(
                        10000.01f,
                        ResponseSpecs.transferAmountMoreThanMax()
                )
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

        TransferRequest transferRequest = transferRequest(senderAccountId, receiverAccountId, amount);

        new CrudRequester(
                userSpec,
                Endpoint.TRANSFER,
                responseSpecification
        ).post(transferRequest);

        AccountResponse senderAccount = AccountSteps.getAccountById(userSpec, senderAccountId);

        assertAccountBalance(softy, senderAccount, senderAccountId, SMALL_SENDER_BALANCE);

        AccountDao senderAccountDao = DataBaseSteps.getAccountById((long) senderAccountId);

        DaoAndModelAssertions
                .assertThat(senderAccount, senderAccountDao)
                .match();

        AccountResponse receiverAccount = AccountSteps.getAccountById(userSpec, receiverAccountId);

        assertAccountBalance(softy, receiverAccount, receiverAccountId, EMPTY_ACCOUNT_BALANCE);

        AccountDao receiverAccountDao = DataBaseSteps.getAccountById((long) receiverAccountId);

        DaoAndModelAssertions
                .assertThat(receiverAccount, receiverAccountDao)
                .match();
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

        TransferRequest transferRequest = transferRequest(senderAccountId, receiverAccountId, amount);

        new CrudRequester(
                userSpec,
                Endpoint.TRANSFER,
                responseSpecification
        ).post(transferRequest);

        AccountResponse senderAccount = AccountSteps.getAccountById(userSpec, senderAccountId);

        assertAccountBalance(softy, senderAccount, senderAccountId, SMALL_SENDER_BALANCE);

        AccountDao senderAccountDao = DataBaseSteps.getAccountById((long) senderAccountId);

        DaoAndModelAssertions
                .assertThat(senderAccount, senderAccountDao)
                .match();

        AccountResponse receiverAccount = AccountSteps.getAccountById(userSpec, receiverAccountId);

        assertAccountBalance(softy, receiverAccount, receiverAccountId, EMPTY_ACCOUNT_BALANCE);

        AccountDao receiverAccountDao = DataBaseSteps.getAccountById((long) receiverAccountId);

        DaoAndModelAssertions
                .assertThat(receiverAccount, receiverAccountDao)
                .match();
    }

    @Test
    public void userCannotTransferWhenBalanceIsNotEnough() {
        RequestSpecification userSpec = createUserSpecForTest();

        int senderAccountId = AccountSteps.createAccount(userSpec);

        int receiverAccountId = AccountSteps.createAccount(userSpec);

        AccountSteps.prepareAccountWithSmallBalance(userSpec, senderAccountId);

        float amount = RandomModelGenerator.generateTransferAmountMoreThanBalance(SMALL_SENDER_BALANCE);

        TransferRequest transferRequest = transferRequest(senderAccountId, receiverAccountId, amount);

        new CrudRequester(
                userSpec,
                Endpoint.TRANSFER,
                ResponseSpecs.invalidTransfer()
        ).post(transferRequest);

        AccountResponse senderAccount = AccountSteps.getAccountById(userSpec, senderAccountId);

        assertAccountBalance(softy, senderAccount, senderAccountId, SMALL_SENDER_BALANCE);

        AccountDao senderAccountDao = DataBaseSteps.getAccountById((long) senderAccountId);

        DaoAndModelAssertions
                .assertThat(senderAccount, senderAccountDao)
                .match();

        AccountResponse receiverAccount = AccountSteps.getAccountById(userSpec, receiverAccountId);

        assertAccountBalance(softy, receiverAccount, receiverAccountId, EMPTY_ACCOUNT_BALANCE);

        AccountDao receiverAccountDao = DataBaseSteps.getAccountById((long) receiverAccountId);

        DaoAndModelAssertions
                .assertThat(receiverAccount, receiverAccountDao)
                .match();
    }

    @Test
    public void userCannotTransferToNonExistingAccount() {
        RequestSpecification userSpec = createUserSpecForTest();

        int senderAccountId = AccountSteps.createAccount(userSpec);

        AccountSteps.prepareAccountForTransfer(userSpec, senderAccountId);

        int nonExistingAccountId = RandomModelGenerator
                        .generateNonExistingAccountIdBasedOn(senderAccountId);

        float amount = RandomModelGenerator.generateValidTransferAmount();

        TransferRequest transferRequest = transferRequest(senderAccountId, nonExistingAccountId, amount);

        new CrudRequester(
                userSpec,
                Endpoint.TRANSFER,
                ResponseSpecs.invalidTransfer()
        ).post(transferRequest);

        AccountResponse senderAccount = AccountSteps.getAccountById(userSpec, senderAccountId);

        assertAccountBalance(softy, senderAccount, senderAccountId, PREPARED_SENDER_BALANCE);

        AccountDao senderAccountDao = DataBaseSteps.getAccountById((long) senderAccountId);

        DaoAndModelAssertions
                .assertThat(senderAccount, senderAccountDao)
                .match();
    }

    @Test
    public void userCannotTransferFromNonExistingAccount() {
        RequestSpecification userSpec = createUserSpecForTest();

        int receiverAccountId = AccountSteps.createAccount(userSpec);

        int nonExistingAccountId = RandomModelGenerator
                        .generateNonExistingAccountIdBasedOn(receiverAccountId);

        float amount = RandomModelGenerator.generateValidTransferAmount();

        TransferRequest transferRequest = transferRequest(nonExistingAccountId, receiverAccountId, amount);

        new CrudRequester(
                userSpec,
                Endpoint.TRANSFER,
                ResponseSpecs.unauthorizedAccessToAccount()
        ).post(transferRequest);

        AccountResponse receiverAccount = AccountSteps.getAccountById(userSpec, receiverAccountId);

        assertAccountBalance(softy, receiverAccount, receiverAccountId, EMPTY_ACCOUNT_BALANCE);

        AccountDao receiverAccountDao = DataBaseSteps.getAccountById((long) receiverAccountId);

        DaoAndModelAssertions
                .assertThat(receiverAccount, receiverAccountDao)
                .match();
    }

    @Test
    public void userCannotTransferFromAnotherUserAccount() {
        RequestSpecification senderUserSpec = createUserSpecForTest();

        int senderAccountId = AccountSteps.createAccount(senderUserSpec);

        AccountSteps.prepareAccountForTransfer(senderUserSpec, senderAccountId);

        RequestSpecification receiverUserSpec = createUserSpecForTest();

        int receiverAccountId = AccountSteps.createAccount(receiverUserSpec);

        float amount = RandomModelGenerator.generateValidTransferAmount();

        TransferRequest transferRequest = transferRequest(senderAccountId, receiverAccountId, amount);

        new CrudRequester(
                receiverUserSpec,
                Endpoint.TRANSFER,
                ResponseSpecs.unauthorizedAccessToAccount()
        ).post(transferRequest);

        AccountResponse senderAccount = AccountSteps.getAccountById(senderUserSpec, senderAccountId);

        assertAccountBalance(softy, senderAccount, senderAccountId, PREPARED_SENDER_BALANCE);

        AccountDao senderAccountDao = DataBaseSteps.getAccountById((long) senderAccountId);

        DaoAndModelAssertions
                .assertThat(senderAccount, senderAccountDao)
                .match();

        AccountResponse receiverAccount = AccountSteps.getAccountById(receiverUserSpec, receiverAccountId);

        assertAccountBalance(softy, receiverAccount, receiverAccountId, EMPTY_ACCOUNT_BALANCE);

        AccountDao receiverAccountDao = DataBaseSteps.getAccountById((long) receiverAccountId);

        DaoAndModelAssertions
                .assertThat(receiverAccount, receiverAccountDao)
                .match();
    }

    @Test
    public void userCannotTransferWithoutAuthorization() {
        RequestSpecification senderUserSpec = createUserSpecForTest();

        int senderAccountId = AccountSteps.createAccount(senderUserSpec);

        AccountSteps.prepareAccountForTransfer(senderUserSpec, senderAccountId);

        RequestSpecification receiverUserSpec = createUserSpecForTest();

        int receiverAccountId = AccountSteps.createAccount(receiverUserSpec);

        float amount = RandomModelGenerator.generateValidTransferAmount();

        TransferRequest transferRequest = transferRequest(senderAccountId, receiverAccountId, amount);

        new CrudRequester(
                RequestSpecs.unauthSpec(),
                Endpoint.TRANSFER,
                ResponseSpecs.unauthorized()
        ).post(transferRequest);

        AccountResponse senderAccount = AccountSteps.getAccountById(senderUserSpec, senderAccountId);

        assertAccountBalance(softy, senderAccount, senderAccountId, PREPARED_SENDER_BALANCE);

        AccountDao senderAccountDao = DataBaseSteps.getAccountById((long) senderAccountId);

        DaoAndModelAssertions
                .assertThat(senderAccount, senderAccountDao)
                .match();

        AccountResponse receiverAccount = AccountSteps.getAccountById(receiverUserSpec, receiverAccountId);

        assertAccountBalance(softy, receiverAccount, receiverAccountId, EMPTY_ACCOUNT_BALANCE);

        AccountDao receiverAccountDao = DataBaseSteps.getAccountById((long) receiverAccountId);

        DaoAndModelAssertions
                .assertThat(receiverAccount, receiverAccountDao)
                .match();
    }

    @Test
    public void userCannotTransferWithWrongAuthorizationToken() {
        RequestSpecification senderUserSpec = createUserSpecForTest();

        int senderAccountId = AccountSteps.createAccount(senderUserSpec);

        AccountSteps.prepareAccountForTransfer(senderUserSpec, senderAccountId);

        RequestSpecification receiverUserSpec = createUserSpecForTest();

        int receiverAccountId = AccountSteps.createAccount(receiverUserSpec);

        float amount = RandomModelGenerator.generateValidTransferAmount();

        TransferRequest transferRequest = transferRequest(senderAccountId, receiverAccountId, amount);

        new CrudRequester(
                RequestSpecs.brokenAuthSpec(),
                Endpoint.TRANSFER,
                ResponseSpecs.unauthorized()
        ).post(transferRequest);

        AccountResponse senderAccount = AccountSteps.getAccountById(senderUserSpec, senderAccountId);

        assertAccountBalance(softy, senderAccount, senderAccountId, PREPARED_SENDER_BALANCE);

        AccountDao senderAccountDao = DataBaseSteps.getAccountById((long) senderAccountId);

        DaoAndModelAssertions
                .assertThat(senderAccount, senderAccountDao)
                .match();

        AccountResponse receiverAccount =
                AccountSteps.getAccountById(
                        receiverUserSpec,
                        receiverAccountId
                );

        assertAccountBalance(softy, receiverAccount, receiverAccountId, EMPTY_ACCOUNT_BALANCE);

        AccountDao receiverAccountDao = DataBaseSteps.getAccountById((long) receiverAccountId);

        DaoAndModelAssertions
                .assertThat(receiverAccount, receiverAccountDao)
                .match();
    }
}