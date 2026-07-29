package iteration1.ui;

import api.requests.steps.AdminSteps;
import com.codeborne.selenide.*;
import api.generators.RandomModelGenerator;
import api.models.CreateUserRequest;
import api.models.CreateUserResponse;
import api.models.comparison.ModelAssertions;
import iteration2.ui.BaseUITest;
import org.junit.jupiter.api.Test;
import ui.pages.AdminPanel;
import ui.pages.BankAlert;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateUserTest extends BaseUITest {
    @Test
    public void adminCanCreateUserTest(){
        // Шаг 1: админ залогинился в банке
        CreateUserRequest admin = CreateUserRequest.getAdmin();

        authAsUser(admin);

        // Шаг 2: админ создает юзера в банке
        CreateUserRequest newUser = RandomModelGenerator.generate(CreateUserRequest.class);

        new AdminPanel()
                .open()
                .createUser(newUser.getUsername(), newUser.getPassword())
                .checkAlertMessageAndAccept(
                        BankAlert.USER_CREATED_SUCCESSFULLY.getMessage()
                )
                .shouldHaveUser(newUser.getUsername());

        // Шаг 3: Проверка, что юзер создан на API
        CreateUserResponse createdUser = AdminSteps.getAllUsers().stream()
                .filter(user -> user.getUsername().equals(newUser.getUsername()))
                .findFirst().get();

        ModelAssertions.assertThatModels(newUser, createdUser);
    }

    @Test
    public void adminCannotCreateUserWithInvalidDataTest(){
        // Шаг 1: админ залогинился в банке
        CreateUserRequest admin = CreateUserRequest.getAdmin();

        authAsUser(admin);

        // Шаг 2: админ создает юзера в банке
        CreateUserRequest newUser = RandomModelGenerator.generate(CreateUserRequest.class);

        newUser.setUsername("a");
        new AdminPanel().open().createUser(newUser.getUsername(),newUser.getPassword())
                .checkAlertMessageAndAccept(BankAlert.USERNAME_MUST_BE_BETWEEN_3_AND_15_CHARACTERS.getMessage())
                .getAllUsers().findBy(Condition.exactText(newUser.getUsername() + "\nUSER"))
                .shouldNotBe(Condition.exist);

        // Шаг 3: Проверка, что юзер НЕ создан на API
        long userWithSameUsernameAsNewUser = AdminSteps.getAllUsers().stream()
                .filter(user -> user.getUsername().equals(newUser.getUsername())).count();

        assertThat(userWithSameUsernameAsNewUser).isZero();
    }
}
