package iteration2;

import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import iteration1.BaseTest;
import models.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import request.*;
import specs.RequestsSpecs;
import specs.ResponseSpecs;

import java.util.stream.Stream;

import static TestData.AccountTestData.*;
import static generators.RandomData.*;
import static iteration2.assertions.AccountAssertions.assertAccountBalance;
import static request.steps.AccountSteps.*;
import static request.steps.UserSteps.createUserAndGetAuthSpec;


public class TransferTests extends BaseTest {
    // позитивный тест на трансфер между своими аккаунтами
    @Test
    public void userCanTransferBetweenOwnAccountsWithCorrectData(){
        RequestSpecification userSpec = createUserAndGetAuthSpec();
        int senderAccountId = createAccount(userSpec);
        int receiverAccountId = createAccount(userSpec);

        depositMoneyOnAccount(userSpec, senderAccountId, SMALL_SENDER_BALANCE, SMALL_SENDER_BALANCE);
        depositMoneyOnAccount(userSpec, senderAccountId, SMALL_SENDER_BALANCE, SMALL_SENDER_BALANCE*2);
        depositMoneyOnAccount(userSpec, senderAccountId, SMALL_SENDER_BALANCE, PREPARED_SENDER_BALANCE);

        // подготовка ожидаемого баланса для отправителя и получателя
        float amount = generateValidTransferAmount();
        float senderInitialBalance = PREPARED_SENDER_BALANCE;
        float receiverInitialBalance = EMPTY_ACCOUNT_BALANCE;
        float senderExpectedBalance = senderInitialBalance - amount;
        float receiverExpectedBalance = receiverInitialBalance + amount;

        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .amount(amount)
                .build();

        new TransferRequester(
                userSpec,
                ResponseSpecs.successfulTransferResponse(amount, senderAccountId, receiverAccountId)
        ).post(transferRequest);

        // проверка измененного баланса у отправителя
        assertAccountBalance(softy, userSpec, senderAccountId, senderExpectedBalance);

        // проверка измененного баланса у получателя
        assertAccountBalance(softy, userSpec, receiverAccountId, receiverExpectedBalance);
    }

    // позитивные тесты на трансфер на чужой аккаунт
    public static Stream<Arguments> correctTransferData(){
        return Stream.of(
                // трансфер минимально допустимой суммы
                Arguments.of(0.01f, 0.01f),
                //  трансфер чуть ниже максимально возможной суммы
                Arguments.of(9999.99f, 9999.99f),
                //  трансфер максимально возможной суммы
                Arguments.of(10000f, 10000f)
        );
    }
    @MethodSource("correctTransferData")
    @ParameterizedTest
    public void userCanTransferToAnotherAccountWithCorrectData(float  amount, float expectedAmount){
        RequestSpecification user1Spec = createUserAndGetAuthSpec();
        int senderAccountId = createAccount(user1Spec);

        depositMoneyOnAccount(user1Spec, senderAccountId, SMALL_SENDER_BALANCE, SMALL_SENDER_BALANCE);
        depositMoneyOnAccount(user1Spec, senderAccountId, SMALL_SENDER_BALANCE, SMALL_SENDER_BALANCE*2);
        depositMoneyOnAccount(user1Spec, senderAccountId, SMALL_SENDER_BALANCE, PREPARED_SENDER_BALANCE);

        RequestSpecification user2Spec = createUserAndGetAuthSpec();
        int receiverAccountId = createAccount(user2Spec);

        // подготовка ожидаемого баланса для отправителя и получателя
        float senderInitialBalance = PREPARED_SENDER_BALANCE;
        float receiverInitialBalance = EMPTY_ACCOUNT_BALANCE;
        float senderExpectedBalance = senderInitialBalance - amount;
        float receiverExpectedBalance = receiverInitialBalance + amount;

        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .amount(amount)
                .build();

        new TransferRequester(
                user1Spec,
                ResponseSpecs.successfulTransferResponse(expectedAmount, senderAccountId, receiverAccountId)
        ).post(transferRequest);

        // проверка измененного баланса у отправителя
        assertAccountBalance(softy, user1Spec, senderAccountId, senderExpectedBalance);

        // проверка измененного баланса у получателя
        assertAccountBalance(softy, user2Spec, receiverAccountId, receiverExpectedBalance);
    }

    // негативный тест c невалидной суммой трансфера
    public static Stream<Arguments> invalidTransferAmountData() {
        return Stream.of(
                Arguments.of(
                        0f,
                        ResponseSpecs.transferAmountLessThanMin()
                ),
                Arguments.of(
                        -100f,
                        ResponseSpecs.transferAmountLessThanMin()
                ),
                Arguments.of(
                        10000.01f,
                        ResponseSpecs.transferAmountMoreThanMax()
                )
        );
    }
    @MethodSource("invalidTransferAmountData")
    @ParameterizedTest
    public void userCannotTransferBetweenOwnAccountsWithInvalidAmount(float  amount, ResponseSpecification errorResponseSpec){
        RequestSpecification userSpec = createUserAndGetAuthSpec();
        int senderAccountId = createAccount(userSpec);
        int receiverAccountId = createAccount(userSpec);

        depositMoneyOnAccount(userSpec, senderAccountId, SMALL_SENDER_BALANCE, SMALL_SENDER_BALANCE);

        // подготовка ожидаемого баланса для отправителя и получателя
        float senderExpectedBalance = SMALL_SENDER_BALANCE;
        float receiverExpectedBalance = EMPTY_ACCOUNT_BALANCE;

        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .amount(amount)
                .build();

        new TransferRequester(userSpec, errorResponseSpec)
                .post(transferRequest);

        // проверка неизмененного баланса у отправителя
        assertAccountBalance(softy, userSpec, senderAccountId, senderExpectedBalance);

        // проверка неизмененного баланса у получателя
        assertAccountBalance(softy, userSpec, receiverAccountId, receiverExpectedBalance);
    }

    // негативный тест, если баланс меньше, чем сумма трансфера
    @Test
    public void userCannotTransferWhenBalanceIsNotEnough() {
        RequestSpecification userSpec = createUserAndGetAuthSpec();

        int senderAccountId = createAccount(userSpec);
        int receiverAccountId = createAccount(userSpec);

        depositMoneyOnAccount(userSpec, senderAccountId, SMALL_SENDER_BALANCE, SMALL_SENDER_BALANCE);

        // подготовка ожидаемого баланса для отправителя и получателя
        float senderExpectedBalance = SMALL_SENDER_BALANCE;
        float receiverExpectedBalance = EMPTY_ACCOUNT_BALANCE;

        float amount = generateTransferAmountMoreThanBalance(SMALL_SENDER_BALANCE);

        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .amount(amount)
                .build();

        new TransferRequester(userSpec, ResponseSpecs.invalidTransfer())
                .post(transferRequest);

        // проверка неизмененного баланса у отправителя
        assertAccountBalance(softy, userSpec, senderAccountId, senderExpectedBalance);

        // проверка неизмененного баланса у получателя
        assertAccountBalance(softy, userSpec, receiverAccountId, receiverExpectedBalance);
    }

    // негативный тест: трансфер на несуществующий аккаунт
    @Test
    public void userCannotTransferToNonExistingAccount(){
        RequestSpecification userSpec = createUserAndGetAuthSpec();
        int senderAccountId = createAccount(userSpec);

        depositMoneyOnAccount(userSpec, senderAccountId, SMALL_SENDER_BALANCE, SMALL_SENDER_BALANCE);
        depositMoneyOnAccount(userSpec, senderAccountId, SMALL_SENDER_BALANCE, SMALL_SENDER_BALANCE*2);
        depositMoneyOnAccount(userSpec, senderAccountId, SMALL_SENDER_BALANCE, PREPARED_SENDER_BALANCE);

        // подготовка ожидаемого баланса для отправителя и получателя
        float senderExpectedBalance = PREPARED_SENDER_BALANCE;
        float amount = generateValidTransferAmount();

        // генерируем id несуществующего аккаунта
        int nonExistingAccId = generateNonExistingAccountIdBasedOn(senderAccountId);

        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(senderAccountId)
                .receiverAccountId(nonExistingAccId)
                .amount(amount)
                .build();

        new TransferRequester(userSpec, ResponseSpecs.invalidTransfer())
                .post(transferRequest);

        // проверка неизмененного баланса у отправителя
        assertAccountBalance(softy, userSpec, senderAccountId, senderExpectedBalance);
    }

    // негативный тест: трансфер с несуществующего аккаунта
    @Test
    public void userCannotTransferFromNonExistingAccount(){
        RequestSpecification userSpec = createUserAndGetAuthSpec();
        int receiverAccountId = createAccount(userSpec);

        depositMoneyOnAccount(userSpec, receiverAccountId, SMALL_SENDER_BALANCE, SMALL_SENDER_BALANCE);

        // подготовка ожидаемого баланса для отправителя и получателя
        float receiverExpectedBalance = SMALL_SENDER_BALANCE;
        float amount = generateValidTransferAmount();

        // генерируем id несуществующего аккаунта
        int nonExistingAccId = generateNonExistingAccountIdBasedOn(receiverAccountId);

        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(nonExistingAccId)
                .receiverAccountId(receiverAccountId)
                .amount(amount)
                .build();

        new TransferRequester(userSpec, ResponseSpecs.unauthorizedAccessToAccount())
                .post(transferRequest);

        // проверка неизмененного баланса у получателя
        assertAccountBalance(softy, userSpec, receiverAccountId, receiverExpectedBalance);

    }

    // негативный тест: трансфер с чужого аккаунта
    @Test
    public void userCannotTransferFromAnotherUserAccount() {
        RequestSpecification user1Spec = createUserAndGetAuthSpec();
        int senderAccountId = createAccount(user1Spec);

        depositMoneyOnAccount(user1Spec, senderAccountId, SMALL_SENDER_BALANCE, SMALL_SENDER_BALANCE);
        depositMoneyOnAccount(user1Spec, senderAccountId, SMALL_SENDER_BALANCE, SMALL_SENDER_BALANCE*2);
        depositMoneyOnAccount(user1Spec, senderAccountId, SMALL_SENDER_BALANCE, PREPARED_SENDER_BALANCE);

        RequestSpecification user2Spec = createUserAndGetAuthSpec();
        int receiverAccountId = createAccount(user2Spec);

        // подготовка ожидаемого баланса для отправителя и получателя
        float senderExpectedBalance = PREPARED_SENDER_BALANCE;
        float receiverExpectedBalance = EMPTY_ACCOUNT_BALANCE;
        float amount = generateValidTransferAmount();

        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .amount(amount)
                .build();

        new TransferRequester(user2Spec, ResponseSpecs.unauthorizedAccessToAccount())
                .post(transferRequest);

        // проверка неизмененного баланса у отправителя
        assertAccountBalance(softy, user1Spec, senderAccountId, senderExpectedBalance);

        // проверка неизмененного баланса у получателя
        assertAccountBalance(softy, user2Spec, receiverAccountId, receiverExpectedBalance);
    }

    // негативный тест: трансфер без токена авторизации
    @Test
    public void userCannotTransferWithoutAuthorizationToken(){
        RequestSpecification user1Spec = createUserAndGetAuthSpec();
        int senderAccountId = createAccount(user1Spec);

        depositMoneyOnAccount(user1Spec, senderAccountId, SMALL_SENDER_BALANCE, SMALL_SENDER_BALANCE);
        depositMoneyOnAccount(user1Spec, senderAccountId, SMALL_SENDER_BALANCE, SMALL_SENDER_BALANCE*2);
        depositMoneyOnAccount(user1Spec, senderAccountId, SMALL_SENDER_BALANCE, PREPARED_SENDER_BALANCE);

        RequestSpecification user2Spec = createUserAndGetAuthSpec();
        int receiverAccountId = createAccount(user2Spec);

        // подготовка ожидаемого баланса для отправителя и получателя
        float senderExpectedBalance = PREPARED_SENDER_BALANCE;
        float receiverExpectedBalance = EMPTY_ACCOUNT_BALANCE;
        float amount = generateValidTransferAmount();

        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .amount(amount)
                .build();

        new TransferRequester(RequestsSpecs.unauthSpec(), ResponseSpecs.unauthorized())
                .post(transferRequest);

        // проверка неизмененного баланса у отправителя
        assertAccountBalance(softy, user1Spec, senderAccountId, senderExpectedBalance);

        // проверка неизмененного баланса у получателя
        assertAccountBalance(softy, user2Spec, receiverAccountId, receiverExpectedBalance);
    }

    // негативный тест: трансфер с битым токеном авторизации
    @Test
    public void userCannotTransferWithWrongAuthorizationToken(){
        RequestSpecification user1Spec = createUserAndGetAuthSpec();
        int senderAccountId = createAccount(user1Spec);

        depositMoneyOnAccount(user1Spec, senderAccountId, SMALL_SENDER_BALANCE, SMALL_SENDER_BALANCE);
        depositMoneyOnAccount(user1Spec, senderAccountId, SMALL_SENDER_BALANCE, SMALL_SENDER_BALANCE*2);
        depositMoneyOnAccount(user1Spec, senderAccountId, SMALL_SENDER_BALANCE, PREPARED_SENDER_BALANCE);

        RequestSpecification user2Spec = createUserAndGetAuthSpec();
        int receiverAccountId = createAccount(user2Spec);

        // подготовка ожидаемого баланса для отправителя и получателя
        float senderExpectedBalance = PREPARED_SENDER_BALANCE;
        float receiverExpectedBalance = EMPTY_ACCOUNT_BALANCE;
        float amount = generateValidTransferAmount();

        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .amount(amount)
                .build();

        new TransferRequester(RequestsSpecs.brokenAuthSpec(), ResponseSpecs.unauthorized())
                .post(transferRequest);

        // проверка неизмененного баланса у отправителя
        assertAccountBalance(softy, user1Spec, senderAccountId, senderExpectedBalance);

        // проверка неизмененного баланса у получателя
        assertAccountBalance(softy, user2Spec, receiverAccountId, receiverExpectedBalance);
    }
}
