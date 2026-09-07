package iteration1.ui;

import api.models.CreateAccountResponse;
import common.annotations.UserSession;
import common.storage.SessionStorage;
import iteration2.ui.BaseUITest;
import org.junit.jupiter.api.Test;
import ui.pages.UserDashboard;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateAccountTest extends BaseUITest {
    @Test
    @UserSession
    public void userCanCreateAccountTest() {
        String accountNumber = new UserDashboard()
                .open()
                .createNewAccount()
                .checkNewAccountCreatedAlertAndAccept();

        List<CreateAccountResponse> createdAccounts = SessionStorage.getSteps().getAllAccounts();

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
