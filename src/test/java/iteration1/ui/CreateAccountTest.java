package iteration1.ui;

import api.requests.steps.UserSteps;
import api.models.CreateAccountResponse;
import api.models.CreateUserRequest;
import iteration2.ui.BaseUITest;
import org.junit.jupiter.api.Test;
import api.requests.steps.AdminSteps;
import api.specs.RequestSpecs;
import ui.pages.BankAlert;
import ui.pages.UserDashboard;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class CreateAccountTest extends BaseUITest {
    @Test
    public void userCanCreateAccountTest(){
        CreateUserRequest user = AdminSteps.createUser();

        authAsUser(user);

        new UserDashboard().open().createNewAccount();

        List<CreateAccountResponse> createdAccounts = new UserSteps(user.getUsername(),user.getPassword())
                .getAllAccounts();

        assertThat(createdAccounts).hasSize(1);

        new UserDashboard().checkAlertMessageAndAccept(BankAlert.NEW_ACCOUNT_CREATED
                + createdAccounts.getFirst().getAccountNumber());

        assertThat(createdAccounts.getFirst().getBalance()).isZero();
    }
}
