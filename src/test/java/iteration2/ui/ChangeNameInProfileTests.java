package iteration2.ui;

import api.generators.RandomModelGenerator;
import api.requests.steps.UserSteps;
import common.annotations.UserSession;
import common.storage.SessionStorage;
import org.junit.jupiter.api.Test;
import ui.pages.BankAlert;
import ui.pages.UserDashboard;

import static api.assertions.ProfileAssertions.assertProfileName;
import static api.testdata.ProfileTestData.*;

public class ChangeNameInProfileTests extends BaseUITest{
    @Test
    @UserSession
    public void userCanUpdateProfileNameWithCorrectDataTest() {
        UserSteps userSteps = SessionStorage.getSteps();

        String correctNewName =
                RandomModelGenerator.generateValidProfileName();

        new UserDashboard()
                .open()
                .openEditProfile()
                .changeName(correctNewName)
                .checkAlertMessageAndAccept(
                        BankAlert.PROFILE_UPDATED_SUCCESSFULLY.getMessage()
                )
                .openUserDashboard()
                .checkDisplayedName(correctNewName);

        assertProfileName(
                softy,
                userSteps,
                correctNewName
        );
    }

    @Test
    @UserSession
    public void userCannotUpdateProfileNameWithIncorrectDataTest() {
        UserSteps userSteps = SessionStorage.getSteps();

        String incorrectNewName =
                RandomModelGenerator.generateStringValue();

        new UserDashboard()
                .open()
                .openEditProfile()
                .changeName(incorrectNewName)
                .checkAlertMessageAndAccept(
                        BankAlert.ENTER_VALID_NAME.getMessage()
                )
                .openUserDashboard()
                .checkDisplayedName(DEFAULT_PROFILE_UI_NAME);

        assertProfileName(
                softy,
                userSteps,
                DEFAULT_PROFILE_API_NAME
        );
    }
}
