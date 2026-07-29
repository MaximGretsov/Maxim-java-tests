package iteration1.ui;

import api.requests.steps.UserSteps;
import api.models.CreateAccountResponse;
import api.models.CreateUserRequest;
import iteration2.ui.BaseUITest;
import org.junit.jupiter.api.Test;
import api.requests.steps.AdminSteps;
import ui.pages.UserDashboard;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateAccountTest extends BaseUITest {
    @Test
    public void userCanCreateAccountTest() {
        CreateUserRequest user = AdminSteps.createUser();

        authAsUser(user);

        String accountNumber = new UserDashboard()
                .open()
                .createNewAccount()
                .checkNewAccountCreatedAlertAndAccept();

        List<CreateAccountResponse> createdAccounts =
                new UserSteps(user.getUsername(), user.getPassword())
                        .getAllAccounts();

        assertThat(createdAccounts)
                .singleElement()
                .satisfies(account -> {
                    assertThat(account.getAccountNumber())
                            .isEqualTo(accountNumber);

                    assertThat(account.getBalance())
                            .isZero();
                });
    }
}
