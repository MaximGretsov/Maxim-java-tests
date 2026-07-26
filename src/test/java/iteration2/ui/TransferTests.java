package iteration2.ui;

import api.models.*;
import api.requests.steps.AccountSteps;
import api.generators.RandomModelGenerator;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;
import api.requests.skeleton.Endpoint;
import api.requests.skeleton.requesters.ValidatedCrudRequester;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
import ui.pages.BankAlert;
import ui.pages.UserDashboard;

import static api.assertions.AccountAssertions.assertAccountBalance;
import static api.assertions.TransferAssertions.assertSuccessfulTransferResponse;
import static api.factories.TransferRequestFactory.transferRequest;
import static api.testdata.AccountTestData.EMPTY_ACCOUNT_BALANCE;
import static api.testdata.AccountTestData.PREPARED_SENDER_BALANCE;

public class TransferTests extends BaseUITest {
    @Test
    public void userCanTransferMoneyWithCorrectDataTest(){
        CreateUserRequest user = createUserForTest();

        RequestSpecification userSpec = RequestSpecs.authAsUserSpec(
                user.getUsername(),
                user.getPassword()
        );

        int senderAccountId = AccountSteps.createAccount(userSpec);

        int receiverAccountId = AccountSteps.createAccount(userSpec);

        AccountSteps.prepareAccountForTransfer(
                userSpec,
                senderAccountId
        );

        String receiverAccountNumber = AccountSteps
                .getAccountById(userSpec, receiverAccountId)
                .getAccountNumber();

        float transferAmount = RandomModelGenerator.generateValidTransferAmount();

        authAsUser(user);

        // UI часть
        new UserDashboard()
                .open()
                .openTransferPage()
                .selectSenderAccount(senderAccountId)
                .enterReceiverName(user.getUsername())
                .enterReceiverAccountNumber(receiverAccountNumber)
                .enterTransferAmount(transferAmount)
                .confirmTransferDetails()
                .submitTransfer()
                .checkAlertMessageAndAccept(BankAlert.GOOD_TRANSFER.format(transferAmount,receiverAccountId));

        assertAccountBalance(softy, userSpec,
                senderAccountId, PREPARED_SENDER_BALANCE - transferAmount);

        assertAccountBalance(softy, userSpec, receiverAccountId,
                EMPTY_ACCOUNT_BALANCE + transferAmount);
    }

    @Test
    public void userCanTransferMoneyToAnotherUserWithCorrectDataTest() {
        CreateUserRequest senderUser = createUserForTest();

        RequestSpecification senderSpec = RequestSpecs.authAsUserSpec(
                senderUser.getUsername(),
                senderUser.getPassword()
        );

        int senderAccountId = AccountSteps.createAccount(senderSpec);

        AccountSteps.prepareAccountForTransfer(
                senderSpec,
                senderAccountId
        );

        CreateUserRequest receiverUser = createUserForTest();

        RequestSpecification receiverSpec = RequestSpecs.authAsUserSpec(
                receiverUser.getUsername(),
                receiverUser.getPassword()
        );

        int receiverAccountId = AccountSteps.createAccount(receiverSpec);

        String receiverAccountNumber = AccountSteps
                .getAccountById(receiverSpec, receiverAccountId)
                .getAccountNumber();

        // Генерируем и вводим корректную сумму перевода
        float transferAmount = RandomModelGenerator.generateValidTransferAmount();

        authAsUser(senderUser);

        // UI часть
        new UserDashboard()
                .open()
                .openTransferPage()
                .selectSenderAccount(senderAccountId)
                .enterReceiverName(receiverUser.getUsername())
                .enterReceiverAccountNumber(receiverAccountNumber)
                .enterTransferAmount(transferAmount)
                .confirmTransferDetails()
                .submitTransfer()
                .checkAlertMessageAndAccept(BankAlert.GOOD_TRANSFER.format(transferAmount,receiverAccountId));

        assertAccountBalance(softy, senderSpec,
                senderAccountId, PREPARED_SENDER_BALANCE - transferAmount);

        assertAccountBalance(softy, receiverSpec,
                receiverAccountId, EMPTY_ACCOUNT_BALANCE + transferAmount);
    }

    @Test
    public void userCannotTransferMoneyWithNegativeAmountTest(){
        CreateUserRequest user = createUserForTest();

        RequestSpecification userSpec = RequestSpecs.authAsUserSpec(
                user.getUsername(),
                user.getPassword()
        );

        int senderAccountId = AccountSteps.createAccount(userSpec);

        int receiverAccountId = AccountSteps.createAccount(userSpec);

        AccountSteps.prepareAccountForTransfer(
                userSpec,
                senderAccountId
        );

        String receiverAccountNumber = AccountSteps
                .getAccountById(userSpec, receiverAccountId)
                .getAccountNumber();

        float negativeTransferAmount = RandomModelGenerator.generateNegativeTransferAmount();

        authAsUser(user);

        // UI часть
        new UserDashboard()
                .open()
                .openTransferPage()
                .selectSenderAccount(senderAccountId)
                .enterReceiverName(user.getUsername())
                .enterReceiverAccountNumber(receiverAccountNumber)
                .enterTransferAmount(negativeTransferAmount)
                .confirmTransferDetails()
                .submitTransfer()
                .checkAlertMessageAndAccept(BankAlert.AMOUNT_MUST_BE_MORE_THAN_MINIMUM.getMessage());

        assertAccountBalance(softy, userSpec, senderAccountId, PREPARED_SENDER_BALANCE);

        assertAccountBalance(softy, userSpec, receiverAccountId, EMPTY_ACCOUNT_BALANCE);
    }


    @Test
    public void userCanRepeatTransferWithCorrectDataTest() {
        CreateUserRequest user = createUserForTest();

        RequestSpecification userSpec = RequestSpecs.authAsUserSpec(
                user.getUsername(),
                user.getPassword()
        );

        int senderAccountId = AccountSteps.createAccount(userSpec);

        int receiverAccountId = AccountSteps.createAccount(userSpec);

        AccountSteps.prepareAccountForTransfer(
                userSpec,
                senderAccountId
        );

        // Этот перевод потом найдём и повторим через UI
        // (делаем generateValidDepositAmount так как он до 5000 тысяч и мы при двух транзакциях не выйдем за 15000)
        float initialTransferAmount = RandomModelGenerator.generateValidDepositAmount();

        TransferRequest initialTransferRequest = transferRequest(
                senderAccountId,
                receiverAccountId,
                initialTransferAmount
        );

        TransferResponse initialTransferResponse =
                new ValidatedCrudRequester<TransferResponse>(
                        userSpec,
                        Endpoint.TRANSFER,
                        ResponseSpecs.requestReturnsOk()
                ).post(initialTransferRequest);

        assertSuccessfulTransferResponse(
                softy,
                initialTransferRequest,
                initialTransferResponse
        );

        float repeatTransferAmount = initialTransferAmount;

        authAsUser(user);

        new UserDashboard()
                .open()
                .openTransferPage()
                .openTransferAgain()
                .searchTransactions(user.getUsername())
                .openRepeatTransferFor("TRANSFER_IN")
                .selectRepeatSenderAccount(senderAccountId)
                .enterRepeatTransferAmount(repeatTransferAmount)
                .confirmRepeatTransferDetails()
                .submitRepeatTransfer()
                .checkAlertMessageAndAccept(
                        BankAlert.REPEAT_TRANSFER_SUCCESS.format(
                                repeatTransferAmount,
                                senderAccountId,
                                receiverAccountId
                        )
                );

        assertAccountBalance(softy, userSpec,
                senderAccountId, PREPARED_SENDER_BALANCE - initialTransferAmount * 2);

        assertAccountBalance(softy, userSpec,
                receiverAccountId, EMPTY_ACCOUNT_BALANCE + initialTransferAmount * 2);
    }

    @Test
    public void userCannotFindTransactionsByNonExistingUserTest() {
        CreateUserRequest user = createUserForTest();

        String nonExistingUsername =
                RandomModelGenerator.generateStringValue();

        authAsUser(user);

        new UserDashboard()
                .open()
                .openTransferPage()
                .openTransferAgain()
                .searchTransactions(nonExistingUsername)
                .checkAlertMessageAndAccept(
                        BankAlert.NO_MATCHING_USERS_FOUND.getMessage()
                )
                .checkSearchResultsAreEmpty();
    }

    @Test
    public void userCannotRepeatTransferWithIncorrectAmountTest() {
        CreateUserRequest user = createUserForTest();

        RequestSpecification userSpec = RequestSpecs.authAsUserSpec(
                user.getUsername(),
                user.getPassword()
        );

        int senderAccountId = AccountSteps.createAccount(userSpec);

        int receiverAccountId = AccountSteps.createAccount(userSpec);

        AccountSteps.prepareAccountForTransfer(
                userSpec,
                senderAccountId
        );

        // Этот перевод потом найдём и повторим через UI
        // (делаем generateValidDepositAmount так как он до 5000 тысяч и мы при двух транзакциях не выйдем за 15000)
        float initialTransferAmount = RandomModelGenerator.generateValidDepositAmount();

        TransferRequest initialTransferRequest = transferRequest(
                senderAccountId,
                receiverAccountId,
                initialTransferAmount
        );

        TransferResponse initialTransferResponse =
                new ValidatedCrudRequester<TransferResponse>(
                        userSpec,
                        Endpoint.TRANSFER,
                        ResponseSpecs.requestReturnsOk()
                ).post(initialTransferRequest);

        assertSuccessfulTransferResponse(
                softy,
                initialTransferRequest,
                initialTransferResponse
        );

        float invalidRepeatTransferAmount =
                RandomModelGenerator.generateNegativeTransferAmount();

        authAsUser(user);

        new UserDashboard()
                .open()
                .openTransferPage()
                .openTransferAgain()
                .searchTransactions(user.getUsername())
                .openRepeatTransferFor("TRANSFER_IN")
                .selectRepeatSenderAccount(senderAccountId)
                .enterRepeatTransferAmount(
                        invalidRepeatTransferAmount
                )
                .confirmRepeatTransferDetails()
                .submitRepeatTransfer()
                .checkAlertMessageAndAccept(
                        BankAlert.REPEAT_TRANSFER_FAILED.getMessage()
                );

        assertAccountBalance(softy, userSpec,
                senderAccountId, PREPARED_SENDER_BALANCE - initialTransferAmount);

        assertAccountBalance(softy, userSpec,
                receiverAccountId, EMPTY_ACCOUNT_BALANCE + initialTransferAmount);
    }
}
