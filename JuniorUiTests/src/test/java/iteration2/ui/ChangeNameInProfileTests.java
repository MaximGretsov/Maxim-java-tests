package iteration2.ui;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import generators.RandomModelGenerator;
import models.CreateUserRequest;
import models.CustomerProfileResponse;
import models.LoginUserRequest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import requests.skeleton.Endpoint;
import requests.skeleton.requesters.CrudRequester;
import requests.steps.AdminSteps;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.*;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChangeNameInProfileTests extends BaseUITest{
    @Test
    public void userCanUpdateProfileNameWithCorrectDataTest(){
        // Шаги по настройке окружения
        // Шаг 1: Админ логинится в банке
        // Шаг 2: Админ создает юзера
        CreateUserRequest user = AdminSteps.createUser();
        // Шаг 3: Юзер логинится в банке
        String userAuthHeader = new CrudRequester(
                RequestSpecs.unauthSpec(),
                Endpoint.LOGIN,
                ResponseSpecs.requestReturnsOk()
        ).post(LoginUserRequest.builder().username(user.getUsername()).password(user.getPassword()).build())
                .extract()
                .header("Authorization");

        Selenide.open("/");
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", userAuthHeader);

        // Шаги теста
        // Шаг 4: Юзер переходит в раздел создания профиля и меняет имя
        String correctNewName = RandomModelGenerator.generateValidProfileName();

        Selenide.open("/dashboard");
        $(".user-info").shouldBe(visible).click();
        $(Selectors.byAttribute("placeholder","Enter new name")).setValue(correctNewName);
        $$("button")
                .findBy(Condition.text("Save Changes"))
                .click();

        // Шаг 5: Проверка алерта
        Alert alert = switchTo().alert();

        assertEquals("✅ Name updated successfully!", alert.getText());

        alert.accept();

        // Шаг 6: проверка отображения на UI на странице пользователя
        $$("button")
                .findBy(Condition.text("Home"))
                .click();

        $(".welcome-text span")
                .shouldBe(visible)
                .shouldHave(exactText(correctNewName));

        // Шаг 7: имя пользователя изменилось на API
        CustomerProfileResponse profile = given()
                .spec(RequestSpecs.authAsUserSpec(
                        user.getUsername(),
                        user.getPassword()
                ))
                .get("http://localhost:4111/api/v1/customer/profile")
                .then().extract()
                .as(CustomerProfileResponse.class);

        assertThat(profile.getName()).isEqualTo(correctNewName);
    }

    @Test
    public void userCannotUpdateProfileNameWithIncorrectDataTest(){
        // Шаги по настройке окружения
        // Шаг 1: Админ логинится в банке
        // Шаг 2: Админ создает юзера
        CreateUserRequest user = AdminSteps.createUser();
        // Шаг 3: Юзер логинится в банке
        String userAuthHeader = new CrudRequester(
                RequestSpecs.unauthSpec(),
                Endpoint.LOGIN,
                ResponseSpecs.requestReturnsOk()
        ).post(LoginUserRequest.builder().username(user.getUsername()).password(user.getPassword()).build())
                .extract()
                .header("Authorization");

        Selenide.open("/");
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", userAuthHeader);

        // Шаги теста
        // Шаг 4: Юзер переходит в раздел создания профиля и меняет имя
        // generateStringValue - сгенирирует слово из 10 букв, что не подходит к требованиям имени
        String incorrectNewName = RandomModelGenerator.generateStringValue();

        Selenide.open("/dashboard");
        $(".user-info").shouldBe(visible).click();
        $(Selectors.byAttribute("placeholder","Enter new name")).setValue(incorrectNewName);
        $$("button")
                .findBy(Condition.text("Save Changes"))
                .click();

        // Шаг 5: Проверка алерта
        Alert alert = switchTo().alert();

        assertEquals("❌ Please enter a valid name.", alert.getText());

        alert.accept();

        // Шаг 6: проверка отображения изначального имени на UI на странице пользователя
        $$("button")
                .findBy(Condition.text("Home"))
                .click();

        $(".welcome-text span")
                .shouldBe(visible)
                .shouldHave(exactText("noname"));

        // Шаг 7: имя пользователя НЕ изменилось на API
        CustomerProfileResponse profile = given()
                .spec(RequestSpecs.authAsUserSpec(
                        user.getUsername(),
                        user.getPassword()
                ))
                .get("http://localhost:4111/api/v1/customer/profile")
                .then().extract()
                .as(CustomerProfileResponse.class);

        assertThat(profile.getName()).isNull();
    }
}
