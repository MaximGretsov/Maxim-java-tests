package iteration1.ui;

import com.codeborne.selenide.Condition;
import api.models.CreateUserRequest;
import iteration2.ui.BaseUITest;
import org.junit.jupiter.api.Test;
import api.requests.steps.AdminSteps;
import ui.pages.AdminPanel;
import ui.pages.LoginPage;
import ui.pages.UserDashboard;

public class LoginUserTest extends BaseUITest {
    @Test
    public void adminCanLoginTestWithCorrectData(){
        CreateUserRequest admin = CreateUserRequest.getAdmin();

        new LoginPage().open().login(admin.getUsername(), admin.getPassword())
                .getPage(AdminPanel.class).getAdminPanelText().shouldBe(Condition.visible);
    }

    @Test
    public void userCanLoginWithCorrectDataTest(){
        // create user
        CreateUserRequest user = AdminSteps.createUser();

        new LoginPage()
                .open()
                .login(user.getUsername(), user.getPassword())
                .getPage(UserDashboard.class)
                .shouldHaveDefaultWelcomeText();
    }
}
