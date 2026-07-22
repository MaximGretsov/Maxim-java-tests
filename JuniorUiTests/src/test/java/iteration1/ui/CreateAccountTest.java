package iteration1.ui;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import models.CreateAccountResponse;
import models.CreateUserRequest;
import models.LoginUserRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import requests.skeleton.Endpoint;
import requests.skeleton.requesters.CrudRequester;
import requests.steps.AdminSteps;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.codeborne.selenide.Selenide.*;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class CreateAccountTest {
    @BeforeAll
    public static void setUpSelenoid(){
        Configuration.remote = "http://localhost:4444/wd/hub";
        Configuration.baseUrl = "http://192.168.3.11:3000";
        Configuration.browser = "chrome";
        Configuration.browserSize = "1920x1080";

        Configuration.browserCapabilities.setCapability("selenoid:options",
                Map.of("enableVNC",true, "enableLog", true));
    }

    @Test
    public void userCanCreateAccountTest(){
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
        // Шаг 4: Юзер создает аккаунт
        Selenide.open("/dashboard");

        $(Selectors.byText("➕ Create New Account")).click();

        // Шаг 5: Проверка алерта
        Alert alert = switchTo().alert();
        String alertText = alert.getText();

        assertThat(alert.getText()).contains("✅ New Account Created! Account Number:");

        alert.accept();

        Pattern pattern = Pattern.compile("Account Number: (\\w+)");
        Matcher matcher = pattern.matcher(alertText);

        matcher.find();

        String createdAccNumber = matcher.group(1);

        // Шаг 6: аккаунт был создан на API
        CreateAccountResponse[] existingUserAccounts = given()
                .spec(RequestSpecs.authAsUserSpec(user.getUsername(),user.getPassword()))
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then().assertThat()
                .extract().as(CreateAccountResponse[].class);

        assertThat(existingUserAccounts).hasSize(1);

        CreateAccountResponse createdAccount = existingUserAccounts[0];

        assertThat(createdAccount).isNotNull();
        assertThat(createdAccount.getBalance()).isZero();

    }
}
