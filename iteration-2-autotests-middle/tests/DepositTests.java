package iteration2;

import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import iteration1.BaseTest;
import models.AccountResponse;
import models.DepositRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import request.DepositRequester;
import specs.RequestsSpecs;
import specs.ResponseSpecs;

import java.util.stream.Stream;

import static TestData.AccountTestData.EMPTY_ACCOUNT_BALANCE;
import static generators.RandomData.*;
import static request.steps.AccountSteps.*;
import static request.steps.UserSteps.createUserAndGetAuthSpec;


public class DepositTests extends BaseTest {
    @Test
    public void userCanDepositWithRandomCorrectAmount() {
        float amount = generateValidDepositAmount();
        RequestSpecification userSpec = createUserAndGetAuthSpec();

        // создаем аккаунт
        int accId = createAccount(userSpec);

        DepositRequest depositRequest = DepositRequest.builder()
                .id(accId)
                .balance(amount)
                .build();

        new DepositRequester(userSpec, ResponseSpecs.successfulDepositResponse(accId, amount))
                .post(depositRequest);

        // проверяем обновленный депозит
        AccountResponse accountAfterDeposit = getAccountById(userSpec, accId);
        softy.assertThat(accountAfterDeposit.getBalance()).isEqualTo(amount);
        softy.assertThat(accountAfterDeposit.getTransactions()).isNotEmpty();
    }

    // граничные значения
    public static Stream<Arguments> correctBoundaryDepositData(){
        return Stream.of(
                // депозит минимально возможной суммы
                Arguments.of(0.01f, 0.01f),
                //  депозит чуть ниже максимально возможной суммы
                Arguments.of(4999.99f, 4999.99f),
                //  депозит максимально возможной суммы
                Arguments.of(5000f, 5000f)
        );
    }
    @MethodSource("correctBoundaryDepositData")
    @ParameterizedTest
    public void userCanDepositWithBoundaryCorrectAmount(float amount, float newBalance){
        RequestSpecification userSpec = createUserAndGetAuthSpec();

        // создаем аккаунт
        int accId = createAccount(userSpec);

        DepositRequest depositRequest = DepositRequest.builder()
                .id(accId)
                .balance(amount)
                .build();

        new DepositRequester(userSpec, ResponseSpecs.successfulDepositResponse(accId, newBalance))
                .post(depositRequest);

        // проверяем обновленный депозит
        AccountResponse accountAfterDeposit = getAccountById(userSpec, accId);
        softy.assertThat(accountAfterDeposit.getBalance()).isEqualTo(newBalance);
        softy.assertThat(accountAfterDeposit.getTransactions()).isNotEmpty();
    }

    // негативные тесты c невалидной граничной суммой депозита
    public static Stream<Arguments> incorrectBoundaryDepositData() {
        return Stream.of(
                Arguments.of(0f, ResponseSpecs.depositAmountLessThanMin()),
                Arguments.of(5000.01f, ResponseSpecs.depositAmountMoreThanMax())
        );
    }

    @MethodSource("incorrectBoundaryDepositData")
    @ParameterizedTest
    public void userCannotDepositWithBoundaryIncorrectAmount(float amount, ResponseSpecification errorResponseSpec){
        RequestSpecification userSpec = createUserAndGetAuthSpec();

        // создаем аккаунт
        int accId = createAccount(userSpec);

        DepositRequest depositRequest = DepositRequest.builder()
                .id(accId)
                .balance(amount)
                .build();

        new DepositRequester(userSpec, errorResponseSpec).post(depositRequest);

        // проверяем что депозит не изменился и записей о транзакциях не прибавилось
        AccountResponse accountAfterDeposit = getAccountById(userSpec, accId);
        softy.assertThat(accountAfterDeposit.getBalance()).isEqualTo(EMPTY_ACCOUNT_BALANCE);
        softy.assertThat(accountAfterDeposit.getTransactions()).isEmpty();
    }


    public static Stream<Arguments> incorrectRandomDepositData() {
        return Stream.of(
                Arguments.of(
                        generateNegativeDepositAmount(),
                        ResponseSpecs.depositAmountLessThanMin()
                ),
                Arguments.of(
                        generateDepositAmountMoreThanMax(),
                        ResponseSpecs.depositAmountMoreThanMax()
                )
        );
    }

    @MethodSource("incorrectRandomDepositData")
    @ParameterizedTest
    public void userCannotDepositWithRandomIncorrectAmount(float amount,
                                                           ResponseSpecification responseSpecification) {
        RequestSpecification userSpec = createUserAndGetAuthSpec();

        // создаем аккаунт
        int accId = createAccount(userSpec);

        DepositRequest depositRequest = DepositRequest.builder()
                .id(accId)
                .balance(amount)
                .build();

        new DepositRequester(userSpec, responseSpecification).post(depositRequest);

        // проверяем что депозит не изменился и записей о транзакциях не прибавилось
        AccountResponse accountAfterDeposit = getAccountById(userSpec, accId);
        softy.assertThat(accountAfterDeposit.getBalance()).isEqualTo(EMPTY_ACCOUNT_BALANCE);
        softy.assertThat(accountAfterDeposit.getTransactions()).isEmpty();
    }
    // негативный тест c депозитом на несуществующий аккаунт
    @Test
    public void userCannotDepositWithNonExistingAccount(){
        float amount = generateValidDepositAmount();

        RequestSpecification userSpec = createUserAndGetAuthSpec();

        // создаем аккаунт
        int accId = createAccount(userSpec);

        // генерируем id несуществующего аккаунта
        int nonExistingAccId = generateNonExistingAccountIdBasedOn(accId);

        DepositRequest depositRequest = DepositRequest.builder()
                .id(nonExistingAccId)
                .balance(amount)
                .build();

        new DepositRequester(userSpec, ResponseSpecs.unauthorizedAccessToAccount()).post(depositRequest);

        // проверяем что депозит не изменился и записей о транзакциях не прибавилось
        AccountResponse accountAfterDeposit = getAccountById(userSpec, accId);
        softy.assertThat(accountAfterDeposit.getBalance()).isEqualTo(EMPTY_ACCOUNT_BALANCE);
        softy.assertThat(accountAfterDeposit.getTransactions()).isEmpty();
    }

    // негативный тест c депозитом на чужой аккаунт
    @Test
    public void userCannotDepositToAnotherUserAccount(){
        float amount = generateValidDepositAmount();

        RequestSpecification userSpec1 = createUserAndGetAuthSpec();

        RequestSpecification userSpec2 = createUserAndGetAuthSpec();
        // создаем аккаунт пользователю 2
        int accId2 = createAccount(userSpec2);

        DepositRequest depositRequest = DepositRequest.builder()
                .id(accId2)
                .balance(amount)
                .build();

        new DepositRequester(userSpec1, ResponseSpecs.unauthorizedAccessToAccount()).post(depositRequest);

        // проверяем что депозит не изменился и записей о транзакциях не прибавилось
        AccountResponse accountAfterDeposit = getAccountById(userSpec2, accId2);
        softy.assertThat(accountAfterDeposit.getBalance()).isEqualTo(EMPTY_ACCOUNT_BALANCE);
        softy.assertThat(accountAfterDeposit.getTransactions()).isEmpty();
    }

    // негативный тест с невалидным токеном авторизации
    @Test
    public void userCannotDepositWithWrongAuthorizationToken(){
        float amount = generateValidDepositAmount();

        RequestSpecification userSpec = createUserAndGetAuthSpec();

        // создаем аккаунт
        int accId = createAccount(userSpec);

        DepositRequest depositRequest = DepositRequest.builder()
                .id(accId)
                .balance(amount)
                .build();

        new DepositRequester(RequestsSpecs.brokenAuthSpec(), ResponseSpecs.unauthorized())
                .post(depositRequest);

        // проверяем что депозит не изменился и записей о транзакциях не прибавилось
        AccountResponse accountAfterDeposit = getAccountById(userSpec, accId);
        softy.assertThat(accountAfterDeposit.getBalance()).isEqualTo(EMPTY_ACCOUNT_BALANCE);
        softy.assertThat(accountAfterDeposit.getTransactions()).isEmpty();
    }

    // негативный тест без токена авторизации(нет хедера с авторизацией)
    @Test
    public void userCannotDepositWithoutAuthorization(){
        float amount = generateValidDepositAmount();

        RequestSpecification userSpec = createUserAndGetAuthSpec();

        // создаем аккаунт
        int accId = createAccount(userSpec);

        DepositRequest depositRequest = DepositRequest.builder()
                .id(accId)
                .balance(amount)
                .build();

        new DepositRequester(RequestsSpecs.unauthSpec(),
                ResponseSpecs.unauthorized())
                .post(depositRequest);

        // проверяем что депозит не изменился и записей о транзакциях не прибавилось
        AccountResponse accountAfterDeposit = getAccountById(userSpec, accId);
        softy.assertThat(accountAfterDeposit.getBalance()).isEqualTo(EMPTY_ACCOUNT_BALANCE);
        softy.assertThat(accountAfterDeposit.getTransactions()).isEmpty();
    }
}

