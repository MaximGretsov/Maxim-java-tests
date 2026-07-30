package iteration2.ui;

import api.models.*;
import api.generators.RandomModelGenerator;
import api.requests.steps.UserSteps;
import common.annotations.UserSession;
import common.storage.SessionStorage;
import org.junit.jupiter.api.Test;
import ui.pages.BankAlert;
import ui.pages.UserDashboard;

import static api.assertions.AccountAssertions.assertAccountBalance;
import static api.assertions.TransferAssertions.assertSuccessfulTransferResponse;
import static api.factories.TransferRequestFactory.transferRequest;
import static api.testdata.AccountTestData.EMPTY_ACCOUNT_BALANCE;
import static api.testdata.AccountTestData.PREPARED_SENDER_BALANCE;

public class TransferTests extends BaseUITest {
    @Test
    @UserSession
    public void userCanTransferMoneyWithCorrectDataTest() {
        CreateUserRequest user = SessionStorage.getUser();
        UserSteps userSteps = SessionStorage.getSteps();

        int senderAccountId = userSteps.createAccount();
        int receiverAccountId = userSteps.createAccount();

        userSteps.prepareAccountForTransfer(senderAccountId);

        String receiverAccountNumber = userSteps
                .getAccountById(receiverAccountId)
                .getAccountNumber();

        float transferAmount =
                RandomModelGenerator.generateValidTransferAmount();

        new UserDashboard()
                .open()
                .openTransferPage()
                .selectSenderAccount(senderAccountId)
                .enterReceiverName(user.getUsername())
                .enterReceiverAccountNumber(receiverAccountNumber)
                .enterTransferAmount(transferAmount)
                .confirmTransferDetails()
                .submitTransfer()
                .checkAlertMessageAndAccept(
                        BankAlert.GOOD_TRANSFER.format(
                                transferAmount,
                                receiverAccountId
                        )
                );

        assertAccountBalance(softy, userSteps, senderAccountId,
                PREPARED_SENDER_BALANCE - transferAmount);

        assertAccountBalance(softy, userSteps, receiverAccountId,
                EMPTY_ACCOUNT_BALANCE + transferAmount);
    }

    @Test
    @UserSession(value = 2, auth = 1)
    public void userCanTransferMoneyToAnotherUserWithCorrectDataTest() {
        CreateUserRequest senderUser = SessionStorage.getUser(1);

        CreateUserRequest receiverUser = SessionStorage.getUser(2);

        UserSteps senderSteps = SessionStorage.getSteps(1);

        UserSteps receiverSteps = SessionStorage.getSteps(2);

        int senderAccountId = senderSteps.createAccount();

        senderSteps.prepareAccountForTransfer(senderAccountId);

        int receiverAccountId = receiverSteps.createAccount();

        String receiverAccountNumber = receiverSteps
                .getAccountById(receiverAccountId)
                .getAccountNumber();

        float transferAmount =
                RandomModelGenerator.generateValidTransferAmount();

        new UserDashboard()
                .open()
                .openTransferPage()
                .selectSenderAccount(senderAccountId)
                .enterReceiverName(receiverUser.getUsername())
                .enterReceiverAccountNumber(receiverAccountNumber)
                .enterTransferAmount(transferAmount)
                .confirmTransferDetails()
                .submitTransfer()
                .checkAlertMessageAndAccept(
                        BankAlert.GOOD_TRANSFER.format(
                                transferAmount,
                                receiverAccountId
                        )
                );

        assertAccountBalance(softy, senderSteps, senderAccountId,
                PREPARED_SENDER_BALANCE - transferAmount);

        assertAccountBalance(softy, receiverSteps, receiverAccountId,
                EMPTY_ACCOUNT_BALANCE + transferAmount);
    }

    @Test
    @UserSession
    public void userCannotTransferMoneyWithNegativeAmountTest() {
        CreateUserRequest user = SessionStorage.getUser();

        UserSteps userSteps = SessionStorage.getSteps();

        int senderAccountId = userSteps.createAccount();

        int receiverAccountId = userSteps.createAccount();

        userSteps.prepareAccountForTransfer(senderAccountId);

        String receiverAccountNumber = userSteps
                .getAccountById(receiverAccountId)
                .getAccountNumber();

        float negativeTransferAmount =
                RandomModelGenerator.generateNegativeTransferAmount();

        new UserDashboard()
                .open()
                .openTransferPage()
                .selectSenderAccount(senderAccountId)
                .enterReceiverName(user.getUsername())
                .enterReceiverAccountNumber(receiverAccountNumber)
                .enterTransferAmount(negativeTransferAmount)
                .confirmTransferDetails()
                .submitTransfer()
                .checkAlertMessageAndAccept(
                        BankAlert.AMOUNT_MUST_BE_MORE_THAN_MINIMUM
                                .getMessage()
                );

        assertAccountBalance(softy, userSteps, senderAccountId,
                PREPARED_SENDER_BALANCE);

        assertAccountBalance(softy, userSteps, receiverAccountId,
                EMPTY_ACCOUNT_BALANCE);
    }

    @Test
    @UserSession
    public void userCanRepeatTransferWithCorrectDataTest() {
        CreateUserRequest user = SessionStorage.getUser();

        UserSteps userSteps = SessionStorage.getSteps();

        int senderAccountId = userSteps.createAccount();

        int receiverAccountId = userSteps.createAccount();

        userSteps.prepareAccountForTransfer(senderAccountId);

        float initialTransferAmount = RandomModelGenerator.generateValidDepositAmount();

        TransferRequest initialTransferRequest =
                transferRequest(
                        senderAccountId,
                        receiverAccountId,
                        initialTransferAmount
                );

        TransferResponse initialTransferResponse = userSteps.transfer(initialTransferRequest);

        assertSuccessfulTransferResponse(softy, initialTransferRequest, initialTransferResponse);

        new UserDashboard()
                .open()
                .openTransferPage()
                .openTransferAgain()
                .searchTransactions(user.getUsername())
                .openIncomingTransferForRepeat()
                .selectRepeatSenderAccount(senderAccountId)
                .enterRepeatTransferAmount(initialTransferAmount)
                .confirmRepeatTransferDetails()
                .submitRepeatTransfer()
                .checkAlertMessageAndAccept(
                        BankAlert.REPEAT_TRANSFER_SUCCESS.format(
                                initialTransferAmount,
                                senderAccountId,
                                receiverAccountId
                        )
                );

        assertAccountBalance(softy, userSteps, senderAccountId,
                PREPARED_SENDER_BALANCE - initialTransferAmount * 2);

        assertAccountBalance(softy, userSteps, receiverAccountId,
                EMPTY_ACCOUNT_BALANCE + initialTransferAmount * 2);
    }

    @Test
    @UserSession
    public void userCannotFindTransactionsByNonExistingUserTest() {
        String nonExistingUsername = RandomModelGenerator.generateStringValue();

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
    @UserSession
    public void userCannotRepeatTransferWithIncorrectAmountTest() {
        CreateUserRequest user = SessionStorage.getUser();

        UserSteps userSteps = SessionStorage.getSteps();

        int senderAccountId = userSteps.createAccount();

        int receiverAccountId = userSteps.createAccount();

        userSteps.prepareAccountForTransfer(senderAccountId);

        float initialTransferAmount = RandomModelGenerator.generateValidDepositAmount();

        TransferRequest initialTransferRequest =
                transferRequest(
                        senderAccountId,
                        receiverAccountId,
                        initialTransferAmount
                );

        TransferResponse initialTransferResponse = userSteps.transfer(initialTransferRequest);

        assertSuccessfulTransferResponse(softy, initialTransferRequest, initialTransferResponse);

        float invalidRepeatTransferAmount =
                RandomModelGenerator.generateNegativeTransferAmount();

        new UserDashboard()
                .open()
                .openTransferPage()
                .openTransferAgain()
                .searchTransactions(user.getUsername())
                .openIncomingTransferForRepeat()
                .selectRepeatSenderAccount(senderAccountId)
                .enterRepeatTransferAmount(
                        invalidRepeatTransferAmount
                )
                .confirmRepeatTransferDetails()
                .submitRepeatTransfer()
                .checkAlertMessageAndAccept(
                        BankAlert.REPEAT_TRANSFER_FAILED.getMessage()
                );

        assertAccountBalance(softy, userSteps, senderAccountId,
                PREPARED_SENDER_BALANCE - initialTransferAmount);

        assertAccountBalance(softy, userSteps, receiverAccountId,
                EMPTY_ACCOUNT_BALANCE + initialTransferAmount);
    }
}
