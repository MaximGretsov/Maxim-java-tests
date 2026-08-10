package iteration2.ui;

import api.models.*;
import api.requests.steps.UserSteps;
import com.codeborne.selenide.*;
import api.generators.RandomModelGenerator;
import common.annotations.UserSession;
import org.junit.jupiter.api.Test;
import ui.pages.BankAlert;
import ui.pages.UserDashboard;
import common.storage.SessionStorage;

import static api.assertions.AccountAssertions.assertAccountAfterSuccessfulDeposit;
import static api.assertions.AccountAssertions.assertAccountIsEmpty;
import static api.factories.DepositRequestFactory.depositRequestWithAmount;

public class DepositTests extends BaseUITest{
    @Test
    @UserSession
    public void userCanDepositMoneyWithCorrectDataTest() {
        UserSteps userSteps = SessionStorage.getSteps();

        int accountId = userSteps.createAccount();

        String accountNumber = userSteps
                .getAccountById(accountId)
                .getAccountNumber();

        assertAccountIsEmpty(softy, userSteps, accountId);

        float depositAmount = RandomModelGenerator.generateValidDepositAmount();

        new UserDashboard()
                .open()
                .openDepositPage()
                .selectAccount(accountId)
                .enterAmount(depositAmount)
                .submitDeposit()
                .checkAlertMessageAndAccept(BankAlert.GOOD_DEPOSIT.format(depositAmount, accountNumber));

        DepositRequest expectedDeposit = depositRequestWithAmount(accountId, depositAmount);

        assertAccountAfterSuccessfulDeposit(softy, userSteps, expectedDeposit);
    }

    @Test
    @UserSession
    public void userCannotDepositMoneyWithIncorrectDataTest() {
        UserSteps userSteps = SessionStorage.getSteps();

        int accountId = userSteps.createAccount();

        assertAccountIsEmpty(softy, userSteps, accountId);

        float incorrectDepositAmount = RandomModelGenerator.generateNegativeDepositAmount();

        new UserDashboard()
                .open()
                .openDepositPage()
                .selectAccount(accountId)
                .enterAmount(incorrectDepositAmount)
                .submitDeposit()
                .checkAlertMessageAndAccept(BankAlert.BAD_DEPOSIT.getMessage());

        assertAccountIsEmpty(softy, userSteps, accountId);
    }
}
