package iteration2.ui;

import api.generators.RandomModelGenerator;
import api.models.CreateUserRequest;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;
import api.specs.RequestSpecs;
import ui.pages.BankAlert;
import ui.pages.UserDashboard;

import static api.assertions.ProfileAssertions.assertProfileName;
import static api.testdata.ProfileTestData.*;

public class ChangeNameInProfileTests extends BaseUITest{
    @Test
    public void userCanUpdateProfileNameWithCorrectDataTest(){
        CreateUserRequest user = createUserForTest();

        RequestSpecification userSpec = RequestSpecs.authAsUserSpec(
                user.getUsername(),
                user.getPassword()
        );

        String correctNewName = RandomModelGenerator.generateValidProfileName();

        authAsUser(user);

        new UserDashboard()
                .open()
                .openEditProfile()
                .changeName(correctNewName)
                .checkAlertMessageAndAccept(BankAlert.PROFILE_UPDATED_SUCCESSFULLY.getMessage())
                .openUserDashboard()
                .checkDisplayedName(correctNewName);

        // Шаг 7: имя пользователя изменилось на API
        assertProfileName(softy, userSpec, correctNewName);
    }

    @Test
    public void userCannotUpdateProfileNameWithIncorrectDataTest(){
        // Шаги по настройке окружения
        CreateUserRequest user = createUserForTest();

        RequestSpecification userSpec = RequestSpecs.authAsUserSpec(
                user.getUsername(),
                user.getPassword()
        );

        String incorrectNewName = RandomModelGenerator.generateStringValue();

        authAsUser(user);

        new UserDashboard()
                .open()
                .openEditProfile()
                .changeName(incorrectNewName)
                .checkAlertMessageAndAccept(BankAlert.ENTER_VALID_NAME.getMessage())
                .openUserDashboard()
                .checkDisplayedName(DEFAULT_PROFILE_UI_NAME);

        assertProfileName(softy, userSpec, DEFAULT_PROFILE_API_NAME);
    }
}
