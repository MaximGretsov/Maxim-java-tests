package iteration2;

import generators.RandomData;
import io.restassured.specification.RequestSpecification;
import iteration1.BaseTest;
import models.AccountResponse;
import models.CreateUserRequest;
import models.DepositRequest;
import models.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import request.AdminCreateUserRequester;
import request.CreateAccountRequester;
import request.CustomerAccountsRequester;
import request.DepositRequester;
import specs.RequestsSpecs;
import specs.ResponseSpecs;

import java.util.List;
import java.util.stream.Stream;


public class DepositTests extends BaseTest {
    private static final String DEPOSIT_PATH = "/api/v1/accounts/deposit";

    // Создаем пользователя и возвращаем request spec уже с токеном этого пользователя
    private RequestSpecification createUserAndGetAuthSpec() {
        CreateUserRequest userRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        // Создаем пользователя админом
        new AdminCreateUserRequester(
                RequestsSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated()
        ).post(userRequest);

        // Логинимся под созданным пользователем и получаем auth spec
        return RequestsSpecs.authAsUserSpec(
                userRequest.getUsername(),
                userRequest.getPassword()
        );
    }

    // Создаем аккаунт и возвращаем его id
    private int createAccount(RequestSpecification userSpec) {
        return new CreateAccountRequester(
                userSpec,
                ResponseSpecs.entityWasCreated()
        )
                .post(null)
                .extract()
                .path("id");
    }

    // Получаем конкретный аккаунт из списка аккаунтов пользователя
    private AccountResponse getAccountById(RequestSpecification userSpec, int accountId) {
        List<AccountResponse> accounts = new CustomerAccountsRequester(
                userSpec,
                ResponseSpecs.requestReturnsOk()
        ).getAccounts();

        return accounts.stream()
                .filter(account -> account.getId() == accountId)
                .findFirst()
                .orElseThrow();
    }

    // позитивные тесты на депозит
    public static Stream<Arguments> correctDepositData(){
        return Stream.of(
                // happy path первый депозит на 100
                Arguments.of(100f, 100f),
                // депозит минимально возможной суммы
                Arguments.of(0.01f, 0.01f),
                //  депозит чуть ниже максимально возможной суммы
                Arguments.of(4999.99f, 4999.99f),
                //  депозит максимально возможной суммы
                Arguments.of(5000f, 5000f)
        );
    }
    @MethodSource("correctDepositData")
    @ParameterizedTest
    public void userCanDepositWithCorrectData(float  balance, float newBalance){
        RequestSpecification userSpec = createUserAndGetAuthSpec();

        // создаем аккаунт
        int accId = createAccount(userSpec);

        DepositRequest depositRequest = DepositRequest.builder()
                .id(accId)
                .balance(balance)
                .build();

        new DepositRequester(userSpec, ResponseSpecs.successfulDepositResponse(accId, newBalance))
                .post(depositRequest);

        // проверяем обновленный депозит
        AccountResponse accountAfterDeposit = getAccountById(userSpec, accId);
        softy.assertThat(accountAfterDeposit.getBalance()).isEqualTo(newBalance);
        softy.assertThat(accountAfterDeposit.getTransactions()).isNotEmpty();
    }

    // негативные тесты c невалидной суммой депозита
    public static Stream<Arguments> invalidAmount(){
        return Stream.of(
                // отправка 0
                Arguments.of(0f, "Deposit amount must be at least 0.01"),
                // отправка отрицательного числа
                Arguments.of(-1f, "Deposit amount must be at least 0.01"),
                //  отправка больше максимума
                Arguments.of(5000.01f, "Deposit amount cannot exceed 5000")
        );
    }

    @MethodSource("invalidAmount")
    @ParameterizedTest
    public void userCannotDepositWithInvalidAmount(float  balance, String errorValue){
        float expectedBalance = 0f;

        RequestSpecification userSpec = createUserAndGetAuthSpec();

        // создаем аккаунт
        int accId = createAccount(userSpec);

        DepositRequest depositRequest = DepositRequest.builder()
                .id(accId)
                .balance(balance)
                .build();

        new DepositRequester(userSpec, ResponseSpecs.requestReturnsBadRequestWithText(errorValue))
                .post(depositRequest);

        // проверяем что депозит не изменился и записей о транзакциях не прибавилось
        AccountResponse accountAfterDeposit = getAccountById(userSpec, accId);
        softy.assertThat(accountAfterDeposit.getBalance()).isEqualTo(expectedBalance);
        softy.assertThat(accountAfterDeposit.getTransactions()).isEmpty();
    }

    // негативный тест c депозитом на несуществующий аккаунт
    @Test
    public void userCannotDepositWithNonExistingAccount(){
        float amount = 100f;
        float expectedBalance = 0f;

        RequestSpecification userSpec = createUserAndGetAuthSpec();

        // создаем аккаунт
        int accId = createAccount(userSpec);

        // берем accId который скорее не будет существовать(и вряд ли у нас столько аккаунтов,
        // что мы выйдем за пределы int)
        int nonExistingAccId = accId + 1000000;

        DepositRequest depositRequest = DepositRequest.builder()
                .id(nonExistingAccId)
                .balance(amount)
                .build();

        new DepositRequester(userSpec,  ResponseSpecs.forbiddenWithText("Unauthorized access to account"))
                .post(depositRequest);

        // проверяем что депозит не изменился и записей о транзакциях не прибавилось
        AccountResponse accountAfterDeposit = getAccountById(userSpec, accId);
        softy.assertThat(accountAfterDeposit.getBalance()).isEqualTo(expectedBalance);
        softy.assertThat(accountAfterDeposit.getTransactions()).isEmpty();
    }

    // негативный тест c депозитом на чужой аккаунт
    @Test
    public void userCannotDepositToAnotherUserAccount(){
        float amount = 100f;
        float expectedBalance = 0f;

        RequestSpecification userSpec1 = createUserAndGetAuthSpec();
        // создаем аккаунт пользователю 1
        int accId1 = createAccount(userSpec1);

        RequestSpecification userSpec2 = createUserAndGetAuthSpec();
        // создаем аккаунт пользователю 2
        int accId2 = createAccount(userSpec2);

        DepositRequest depositRequest = DepositRequest.builder()
                .id(accId2)
                .balance(amount)
                .build();

        new DepositRequester(userSpec1,  ResponseSpecs.forbiddenWithText("Unauthorized access to account"))
                .post(depositRequest);

        // проверяем что депозит не изменился и записей о транзакциях не прибавилось
        AccountResponse accountAfterDeposit = getAccountById(userSpec2, accId2);
        softy.assertThat(accountAfterDeposit.getBalance()).isEqualTo(expectedBalance);
        softy.assertThat(accountAfterDeposit.getTransactions()).isEmpty();
    }

    // негативный тест c string id в body
    @Test
    public void userCannotDepositWithStringIdInBody(){
        float expectedBalance = 0f;

        RequestSpecification userSpec = createUserAndGetAuthSpec();

        // создаем аккаунт
        int accId = createAccount(userSpec);

        String requestBody = """
            {
                "id": "abcd",
                "balance": 100
            }
            """;

        new DepositRequester(userSpec,
                ResponseSpecs.internalServerErrorForPath(DEPOSIT_PATH))
                .postRawBody(requestBody);

        // проверяем что депозит не изменился и записей о транзакциях не прибавилось
        AccountResponse accountAfterDeposit = getAccountById(userSpec, accId);
        softy.assertThat(accountAfterDeposit.getBalance()).isEqualTo(expectedBalance);
        softy.assertThat(accountAfterDeposit.getTransactions()).isEmpty();
    }

    // негативный тест c string balance в body
    @Test
    public void userCannotDepositWithStringBalanceInBody(){
        float expectedBalance = 0f;

        RequestSpecification userSpec = createUserAndGetAuthSpec();

        // создаем аккаунт
        int accId = createAccount(userSpec);

        String requestBody = String.format("""
                {
                            "id": %s,
                            "balance": "%s"
                        }
                """, accId, "abcd");

        new DepositRequester(userSpec,
                ResponseSpecs.internalServerErrorForPath(DEPOSIT_PATH))
                .postRawBody(requestBody);

        // проверяем что депозит не изменился и записей о транзакциях не прибавилось
        AccountResponse accountAfterDeposit = getAccountById(userSpec, accId);
        softy.assertThat(accountAfterDeposit.getBalance()).isEqualTo(expectedBalance);
        softy.assertThat(accountAfterDeposit.getTransactions()).isEmpty();
    }

    // негативный тест без id в body (для этого теста создавать аккаунт внутри не надо)
    @Test
    public void userCannotDepositWithoutIdInBody(){
        float expectedBalance = 0f;

        RequestSpecification userSpec = createUserAndGetAuthSpec();

        // создаем аккаунт
        int accId = createAccount(userSpec);

        String requestBody = String.format("""
                {
                            "balance": %s
                        }
                """, 100);

        new DepositRequester(userSpec,
                ResponseSpecs.internalServerErrorForPath(DEPOSIT_PATH))
                .postRawBody(requestBody);

        // проверяем что депозит не изменился и записей о транзакциях не прибавилось
        AccountResponse accountAfterDeposit = getAccountById(userSpec, accId);
        softy.assertThat(accountAfterDeposit.getBalance()).isEqualTo(expectedBalance);
        softy.assertThat(accountAfterDeposit.getTransactions()).isEmpty();
    }

    // негативный тест без balance в body
    @Test
    public void userCannotDepositWithoutBalanceInBody(){
        float expectedBalance = 0f;

        RequestSpecification userSpec = createUserAndGetAuthSpec();

        // создаем аккаунт
        int accId = createAccount(userSpec);

        String requestBody = String.format("""
                {
                            "id": %s
                        }
                """, accId);

        new DepositRequester(userSpec,
                ResponseSpecs.internalServerErrorForPath(DEPOSIT_PATH))
                .postRawBody(requestBody);

        // проверяем что депозит не изменился и записей о транзакциях не прибавилось
        AccountResponse accountAfterDeposit = getAccountById(userSpec, accId);
        softy.assertThat(accountAfterDeposit.getBalance()).isEqualTo(expectedBalance);
        softy.assertThat(accountAfterDeposit.getTransactions()).isEmpty();
    }

    // негативный тест с невалидным токеном авторизации
    @Test
    public void userCannotDepositWithWrongAuthorizationToken(){
        float amount = 100f;
        float expectedBalance = 0f;

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
        softy.assertThat(accountAfterDeposit.getBalance()).isEqualTo(expectedBalance);
        softy.assertThat(accountAfterDeposit.getTransactions()).isEmpty();
    }

    // негативный тест без токена авторизации(нет хедера с авторизацией)
    @Test
    public void userCannotDepositWithoutAuthorization(){
        float amount = 100f;
        float expectedBalance = 0f;

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
        softy.assertThat(accountAfterDeposit.getBalance()).isEqualTo(expectedBalance);
        softy.assertThat(accountAfterDeposit.getTransactions()).isEmpty();
    }
}

