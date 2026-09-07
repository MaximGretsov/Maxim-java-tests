package iteration2.ui;

import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import generators.RandomModelGenerator;
import io.restassured.specification.RequestSpecification;
import models.*;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import requests.skeleton.Endpoint;
import requests.skeleton.requesters.CrudRequester;
import requests.skeleton.requesters.ValidatedCrudRequester;
import requests.steps.AdminSteps;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.Arrays;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.*;
import static com.codeborne.selenide.Selenide.switchTo;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static requests.steps.AccountSteps.prepareAccountForTransfer;

public class TransferTests extends BaseUITest {
    @Test
    public void userCanTransferMoneyWithCorrectDataTest(){
        // ШАГ 1: создаём пользователя
        CreateUserRequest user = AdminSteps.createUser();

        RequestSpecification userSpec = RequestSpecs.authAsUserSpec(
                user.getUsername(),
                user.getPassword()
        );

        // ШАГ 2: создаём первый счёт — отправитель
        CreateAccountResponse senderAccount = new ValidatedCrudRequester<CreateAccountResponse>(
                userSpec,
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated()
        ).post();

        // ШАГ 3: создаём второй счёт — получатель
        CreateAccountResponse receiverAccount = new ValidatedCrudRequester<CreateAccountResponse>(
                userSpec,
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated()
        ).post();

        // ШАГ 4: пополняем счёт отправителя до 15 000
        prepareAccountForTransfer(
                userSpec,
                (int) senderAccount.getId()
        );

        // ШАГ 5: получаем токен пользователя для авторизации на UI
        String userAuthHeader = new CrudRequester(
                RequestSpecs.unauthSpec(),
                Endpoint.LOGIN,
                ResponseSpecs.requestReturnsOk()
        )
                .post(LoginUserRequest.builder()
                        .username(user.getUsername())
                        .password(user.getPassword())
                        .build())
                .extract()
                .header("Authorization");

        // Сохраняем начальные балансы
        float initialSenderBalance = 15_000f;
        float initialReceiverBalance = 0f;

        // Сохраняем id счетов для UI и дальнейших проверок
        String senderAccountId = String.valueOf(senderAccount.getId());

        int receiverAccountId = (int) receiverAccount.getId();

        String receiverAccountNumber = receiverAccount.getAccountNumber();

        // ШАГ 6: авторизуем пользователя на UI
        Selenide.open("/");

        executeJavaScript(
                "localStorage.setItem('authToken', arguments[0]);",
                userAuthHeader
        );

        Selenide.open("/dashboard");

        // ШАГ 7: делаем трансфер
        $$("button")
                .findBy(text("Make a Transfer"))
                .click();

        $(".account-selector")
                .shouldBe(visible)
                .shouldBe(enabled)
                .selectOptionByValue(senderAccountId);

        $(Selectors.byAttribute("placeholder", "Enter recipient name"))
                .setValue(user.getUsername());

        $(Selectors.byAttribute(
                "placeholder",
                "Enter recipient account number"
        ))
                .setValue(receiverAccountNumber);

        float transferAmount = RandomModelGenerator.generateValidTransferAmount();
        String transferAmountValue = Float.toString(transferAmount);

        $(Selectors.byAttribute("placeholder", "Enter amount"))
                .setValue(transferAmountValue)
                .shouldHave(exactValue(transferAmountValue));

        $("input[type='checkbox']")
                .shouldBe(visible)
                .setSelected(true)
                .shouldBe(selected);

        $$("button")
                .findBy(text("Send Transfer"))
                .shouldBe(visible)
                .shouldBe(enabled)
                .click();

        // ШАГ 8: проверка алерта
        Alert alert = switchTo().alert();

        assertEquals("✅ Successfully transferred $" + transferAmount
                        + " to account " + receiverAccountNumber + "!", alert.getText());

        alert.accept();

        // ШАГ 9: получаем актуальные данные счетов через API
        CreateAccountResponse[] updatedAccounts = given()
                .spec(userSpec)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then().extract()
                .as(CreateAccountResponse[].class);

        // Находим счёт отправителя по id
        CreateAccountResponse updatedSenderAccount = Arrays.stream(updatedAccounts)
                .filter(account -> account.getId() == senderAccount.getId())
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Sender account with id " + senderAccount.getId() + " not found"
                ));

        // Находим счёт получателя по id
        CreateAccountResponse updatedReceiverAccount = Arrays.stream(updatedAccounts)
                .filter(account -> account.getId() == receiverAccountId)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Receiver account with id " + receiverAccountId + " not found"
                ));

        // ШАГ 10: проверяем баланс и транзакцию счёта отправителя
        float expectedSenderBalance =
                initialSenderBalance - transferAmount;

        assertThat((double) updatedSenderAccount.getBalance())
                .isCloseTo(
                        (double) expectedSenderBalance,
                        within(0.001)
                );

        // До перевода у отправителя было 3 депозитные транзакции.
        // После перевода должна появиться четвёртая транзакция.
        assertThat(updatedSenderAccount.getTransactions())
                .hasSize(4);

        assertThat(updatedSenderAccount.getTransactions())
                .anySatisfy(transaction -> {
                    assertThat(transaction.getType())
                            .isEqualTo("TRANSFER_OUT");

                    assertThat(Math.abs((double) transaction.getAmount()))
                            .isCloseTo(
                                    (double) transferAmount,
                                    within(0.001)
                            );
                });

        // ШАГ 11: проверяем баланс и транзакцию счёта получателя
        float expectedReceiverBalance =
                initialReceiverBalance + transferAmount;

        assertThat((double) updatedReceiverAccount.getBalance())
                .isCloseTo(
                        (double) expectedReceiverBalance,
                        within(0.001)
                );

        // До перевода у получателя транзакций не было.
        // После перевода должна появиться одна транзакция.
        assertThat(updatedReceiverAccount.getTransactions())
                .hasSize(1);

        TransactionResponse receiverTransaction =
                updatedReceiverAccount.getTransactions().get(0);

        assertThat(receiverTransaction.getType())
                .isEqualTo("TRANSFER_IN");

        assertThat(Math.abs((double) receiverTransaction.getAmount()))
                .isCloseTo(
                        (double) transferAmount,
                        within(0.001)
                );
    }

    @Test
    public void userCanTransferMoneyToAnotherUserWithCorrectDataTest() {
        // ШАГ 1: создаём пользователя-отправителя
        CreateUserRequest senderUser = AdminSteps.createUser();

        RequestSpecification senderSpec = RequestSpecs.authAsUserSpec(
                senderUser.getUsername(),
                senderUser.getPassword()
        );

        // ШАГ 2: создаём счёт пользователя-отправителя
        CreateAccountResponse senderAccount = new ValidatedCrudRequester<CreateAccountResponse>(
                senderSpec,
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated()
        ).post();

        // ШАГ 3: создаём пользователя-получателя
        CreateUserRequest receiverUser = AdminSteps.createUser();

        RequestSpecification receiverSpec = RequestSpecs.authAsUserSpec(
                receiverUser.getUsername(),
                receiverUser.getPassword()
        );

        // ШАГ 4: создаём счёт пользователя-получателя
        CreateAccountResponse receiverAccount = new ValidatedCrudRequester<CreateAccountResponse>(
                receiverSpec,
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated()
        ).post();

        // ШАГ 5: пополняем счёт отправителя до 15 000
        prepareAccountForTransfer(
                senderSpec,
                (int) senderAccount.getId()
        );

        // ШАГ 6: получаем токен пользователя-отправителя для UI
        String senderAuthHeader = new CrudRequester(
                RequestSpecs.unauthSpec(),
                Endpoint.LOGIN,
                ResponseSpecs.requestReturnsOk()
        )
                .post(LoginUserRequest.builder()
                        .username(senderUser.getUsername())
                        .password(senderUser.getPassword())
                        .build())
                .extract()
                .header("Authorization");

        // Сохраняем начальные балансы
        float initialSenderBalance = 15_000f;
        float initialReceiverBalance = 0f;

        // ID счёта отправителя нужен для выбора счёта на UI
        String senderAccountId =
                String.valueOf(senderAccount.getId());

        // Номер счёта получателя нужен для заполнения формы
        String receiverAccountNumber =
                receiverAccount.getAccountNumber();

        // ШАГ 7: авторизуем пользователя-отправителя на UI
        Selenide.open("/");

        executeJavaScript(
                "localStorage.setItem('authToken', arguments[0]);",
                senderAuthHeader
        );

        Selenide.open("/dashboard");

        // ШАГ 8: открываем форму перевода
        $$("button")
                .findBy(text("Make a Transfer"))
                .shouldBe(visible)
                .click();

        // Выбираем счёт отправителя
        $(".account-selector")
                .shouldBe(visible)
                .shouldBe(enabled)
                .selectOptionByValue(senderAccountId);

        // Вводим username пользователя-получателя
        $(Selectors.byAttribute(
                "placeholder",
                "Enter recipient name"
        ))
                .setValue(receiverUser.getUsername())
                .shouldHave(exactValue(receiverUser.getUsername()));

        // Вводим номер счёта пользователя-получателя
        $(Selectors.byAttribute(
                "placeholder",
                "Enter recipient account number"
        ))
                .setValue(receiverAccountNumber)
                .shouldHave(exactValue(receiverAccountNumber));

        // Генерируем и вводим корректную сумму перевода
        float transferAmount =
                RandomModelGenerator.generateValidTransferAmount();

        String transferAmountValue =
                Float.toString(transferAmount);

        $(Selectors.byAttribute("placeholder", "Enter amount"))
                .setValue(transferAmountValue)
                .shouldHave(exactValue(transferAmountValue));

        // Подтверждаем правильность данных
        $("input[type='checkbox']")
                .shouldBe(visible)
                .setSelected(true)
                .shouldBe(selected);

        // Выполняем перевод
        $$("button")
                .findBy(text("Send Transfer"))
                .shouldBe(visible)
                .shouldBe(enabled)
                .click();

        // ШАГ 9: проверяем сообщение об успешном переводе
        Alert alert = switchTo().alert();

        assertEquals("✅ Successfully transferred $" + transferAmount
                        + " to account " + receiverAccountNumber + "!", alert.getText());

        alert.accept();

        // ШАГ 10: получаем актуальный счёт отправителя через API
        CreateAccountResponse[] senderAccounts = given()
                .spec(senderSpec)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then().extract()
                .as(CreateAccountResponse[].class);

        assertThat(senderAccounts).hasSize(1);

        CreateAccountResponse updatedSenderAccount =
                senderAccounts[0];

        assertThat(updatedSenderAccount.getId())
                .isEqualTo(senderAccount.getId());

        // Проверяем баланс отправителя
        float expectedSenderBalance =
                initialSenderBalance - transferAmount;

        assertThat((double) updatedSenderAccount.getBalance())
                .isCloseTo(
                        (double) expectedSenderBalance,
                        within(0.001)
                );

        // Три депозита при подготовке + одна исходящая транзакция
        assertThat(updatedSenderAccount.getTransactions())
                .hasSize(4);

        assertThat(updatedSenderAccount.getTransactions())
                .anySatisfy(transaction -> {
                    assertThat(transaction.getType())
                            .isEqualTo("TRANSFER_OUT");

                    assertThat(Math.abs((double) transaction.getAmount()))
                            .isCloseTo(
                                    (double) transferAmount,
                                    within(0.001)
                            );
                });

        // ШАГ 11: получаем актуальный счёт получателя через API
        CreateAccountResponse[] receiverAccounts = given()
                .spec(receiverSpec)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then().extract()
                .as(CreateAccountResponse[].class);

        assertThat(receiverAccounts).hasSize(1);

        CreateAccountResponse updatedReceiverAccount =
                receiverAccounts[0];

        assertThat(updatedReceiverAccount.getId())
                .isEqualTo(receiverAccount.getId());

        // Проверяем баланс получателя
        float expectedReceiverBalance =
                initialReceiverBalance + transferAmount;

        assertThat((double) updatedReceiverAccount.getBalance())
                .isCloseTo(
                        (double) expectedReceiverBalance,
                        within(0.001)
                );

        // У получателя должна появиться одна входящая транзакция
        assertThat(updatedReceiverAccount.getTransactions())
                .hasSize(1);

        TransactionResponse receiverTransaction =
                updatedReceiverAccount.getTransactions().get(0);

        assertThat(receiverTransaction.getType())
                .isEqualTo("TRANSFER_IN");

        assertThat(Math.abs((double) receiverTransaction.getAmount()))
                .isCloseTo(
                        (double) transferAmount,
                        within(0.001)
                );
    }

    @Test
    public void userCannotTransferMoneyWithNegativeAmountTest(){
        // ШАГ 1: создаём пользователя
        CreateUserRequest user = AdminSteps.createUser();

        RequestSpecification userSpec = RequestSpecs.authAsUserSpec(
                user.getUsername(),
                user.getPassword()
        );

        // ШАГ 2: создаём первый счёт — отправитель
        CreateAccountResponse senderAccount = new ValidatedCrudRequester<CreateAccountResponse>(
                userSpec,
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated()
        ).post();

        // ШАГ 3: создаём второй счёт — получатель
        CreateAccountResponse receiverAccount = new ValidatedCrudRequester<CreateAccountResponse>(
                userSpec,
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated()
        ).post();

        // ШАГ 4: пополняем счёт отправителя до 15 000
        prepareAccountForTransfer(
                userSpec,
                (int) senderAccount.getId()
        );

        // ШАГ 5: получаем токен пользователя для авторизации на UI
        String userAuthHeader = new CrudRequester(
                RequestSpecs.unauthSpec(),
                Endpoint.LOGIN,
                ResponseSpecs.requestReturnsOk()
        )
                .post(LoginUserRequest.builder()
                        .username(user.getUsername())
                        .password(user.getPassword())
                        .build())
                .extract()
                .header("Authorization");

        // Сохраняем начальные балансы
        float initialSenderBalance = 15_000f;
        float initialReceiverBalance = 0f;

        // Сохраняем id счетов для UI и дальнейших проверок
        String senderAccountId = String.valueOf(senderAccount.getId());

        int receiverAccountId = (int) receiverAccount.getId();

        String receiverAccountNumber = receiverAccount.getAccountNumber();

        // ШАГ 6: авторизуем пользователя на UI
        Selenide.open("/");

        executeJavaScript(
                "localStorage.setItem('authToken', arguments[0]);",
                userAuthHeader
        );

        Selenide.open("/dashboard");

        // ШАГ 7: делаем трансфер
        $$("button")
                .findBy(text("Make a Transfer"))
                .click();

        $(".account-selector")
                .shouldBe(visible)
                .shouldBe(enabled)
                .selectOptionByValue(senderAccountId);

        $(Selectors.byAttribute("placeholder", "Enter recipient name"))
                .setValue(user.getUsername());

        $(Selectors.byAttribute(
                "placeholder",
                "Enter recipient account number"
        ))
                .setValue(receiverAccountNumber);

        float negativeTransferAmount = RandomModelGenerator.generateNegativeTransferAmount();
        String transferAmountValue = Float.toString(negativeTransferAmount);

        $(Selectors.byAttribute("placeholder", "Enter amount"))
                .setValue(transferAmountValue)
                .shouldHave(exactValue(transferAmountValue));

        $("input[type='checkbox']")
                .shouldBe(visible)
                .setSelected(true)
                .shouldBe(selected);

        $$("button")
                .findBy(text("Send Transfer"))
                .shouldBe(visible)
                .shouldBe(enabled)
                .click();

        // ШАГ 8: проверка негативного алерта
        Alert alert = switchTo().alert();

        assertEquals("❌ Error: Transfer amount must be at least 0.01", alert.getText());

        alert.accept();

        // ШАГ 9: получаем актуальное состояние счетов через API
        CreateAccountResponse[] updatedAccounts = given()
                .spec(userSpec)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then().extract()
                .as(CreateAccountResponse[].class);

        // Находим счёт отправителя по id
        CreateAccountResponse updatedSenderAccount = Arrays.stream(updatedAccounts)
                .filter(account -> account.getId() == senderAccount.getId())
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Sender account with id "
                                + senderAccount.getId()
                                + " not found"
                ));

        // Находим счёт получателя по id
        CreateAccountResponse updatedReceiverAccount = Arrays.stream(updatedAccounts)
                .filter(account -> account.getId() == receiverAccountId)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Receiver account with id "
                                + receiverAccountId
                                + " not found"
                ));

        // ШАГ 10: проверяем, что баланс отправителя не изменился
        assertThat((double) updatedSenderAccount.getBalance())
                .isCloseTo(
                        (double) initialSenderBalance,
                        within(0.001)
                );

        // У отправителя остаются только три депозитные транзакции,
        // созданные во время подготовки счёта
        assertThat(updatedSenderAccount.getTransactions())
                .hasSize(3);

        assertThat(updatedSenderAccount.getTransactions())
                .allSatisfy(transaction ->
                        assertThat(transaction.getType())
                                .isEqualTo("DEPOSIT")
                );

        // ШАГ 11: проверяем, что баланс получателя не изменился
        assertThat((double) updatedReceiverAccount.getBalance())
                .isCloseTo(
                        (double) initialReceiverBalance,
                        within(0.001)
                );

        // Перевод отклонён, поэтому у получателя транзакций нет
        assertThat(updatedReceiverAccount.getTransactions())
                .isEmpty();
    }

    @Test
    public void userCanRepeatTransferWithCorrectDataTest() {
        // ШАГ 1: создаём пользователя
        CreateUserRequest user = AdminSteps.createUser();

        RequestSpecification userSpec = RequestSpecs.authAsUserSpec(
                user.getUsername(),
                user.getPassword()
        );

        // ШАГ 2: создаём первый счёт — отправитель
        CreateAccountResponse senderAccount =new ValidatedCrudRequester<CreateAccountResponse>(
                userSpec,
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated()
        ).post();

        // ШАГ 3: создаём второй счёт — получатель
        CreateAccountResponse receiverAccount = new ValidatedCrudRequester<CreateAccountResponse>(
                userSpec,
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated()
        ).post();

        int senderAccountId = (int) senderAccount.getId();
        int receiverAccountId = (int) receiverAccount.getId();

        // ШАГ 4: пополняем счёт отправителя до 15 000
        // Метод создаёт три депозитные транзакции по 5000
        prepareAccountForTransfer(userSpec, senderAccountId);

        float initialSenderBalance = 15_000f;
        float initialReceiverBalance = 0f;

        // ШАГ 5: выполняем первоначальный перевод через API
        // Этот перевод потом найдём и повторим через UI
        // (делаем generateValidDepositAmount так как он до 5000 тысяч и мы при двух транзакциях не выйдем за 15000)
        float initialTransferAmount = RandomModelGenerator.generateValidDepositAmount();

        TransferRequest initialTransferRequest = TransferRequest.builder()
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .amount(initialTransferAmount)
                .build();

        new CrudRequester(
                userSpec,
                Endpoint.TRANSFER,
                ResponseSpecs.requestReturnsOk()
        ).post(initialTransferRequest);

        // ШАГ 6: получаем токен пользователя для авторизации на UI
        String userAuthHeader = new CrudRequester(
                RequestSpecs.unauthSpec(),
                Endpoint.LOGIN,
                ResponseSpecs.requestReturnsOk()
        )
                .post(LoginUserRequest.builder()
                        .username(user.getUsername())
                        .password(user.getPassword())
                        .build())
                .extract()
                .header("Authorization");

        // ШАГ 7: авторизуем пользователя на UI
        Selenide.open("/");

        executeJavaScript(
                "localStorage.setItem('authToken', arguments[0]);",
                userAuthHeader
        );

        // ШАГ 8: Делаем повторный трансфер
        Selenide.open("/dashboard");

        $$("button")
                .findBy(text("Make a Transfer"))
                .shouldBe(visible)
                .click();

        $$("button")
                .findBy(text("Transfer Again"))
                .shouldBe(visible)
                .click();

        $(Selectors.byAttribute("placeholder", "Enter name to find transactions"))
                .setValue(user.getUsername());

        $$("button")
                .findBy(text("Search Transactions"))
                .shouldBe(visible)
                .shouldBe(enabled)
                .click();

        // Находим исходящий трансфер(он гарантированно единственный) и повторяем его
        $$("li.list-group-item")
                .findBy(text("TRANSFER_IN"))
                .$("button")
                .shouldBe(visible)
                .click();

        $(".modal.show")
                .shouldBe(visible)
                .shouldHave(text("Repeat Transfer"));

        $(".modal.show select")
                .shouldBe(visible)
                .shouldBe(enabled)
                .selectOptionByValue(String.valueOf(senderAccountId));

        float repeatTransferAmount = initialTransferAmount;

        $(".modal.show input[type='number']")
                .setValue(Float.toString(repeatTransferAmount))
                .shouldHave(exactValue(Float.toString(repeatTransferAmount)));

        $(".modal.show input[type='checkbox']")
                .setSelected(true)
                .shouldBe(selected);

        $(".modal.show")
                .$$("button")
                .findBy(text("Send Transfer"))
                .shouldBe(visible)
                .shouldBe(enabled)
                .click();

        // ШАГ 9: Алерт об успешном трансфере
        Alert alert = switchTo().alert();

        assertEquals("✅ Transfer of $" + repeatTransferAmount + " successful from Account "
                        + senderAccountId + " to " + receiverAccountId + "!", alert.getText());

        alert.accept();

        // ШАГ 10: получаем актуальные данные счетов через API
        CreateAccountResponse[] updatedAccounts = given()
                .spec(userSpec)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then().extract()
                .as(CreateAccountResponse[].class);

        assertThat(updatedAccounts).hasSize(2);

        // Находим счёт-отправитель
        CreateAccountResponse updatedSenderAccount =
                Arrays.stream(updatedAccounts)
                        .filter(account ->
                                account.getId() == senderAccountId
                        )
                        .findFirst()
                        .orElseThrow(() -> new AssertionError(
                                "Sender account with id "
                                        + senderAccountId
                                        + " not found"
                        ));

        // Находим счёт-получатель
        CreateAccountResponse updatedReceiverAccount =
                Arrays.stream(updatedAccounts)
                        .filter(account ->
                                account.getId() == receiverAccountId
                        )
                        .findFirst()
                        .orElseThrow(() -> new AssertionError(
                                "Receiver account with id "
                                        + receiverAccountId
                                        + " not found"
                        ));

        // ШАГ 11: проверяем счёт-отправитель
        // С отправителя списалась сумма первоначального
        // и повторного переводов
        float expectedSenderBalance =
                initialSenderBalance
                        - initialTransferAmount
                        - repeatTransferAmount;

        assertThat((double) updatedSenderAccount.getBalance())
                .isCloseTo(
                        (double) expectedSenderBalance,
                        within(0.001)
                );

        // На счёте отправителя должно быть два исходящих перевода:
        // первоначальный перевод и повторный перевод
        assertThat(updatedSenderAccount.getTransactions())
                .filteredOn(transaction ->
                        "TRANSFER_OUT".equals(transaction.getType())
                )
                .hasSize(2)
                .allSatisfy(transaction -> {
                    assertThat((double) transaction.getAmount())
                            .isCloseTo(
                                    (double) initialTransferAmount,
                                    within(0.001)
                            );

                    assertThat(transaction.getRelatedAccountId())
                            .isEqualTo(receiverAccountId);
                });

        // На счёте отправителя не должно быть входящих переводов
        assertThat(updatedSenderAccount.getTransactions())
                .filteredOn(transaction ->
                        "TRANSFER_IN".equals(transaction.getType())
                )
                .isEmpty();

        // ШАГ 12: проверяем счёт-получатель
        // На получателя поступила сумма первоначального
        // и повторного переводов
        float expectedReceiverBalance =
                initialReceiverBalance
                        + initialTransferAmount
                        + repeatTransferAmount;

        assertThat((double) updatedReceiverAccount.getBalance())
                .isCloseTo(
                        (double) expectedReceiverBalance,
                        within(0.001)
                );

        // На счёте получателя должно быть два входящих перевода:
        // первоначальный перевод и повторный перевод
        assertThat(updatedReceiverAccount.getTransactions())
                .filteredOn(transaction ->
                        "TRANSFER_IN".equals(transaction.getType())
                )
                .hasSize(2)
                .allSatisfy(transaction -> {
                    assertThat((double) transaction.getAmount())
                            .isCloseTo(
                                    (double) initialTransferAmount,
                                    within(0.001)
                            );

                    assertThat(transaction.getRelatedAccountId())
                            .isEqualTo(senderAccountId);
                });

        // На счёте получателя не должно быть исходящих переводов
        assertThat(updatedReceiverAccount.getTransactions())
                .filteredOn(transaction ->
                        "TRANSFER_OUT".equals(transaction.getType())
                )
                .isEmpty();
    }

    @Test
    public void userCannotFindTransactionsByNonExistingUserTest() {
        // ШАГ 1: создаём пользователя
        CreateUserRequest user = AdminSteps.createUser();

        RequestSpecification userSpec = RequestSpecs.authAsUserSpec(
                user.getUsername(),
                user.getPassword()
        );

        // ШАГ 2: создаём первый счёт — отправитель
        CreateAccountResponse senderAccount = new ValidatedCrudRequester<CreateAccountResponse>(
                userSpec,
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated()
        ).post();

        // ШАГ 3: создаём второй счёт — получатель
        CreateAccountResponse receiverAccount = new ValidatedCrudRequester<CreateAccountResponse>(
                userSpec,
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated()
        ).post();

        // ШАГ 4: получаем токен пользователя
        String userAuthHeader = new CrudRequester(
                RequestSpecs.unauthSpec(),
                Endpoint.LOGIN,
                ResponseSpecs.requestReturnsOk()
        )
                .post(LoginUserRequest.builder()
                        .username(user.getUsername())
                        .password(user.getPassword())
                        .build())
                .extract()
                .header("Authorization");

        // ШАГ 5: авторизуем пользователя на UI
        Selenide.open("/");

        executeJavaScript(
                "localStorage.setItem('authToken', arguments[0]);",
                userAuthHeader
        );

        Selenide.open("/dashboard");

        // ШАГ 6: открываем раздел Transfer Again
        $$("button")
                .findBy(text("Make a Transfer"))
                .shouldBe(visible)
                .click();

        $$("button")
                .findBy(text("Transfer Again"))
                .shouldBe(visible)
                .click();

        // ШАГ 7: вводим username, которого точно не существует
        String nonExistingUsername = RandomModelGenerator.generateStringValue();

        $(Selectors.byAttribute("placeholder", "Enter name to find transactions"))
                .setValue(nonExistingUsername)
                .shouldHave(exactValue(nonExistingUsername));

        // ШАГ 8: выполняем поиск
        $$("button")
                .findBy(text("Search Transactions"))
                .shouldBe(visible)
                .shouldBe(enabled)
                .click();

        // ШАГ 9: проверяем alert о неуспешном поиске
        Alert alert = switchTo().alert();

        assertEquals("❌ No matching users found.", alert.getText());

        alert.accept();

        // ШАГ 10: проверяем, что результатов поиска нет
        $("ul.list-group")
                .should(exist)
                .$$("li.list-group-item")
                .shouldHave(size(0));

        // Дополнительно проверяем отсутствие кнопок Repeat
        $$("button")
                .filterBy(text("Repeat"))
                .shouldHave(size(0));
    }

    @Test
    public void userCannotRepeatTransferWithIncorrectAmountTest() {
        // ШАГ 1: создаём пользователя
        CreateUserRequest user = AdminSteps.createUser();

        RequestSpecification userSpec = RequestSpecs.authAsUserSpec(
                user.getUsername(),
                user.getPassword()
        );

        // ШАГ 2: создаём первый счёт — отправитель
        CreateAccountResponse senderAccount =
                new ValidatedCrudRequester<CreateAccountResponse>(
                        userSpec,
                        Endpoint.ACCOUNTS,
                        ResponseSpecs.entityWasCreated()
                ).post();

        // ШАГ 3: создаём второй счёт — получатель
        CreateAccountResponse receiverAccount =
                new ValidatedCrudRequester<CreateAccountResponse>(
                        userSpec,
                        Endpoint.ACCOUNTS,
                        ResponseSpecs.entityWasCreated()
                ).post();

        int senderAccountId = (int) senderAccount.getId();
        int receiverAccountId = (int) receiverAccount.getId();

        // ШАГ 4: пополняем счёт отправителя до 15 000
        // Метод создаёт три депозитные транзакции по 5000
        prepareAccountForTransfer(userSpec, senderAccountId);

        float initialSenderBalance = 15_000f;
        float initialReceiverBalance = 0f;

        // ШАГ 5: выполняем первоначальный успешный перевод через API
        // Этот перевод затем найдём в разделе Transfer Again
        float initialTransferAmount =
                RandomModelGenerator.generateValidDepositAmount();

        TransferRequest initialTransferRequest = TransferRequest.builder()
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .amount(initialTransferAmount)
                .build();

        new CrudRequester(
                userSpec,
                Endpoint.TRANSFER,
                ResponseSpecs.requestReturnsOk()
        ).post(initialTransferRequest);

        // Состояние счетов после первоначального успешного перевода
        float senderBalanceAfterInitialTransfer =
                initialSenderBalance - initialTransferAmount;

        float receiverBalanceAfterInitialTransfer =
                initialReceiverBalance + initialTransferAmount;

        // ШАГ 6: получаем токен пользователя для авторизации на UI
        String userAuthHeader = new CrudRequester(
                RequestSpecs.unauthSpec(),
                Endpoint.LOGIN,
                ResponseSpecs.requestReturnsOk()
        )
                .post(LoginUserRequest.builder()
                        .username(user.getUsername())
                        .password(user.getPassword())
                        .build())
                .extract()
                .header("Authorization");

        // ШАГ 7: авторизуем пользователя на UI
        Selenide.open("/");

        executeJavaScript(
                "localStorage.setItem('authToken', arguments[0]);",
                userAuthHeader
        );


        // ШАГ 8: Делаем повторный трансфер
        Selenide.open("/dashboard");

        $$("button")
                .findBy(text("Make a Transfer"))
                .shouldBe(visible)
                .click();

        $$("button")
                .findBy(text("Transfer Again"))
                .shouldBe(visible)
                .click();

        $(Selectors.byAttribute("placeholder", "Enter name to find transactions"))
                .setValue(user.getUsername());

        $$("button")
                .findBy(text("Search Transactions"))
                .shouldBe(visible)
                .shouldBe(enabled)
                .click();

        // Находим входящую транзакцию и повторяем перевод
        $$("li.list-group-item")
                .findBy(text("TRANSFER_IN"))
                .$("button")
                .shouldBe(visible)
                .click();

        $(".modal.show")
                .shouldBe(visible)
                .shouldHave(text("Repeat Transfer"));

        $(".modal.show select")
                .shouldBe(visible)
                .shouldBe(enabled)
                .selectOptionByValue(String.valueOf(senderAccountId));

        float invalidRepeatTransferAmount = RandomModelGenerator.generateNegativeTransferAmount();

        $(".modal.show input[type='number']")
                .setValue(Float.toString(invalidRepeatTransferAmount))
                .shouldHave(exactValue(Float.toString(invalidRepeatTransferAmount)));

        $(".modal.show input[type='checkbox']")
                .setSelected(true)
                .shouldBe(selected);

        $(".modal.show")
                .$$("button")
                .findBy(text("Send Transfer"))
                .shouldBe(visible)
                .shouldBe(enabled)
                .click();

        // ШАГ 9: проверяем alert о неуспешном трансфере
        Alert alert = switchTo().alert();

        assertEquals("❌ Transfer failed: Please try again.", alert.getText());

        alert.accept();

        // ШАГ 10: получаем актуальные данные счетов через API
        CreateAccountResponse[] updatedAccounts = new CrudRequester(
                userSpec,
                Endpoint.CUSTOMER_ACCOUNTS,
                ResponseSpecs.requestReturnsOk()
        )
                .get()
                .extract()
                .as(CreateAccountResponse[].class);

        assertThat(updatedAccounts).hasSize(2);

        // Находим счёт-отправитель
        CreateAccountResponse updatedSenderAccount =
                Arrays.stream(updatedAccounts)
                        .filter(account ->
                                account.getId() == senderAccountId
                        )
                        .findFirst()
                        .orElseThrow(() -> new AssertionError(
                                "Sender account with id "
                                        + senderAccountId
                                        + " not found"
                        ));

        // Находим счёт-получатель
        CreateAccountResponse updatedReceiverAccount =
                Arrays.stream(updatedAccounts)
                        .filter(account ->
                                account.getId() == receiverAccountId
                        )
                        .findFirst()
                        .orElseThrow(() -> new AssertionError(
                                "Receiver account with id "
                                        + receiverAccountId
                                        + " not found"
                        ));

        // ШАГ 11: проверяем, что баланс отправителя не изменился
        // после неуспешного повторения
        assertThat((double) updatedSenderAccount.getBalance())
                .isCloseTo(
                        (double) senderBalanceAfterInitialTransfer,
                        within(0.001)
                );

        // У отправителя остался только один успешный TRANSFER_OUT
        assertThat(updatedSenderAccount.getTransactions())
                .filteredOn(transaction ->
                        "TRANSFER_OUT".equals(transaction.getType())
                )
                .hasSize(1)
                .allSatisfy(transaction -> {
                    assertThat((double) transaction.getAmount())
                            .isCloseTo(
                                    (double) initialTransferAmount,
                                    within(0.001)
                            );

                    assertThat(transaction.getRelatedAccountId())
                            .isEqualTo(receiverAccountId);
                });

        // ШАГ 12: проверяем, что баланс получателя не изменился
        // после неуспешного повторения
        assertThat((double) updatedReceiverAccount.getBalance())
                .isCloseTo(
                        (double) receiverBalanceAfterInitialTransfer,
                        within(0.001)
                );

        // У получателя остался только один успешный TRANSFER_IN
        assertThat(updatedReceiverAccount.getTransactions())
                .filteredOn(transaction ->
                        "TRANSFER_IN".equals(transaction.getType())
                )
                .hasSize(1)
                .allSatisfy(transaction -> {
                    assertThat((double) transaction.getAmount())
                            .isCloseTo(
                                    (double) initialTransferAmount,
                                    within(0.001)
                            );

                    assertThat(transaction.getRelatedAccountId())
                            .isEqualTo(senderAccountId);
                });
    }
}
