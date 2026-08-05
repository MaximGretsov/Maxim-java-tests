package iteration2.ui;

import api.models.*;
import api.requests.steps.AccountSteps;
import com.codeborne.selenide.*;
import api.generators.RandomModelGenerator;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;
import api.specs.RequestSpecs;
import ui.pages.BankAlert;
import ui.pages.UserDashboard;

import static api.assertions.AccountAssertions.assertAccountAfterSuccessfulDeposit;
import static api.assertions.AccountAssertions.assertAccountIsEmpty;
import static api.factories.DepositRequestFactory.depositRequestWithAmount;

public class DepositTests extends BaseUITest{
    @Test
    public void userCanDepositMoneyWithCorrectDataTest(){
        CreateUserRequest user = createUserForTest();

        RequestSpecification userSpec = RequestSpecs.authAsUserSpec(
                user.getUsername(),
                user.getPassword()
        );

        int accountId = AccountSteps.createAccount(userSpec);

        assertAccountIsEmpty(softy, userSpec, accountId);

        float depositAmount = RandomModelGenerator.generateValidDepositAmount();

        authAsUser(user);

        new UserDashboard()
                .open()
                .openDepositPage()
                .selectAccount(accountId)
                .enterAmount(depositAmount)
                .submitDeposit()
                .checkAlertMessageAndAccept(BankAlert.GOOD_DEPOSIT.format(depositAmount,accountId));

        DepositRequest expectedDeposit = depositRequestWithAmount(accountId, depositAmount);

        assertAccountAfterSuccessfulDeposit(softy, userSpec, expectedDeposit);
    }

    @Test
    public void userCannotDepositMoneyWithIncorrectDataTest(){
        CreateUserRequest user = createUserForTest();

        RequestSpecification userSpec = RequestSpecs.authAsUserSpec(
                user.getUsername(),
                user.getPassword()
        );

        int accountId = AccountSteps.createAccount(userSpec);

        assertAccountIsEmpty(
                softy,
                userSpec,
                accountId
        );
        float incorrectDepositAmount = RandomModelGenerator.generateNegativeDepositAmount();

        authAsUser(user);

        new UserDashboard()
                .open()
                .openDepositPage()
                .selectAccount(accountId)
                .enterAmount(incorrectDepositAmount)
                .submitDeposit()
                .checkAlertMessageAndAccept(BankAlert.BAD_DEPOSIT.getMessage());

        assertAccountIsEmpty(softy, userSpec, accountId);
    }
}
