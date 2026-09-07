package iteration2.ui;

import com.codeborne.selenide.*;
import generators.RandomModelGenerator;
import io.restassured.specification.RequestSpecification;
import models.CreateAccountResponse;
import models.CreateUserRequest;
import models.LoginUserRequest;
import models.TransactionResponse;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import requests.skeleton.Endpoint;
import requests.skeleton.requesters.CrudRequester;
import requests.skeleton.requesters.ValidatedCrudRequester;
import requests.steps.AdminSteps;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.*;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DepositTests extends BaseUITest{
    @Test
    public void userCanDepositMoneyWithCorrectDataTest(){
        // Шаги по настройке окружения
        // Шаг 1: Админ логинится в банке
        // Шаг 2: Админ создает юзера
        CreateUserRequest user = AdminSteps.createUser();

        RequestSpecification userSpec = RequestSpecs.authAsUserSpec(
                user.getUsername(),
                user.getPassword()
        );
        // Шаг 3: создаём счёт юзеру через API
        CreateAccountResponse account =  new ValidatedCrudRequester<CreateAccountResponse>(
                userSpec,
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated()
        ).post();

        String accountId = String.valueOf(account.getId());

        // Шаг 4: получаем токен юзера для UI
        String userAuthHeader = new CrudRequester(
                RequestSpecs.unauthSpec(),
                Endpoint.LOGIN,
                ResponseSpecs.requestReturnsOk()
        ).post(LoginUserRequest.builder().username(user.getUsername()).password(user.getPassword()).build())
                .extract()
                .header("Authorization");

        // Шаг 5: Юзер логинится в банке
        Selenide.open("/");
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", userAuthHeader);

        // Шаг 6: Депозит на аккаунт
        float depositAmount =
                RandomModelGenerator.generateValidDepositAmount();

        Selenide.open("/dashboard");

        $$("button")
                .findBy(text("Deposit Money"))
                .click();

        $(".account-selector")
                .shouldBe(visible)
                .shouldBe(enabled)
                .selectOptionByValue(accountId);

        $(Selectors.byAttribute("placeholder", "Enter amount"))
                .setValue(Float.toString(depositAmount))
                .shouldHave(exactValue(Float.toString(depositAmount)));

        $$("button")
                .findBy(text("Deposit"))
                .shouldBe(enabled)
                .click();

        // Шаг 7: Проверка алерта
        Alert alert = switchTo().alert();

        assertEquals("✅ Successfully deposited $" + depositAmount
                + " to account ACC" + accountId + "!", alert.getText());

        alert.accept();

        // Шаг 8: баланс поменялся на API и есть транзакция
        CreateAccountResponse[] accounts = given()
                .spec(userSpec)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then().extract()
                .as(CreateAccountResponse[].class);

        assertThat(accounts).hasSize(1);

        CreateAccountResponse updatedAccount = accounts[0];

        assertThat((double) updatedAccount.getBalance())
                .isCloseTo((double) depositAmount, within(0.001));

        assertThat(updatedAccount.getTransactions()).hasSize(1);

        TransactionResponse transaction =
                updatedAccount.getTransactions().getFirst();

        assertThat(transaction.getAmount())
                .isCloseTo(depositAmount, within(0.001f));

        assertThat(transaction.getType())
                .isEqualTo("DEPOSIT");
    }

    @Test
    public void userCannotDepositMoneyWithIncorrectDataTest(){
        // Шаги по настройке окружения
        // Шаг 1: Админ логинится в банке
        // Шаг 2: Админ создает юзера
        CreateUserRequest user = AdminSteps.createUser();

        RequestSpecification userSpec = RequestSpecs.authAsUserSpec(
                user.getUsername(),
                user.getPassword()
        );
        // Шаг 3: создаём счёт юзеру через API
        CreateAccountResponse account =  new ValidatedCrudRequester<CreateAccountResponse>(
                userSpec,
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated()
        ).post();

        String accountId = String.valueOf(account.getId());

        // Шаг 4: получаем токен юзера для UI
        String userAuthHeader = new CrudRequester(
                RequestSpecs.unauthSpec(),
                Endpoint.LOGIN,
                ResponseSpecs.requestReturnsOk()
        ).post(LoginUserRequest.builder().username(user.getUsername()).password(user.getPassword()).build())
                .extract()
                .header("Authorization");

        // Шаг 5: Юзер логинится в банке
        Selenide.open("/");
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", userAuthHeader);

        // Шаг 6: Депозит на аккаунт
        float incorrectDepositAmount =
                RandomModelGenerator.generateNegativeDepositAmount();

        Selenide.open("/dashboard");

        $$("button")
                .findBy(text("Deposit Money"))
                .click();

        $(".account-selector")
                .shouldBe(visible)
                .shouldBe(enabled)
                .selectOptionByValue(accountId);

        $(Selectors.byAttribute("placeholder", "Enter amount"))
                .setValue(Float.toString(incorrectDepositAmount))
                .shouldHave(exactValue(Float.toString(incorrectDepositAmount)));

        $$("button")
                .findBy(text("Deposit"))
                .shouldBe(enabled)
                .click();

        // Шаг 7: Проверка алерта
        Alert alert = switchTo().alert();

        assertEquals("❌ Please enter a valid amount.", alert.getText());

        alert.accept();

        // Шаг 8: баланс НЕ поменялся на API
        CreateAccountResponse[] accounts = given()
                .spec(userSpec)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then().extract()
                .as(CreateAccountResponse[].class);

        assertThat(accounts).hasSize(1);

        CreateAccountResponse updatedAccount = accounts[0];

        assertThat((double) updatedAccount.getBalance())
                .isCloseTo(0.0, within(0.001));

        assertThat(updatedAccount.getTransactions())
                .isEmpty();
    }
}
