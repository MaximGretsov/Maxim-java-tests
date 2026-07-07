package iteration2;

import generators.RandomData;
import io.restassured.specification.RequestSpecification;
import iteration1.BaseTest;
import models.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import request.*;
import specs.RequestsSpecs;
import specs.ResponseSpecs;

import java.util.List;
import java.util.stream.Stream;


public class TransferTests extends BaseTest {
    private static final String TRANSFER_PATH = "/api/v1/accounts/transfer";

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

    // депозит денег на аккаунт
    private void depositMoneyOnAccount(RequestSpecification userSpec,
                                       int accountId,
                                       float depositAmount,
                                       float expectedBalanceAfterDeposit) {
        DepositRequest depositRequest = DepositRequest.builder()
                .id(accountId)
                .balance(depositAmount)
                .build();

        new DepositRequester(
                userSpec,
                ResponseSpecs.successfulDepositResponse(accountId, expectedBalanceAfterDeposit)
        ).post(depositRequest);
    }

    //
    private void assertAccountBalance(RequestSpecification userSpec, int accountId, float expectedBalance) {
        AccountResponse account = getAccountById(userSpec, accountId);

        softy.assertThat(account.getBalance())
                .isEqualTo(expectedBalance);
    }

    // позитивные тесты на трансфер на чужой аккаунт
    public static Stream<Arguments> correctTransferData(){
        return Stream.of(
                // happy path трансфер на 100
                Arguments.of(100f, 100f),
                // трансфер минимально возможной суммы
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

        depositMoneyOnAccount(user1Spec, senderAccountId, 5000f, 5000f);
        depositMoneyOnAccount(user1Spec, senderAccountId, 5000f, 10000f);
        depositMoneyOnAccount(user1Spec, senderAccountId, 5000f, 15000f);

        RequestSpecification user2Spec = createUserAndGetAuthSpec();
        int receiverAccountId = createAccount(user2Spec);

        // подготовка ожидаемого баланса для отправителя и получателя
        float senderInitialBalance = 15000f;
        float receiverInitialBalance = 0f;
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
        assertAccountBalance(user1Spec, senderAccountId, senderExpectedBalance);

        // проверка измененного баланса у получателя
        assertAccountBalance(user2Spec, receiverAccountId, receiverExpectedBalance);
    }

    // позитивный тест на трансфер между своими аккаунтами
    @Test
    public void userCanTransferBetweenOwnAccountsWithCorrectData(){
        RequestSpecification userSpec = createUserAndGetAuthSpec();
        int senderAccountId = createAccount(userSpec);
        int receiverAccountId = createAccount(userSpec);

        depositMoneyOnAccount(userSpec, senderAccountId, 5000f, 5000f);

        // подготовка ожидаемого баланса для отправителя и получателя
        float amount = 100f;
        float senderInitialBalance = 5000f;
        float receiverInitialBalance = 0f;
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
        assertAccountBalance(userSpec, senderAccountId, senderExpectedBalance);

        // проверка измененного баланса у получателя
        assertAccountBalance(userSpec, receiverAccountId, receiverExpectedBalance);
    }

    // негативный тест c невалидной суммой трансфера
    public static Stream<Arguments> invalidTransferAmountData(){
        return Stream.of(
                // трансфер 0
                Arguments.of(0f, "Transfer amount must be at least 0.01"),
                // трансфер отрицательного значения
                Arguments.of(-100f, "Transfer amount must be at least 0.01"),
                //  трансфер чуть выше максимально возможной суммы
                Arguments.of(10000.01f, "Transfer amount cannot exceed 10000"),
                //  трансфер суммы большей, чем есть на аккаунте
                Arguments.of(7500f, "Invalid transfer: insufficient funds or invalid accounts")
        );
    }
    @MethodSource("invalidTransferAmountData")
    @ParameterizedTest
    public void userCannotTransferBetweenOwnAccountsWithInvalidAmount(float  amount, String errorValue){
        RequestSpecification userSpec = createUserAndGetAuthSpec();
        int senderAccountId = createAccount(userSpec);
        int receiverAccountId = createAccount(userSpec);

        depositMoneyOnAccount(userSpec, senderAccountId, 5000f, 5000f);

        // подготовка ожидаемого баланса для отправителя и получателя
        float senderExpectedBalance = 5000f;
        float receiverExpectedBalance = 0f;

        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .amount(amount)
                .build();

        new TransferRequester(
                userSpec,
                ResponseSpecs.requestReturnsBadRequestWithText(errorValue))
                .post(transferRequest);

        // проверка неизмененного баланса у отправителя
        assertAccountBalance(userSpec, senderAccountId, senderExpectedBalance);

        // проверка неизмененного баланса у получателя
        assertAccountBalance(userSpec, receiverAccountId, receiverExpectedBalance);
    }

    // негативный тест: трансфер на несуществующий аккаунт
    @Test
    public void userCannotTransferToNonExistingAccount(){
        RequestSpecification userSpec = createUserAndGetAuthSpec();
        int senderAccountId = createAccount(userSpec);

        depositMoneyOnAccount(userSpec, senderAccountId, 5000f, 5000f);

        // подготовка ожидаемого баланса для отправителя и получателя
        float senderExpectedBalance = 5000f;
        float amount = 100f;

        // берем accId который скорее не будет существовать(и вряд ли у нас столько аккаунтов,
        // что мы выйдем за пределы int)
        int nonExistingAccId = senderAccountId + 1000000;

        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(senderAccountId)
                .receiverAccountId(nonExistingAccId)
                .amount(amount)
                .build();

        new TransferRequester(
                userSpec,
                ResponseSpecs.requestReturnsBadRequestWithText
                        ("Invalid transfer: insufficient funds or invalid accounts"))
                .post(transferRequest);

        // проверка неизмененного баланса у отправителя
        assertAccountBalance(userSpec, senderAccountId, senderExpectedBalance);
    }

    // негативный тест: трансфер с несуществующего аккаунта
    @Test
    public void userCannotTransferFromNonExistingAccount(){
        RequestSpecification userSpec = createUserAndGetAuthSpec();
        int receiverAccountId = createAccount(userSpec);

        depositMoneyOnAccount(userSpec, receiverAccountId, 5000f, 5000f);

        // подготовка ожидаемого баланса для отправителя и получателя
        float receiverExpectedBalance = 5000f;
        float amount = 100f;


        // берем accId который скорее не будет существовать(и вряд ли у нас столько аккаунтов,
        // что мы выйдем за пределы int)
        int nonExistingAccId = receiverAccountId + 1000000;

        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(nonExistingAccId)
                .receiverAccountId(receiverAccountId)
                .amount(amount)
                .build();

        new TransferRequester(
                userSpec,
                ResponseSpecs.forbiddenWithText
                        ("Unauthorized access to account"))
                .post(transferRequest);

        // проверка неизмененного баланса у получателя
        assertAccountBalance(userSpec, receiverAccountId, receiverExpectedBalance);

    }

    // негативный тест: трансфер с чужого аккаунта
    @Test
    public void userCannotTransferFromAnotherUserAccount() {
        RequestSpecification user1Spec = createUserAndGetAuthSpec();
        int senderAccountId = createAccount(user1Spec);

        depositMoneyOnAccount(user1Spec, senderAccountId, 5000f, 5000f);

        RequestSpecification user2Spec = createUserAndGetAuthSpec();
        int receiverAccountId = createAccount(user2Spec);

        // подготовка ожидаемого баланса для отправителя и получателя
        float senderExpectedBalance = 5000f;
        float receiverExpectedBalance = 0f;
        float amount = 100f;

        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .amount(amount)
                .build();

        new TransferRequester(
                user2Spec,
                ResponseSpecs.forbiddenWithText
                        ("Unauthorized access to account"))
                .post(transferRequest);

        // проверка неизмененного баланса у отправителя
        assertAccountBalance(user1Spec, senderAccountId, senderExpectedBalance);

        // проверка неизмененного баланса у получателя
        assertAccountBalance(user2Spec, receiverAccountId, receiverExpectedBalance);
    }

    // негативный тест: без senderAccountId в body
    @Test
    public void userCannotTransferWithoutSenderAccountIdInBody(){
        RequestSpecification userSpec = createUserAndGetAuthSpec();
        int receiverAccountId = createAccount(userSpec);

        // подготовка ожидаемого баланса для получателя
        float receiverExpectedBalance = 0f;

        String requestBody = String.format("""
                {
                            "receiverAccountId": %s,
                            "amount": %s
                        }
                """, receiverAccountId, 100f);

        new TransferRequester(
                userSpec,
                ResponseSpecs.internalServerErrorForPath(TRANSFER_PATH)
        ).postRawBody(requestBody);

        // проверка неизмененного баланса у получателя
        assertAccountBalance(userSpec, receiverAccountId, receiverExpectedBalance);
    }

    // негативный тест: без receiverAccountId в body
    @Test
    public void userCannotTransferWithoutReceiverAccountIdInBody(){
        RequestSpecification userSpec = createUserAndGetAuthSpec();
        int senderAccountId = createAccount(userSpec);

        depositMoneyOnAccount(userSpec, senderAccountId, 5000f, 5000f);

        // подготовка ожидаемого баланса для отправителя
        float senderExpectedBalance = 5000f;

        String requestBody = String.format("""
                {
                            "senderAccountId": %s,
                            "amount": %s
                        }
                """, senderAccountId, 100f);

        new TransferRequester(
                userSpec,
                ResponseSpecs.internalServerErrorForPath(TRANSFER_PATH)
        ).postRawBody(requestBody);

        // проверка неизмененного баланса у отправителя
        assertAccountBalance(userSpec, senderAccountId, senderExpectedBalance);
    }

    // негативный тест: без amount в body
    @Test
    public void userCannotTransferWithoutAmountInBody(){
        RequestSpecification userSpec = createUserAndGetAuthSpec();
        int senderAccountId = createAccount(userSpec);
        int receiverAccountId = createAccount(userSpec);

        depositMoneyOnAccount(userSpec, senderAccountId, 5000f, 5000f);

        // подготовка ожидаемого баланса для получателя
        float senderExpectedBalance = 5000f;
        float receiverExpectedBalance = 0f;

        String requestBody = String.format("""
                {
                            "senderAccountId": %s,
                            "receiverAccountId": %s
                        }
                """, senderAccountId, receiverAccountId);

        new TransferRequester(
                userSpec,
                ResponseSpecs.internalServerErrorForPath(TRANSFER_PATH)
        ).postRawBody(requestBody);

        // проверка неизмененного баланса у отправителя
        assertAccountBalance(userSpec, senderAccountId, senderExpectedBalance);

        // проверка неизмененного баланса у получателя
        assertAccountBalance(userSpec, receiverAccountId, receiverExpectedBalance);
    }

    // негативный тест: c string senderAccountId в body
    @Test
    public void userCannotTransferWithStringSenderAccountIdInBody(){
        // Sender account не создаем, потому что проверяем senderAccountId = string в body
        RequestSpecification userSpec = createUserAndGetAuthSpec();
        int receiverAccountId = createAccount(userSpec);

        // подготовка ожидаемого баланса для получателя
        float receiverExpectedBalance = 0f;

        String requestBody = String.format("""
                {
                            "senderAccountId": "%s",
                            "receiverAccountId": %s,
                            "amount": %s
                        }
                """, "abcd", receiverAccountId, 100f);

        new TransferRequester(
                userSpec,
                ResponseSpecs.internalServerErrorForPath(TRANSFER_PATH)
        ).postRawBody(requestBody);

        // проверка неизмененного баланса у получателя
        assertAccountBalance(userSpec, receiverAccountId, receiverExpectedBalance);
    }

    // негативный тест: c string receiverAccountId в body
    @Test
    public void userCannotTransferWithStringReceiverAccountIdInBody(){
        RequestSpecification userSpec = createUserAndGetAuthSpec();
        int senderAccountId = createAccount(userSpec);

        depositMoneyOnAccount(userSpec, senderAccountId, 5000f, 5000f);

        // Receiver account не создаем, потому что проверяем receiverAccountId = string в body

        // подготовка ожидаемого баланса для отправителя и получателя
        float senderExpectedBalance = 5000f;

        String requestBody = String.format("""
                {
                            "senderAccountId": %s,
                            "receiverAccountId": "%s",
                            "amount": %s
                        }
                """, senderAccountId, "abcd", 100f);

        new TransferRequester(
                userSpec,
                ResponseSpecs.internalServerErrorForPath(TRANSFER_PATH)
        ).postRawBody(requestBody);

        // проверка неизмененного баланса у отправителя
        assertAccountBalance(userSpec, senderAccountId, senderExpectedBalance);
    }

    // негативный тест: c string amount в body
    @Test
    public void userCannotTransferWithStringAmountInBody(){
        RequestSpecification userSpec = createUserAndGetAuthSpec();
        int senderAccountId = createAccount(userSpec);
        int receiverAccountId = createAccount(userSpec);

        depositMoneyOnAccount(userSpec, senderAccountId, 5000f, 5000f);

        // подготовка ожидаемого баланса для отправителя и получателя
        float senderExpectedBalance = 5000f;
        float receiverExpectedBalance = 0f;

        String requestBody = String.format("""
                {
                            "senderAccountId": %s,
                            "receiverAccountId": %s,
                            "amount": "%s"
                        }
                """, senderAccountId, receiverAccountId, "abcd");

        new TransferRequester(
                userSpec,
                ResponseSpecs.internalServerErrorForPath(TRANSFER_PATH)
        ).postRawBody(requestBody);

        // проверка неизмененного баланса у отправителя
        assertAccountBalance(userSpec, senderAccountId, senderExpectedBalance);

        // проверка неизмененного баланса у получателя
        assertAccountBalance(userSpec, receiverAccountId, receiverExpectedBalance);
    }


    // негативный тест: трансфер без токена авторизации
    @Test
    public void userCannotTransferWithoutAuthorizationToken(){
        RequestSpecification user1Spec = createUserAndGetAuthSpec();
        int senderAccountId = createAccount(user1Spec);

        depositMoneyOnAccount(user1Spec, senderAccountId, 5000f, 5000f);

        RequestSpecification user2Spec = createUserAndGetAuthSpec();
        int receiverAccountId = createAccount(user2Spec);

        // подготовка ожидаемого баланса для отправителя и получателя
        float senderExpectedBalance = 5000f;
        float receiverExpectedBalance = 0f;
        float amount = 100f;

        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .amount(amount)
                .build();

        new TransferRequester(RequestsSpecs.unauthSpec(), ResponseSpecs.unauthorized())
                .post(transferRequest);

        // проверка неизмененного баланса у отправителя
        assertAccountBalance(user1Spec, senderAccountId, senderExpectedBalance);

        // проверка неизмененного баланса у получателя
        assertAccountBalance(user2Spec, receiverAccountId, receiverExpectedBalance);
    }

    // негативный тест: трансфер с битым токеном авторизации
    @Test
    public void userCannotTransferWithWrongAuthorizationToken(){
        RequestSpecification user1Spec = createUserAndGetAuthSpec();
        int senderAccountId = createAccount(user1Spec);

        depositMoneyOnAccount(user1Spec, senderAccountId, 5000f, 5000f);

        RequestSpecification user2Spec = createUserAndGetAuthSpec();
        int receiverAccountId = createAccount(user2Spec);

        // подготовка ожидаемого баланса для отправителя и получателя
        float senderExpectedBalance = 5000f;
        float receiverExpectedBalance = 0f;
        float amount = 100f;

        TransferRequest transferRequest = TransferRequest.builder()
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .amount(amount)
                .build();

        new TransferRequester(RequestsSpecs.brokenAuthSpec(), ResponseSpecs.unauthorized())
                .post(transferRequest);

        // проверка неизмененного баланса у отправителя
        assertAccountBalance(user1Spec, senderAccountId, senderExpectedBalance);

        // проверка неизмененного баланса у получателя
        assertAccountBalance(user2Spec, receiverAccountId, receiverExpectedBalance);
    }
}
