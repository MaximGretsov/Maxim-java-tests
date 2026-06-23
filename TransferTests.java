package iteration2;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

public class TransferTests {
    @BeforeAll
    public static void setupRestAssured(){
        RestAssured.filters(
                List.of(new RequestLoggingFilter(),
                        new ResponseLoggingFilter()));
    }

    // Генерируем уникальный username чтобы не ловить ошибки + добавили в метод сообщение, чтоб избежать одинаковых
    // юзернеймов при создании нескольких пользователей в одном тесте
    public static String getUsername(String prefix){
        return prefix + "Acc" + String.format("%08d", System.currentTimeMillis() %100_000_000L);
    }

    // сначала получаем токен админа
    public static String getAdminToken(){
        String authHeader = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                    {
                        "username": "admin",
                        "password": "admin"
                    }
                    """)
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .header("Authorization");
        return authHeader;
    }

    // создаем пользователя
    public static void createUser(String username){
        String authAdminToken = getAdminToken();
        String requestBody = String.format("""
                    {
                        "username": "%s",
                        "password": "Password!1",
                        "role": "USER"
                    }
                """, username);
        // создание пользователя сначала
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", authAdminToken)
                .body(requestBody)
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED);
    }

    // получаем токен юзера
    public static String getUserToken(String username){
        String requestBody = String.format("""
                    {
                        "username": "%s",
                        "password": "Password!1"
                    }
                """, username);
        String authUserHeader = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody)
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .header("Authorization");

        return authUserHeader;
    }

    // Создаем аккаунт
    public static int createUserAccount(String userToken){

        int id = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userToken)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        return id;
    }

    // Депозит на аккаунт, чтобы была возможность делать перевод денег
    public static void depositMoneyOnAccount(int accId, String userToken){
        String requestBody = String.format("""
                {
                            "id": %s,
                            "balance": %s
                        }
                """, accId, 5000);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userToken)
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                // проверяем только что статус ок, так как в данных тестах мы проверяем трансфер
                // и если мы упадем на депозите, то пойдем проверять его отдебно
                .statusCode(HttpStatus.SC_OK);
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
        // Создаем имя пользователя 1
        String username1 = getUsername("u1");

        // Создаем пользователя 1
        createUser(username1);

        // Получаем токен пользователя 1
        String user1Token = getUserToken(username1);

        // Создаем аккаунт пользователя
        int senderAccountId = createUserAccount(user1Token);

        // Делаем депозит на 15 тысяч(3 раза по 5 тысяч, чтобы точно хватало денег)
        depositMoneyOnAccount(senderAccountId, user1Token);
        depositMoneyOnAccount(senderAccountId, user1Token);
        depositMoneyOnAccount(senderAccountId, user1Token);

        // Создаем имя пользователя 2
        String username2 = getUsername("u2");

        // Создаем пользователя 2
        createUser(username2);

        // Получаем токен пользователя 2
        String user2Token = getUserToken(username2);

        // Создаем аккаунт пользователя 2
        int receiverAccountId = createUserAccount(user2Token);

        String requestBody = String.format("""
                {
                            "senderAccountId": %s,
                            "receiverAccountId": %s,
                            "amount": %s
                        }
                """, senderAccountId, receiverAccountId, amount);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", user1Token)
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("amount", Matchers.equalTo(expectedAmount))
                .body("receiverAccountId", Matchers.equalTo(receiverAccountId))
                .body("senderAccountId", Matchers.equalTo(senderAccountId))
                .body("message", Matchers.equalTo("Transfer successful"));
    }

    // позитивный тест на трансфер между своими аккаунтами
    @Test
    public void userCanTransferBetweenOwnAccountsWithCorrectData(){
        // Создаем имя пользователя 1
        String username1 = getUsername("u1");

        // Создаем пользователя 1
        createUser(username1);

        // Получаем токен пользователя 1
        String user1Token = getUserToken(username1);

        // Создаем первый аккаунт пользователя, с которого будем списывать деньги
        int senderAccountId = createUserAccount(user1Token);

        // Делаем депозит на 5 тысяч
        depositMoneyOnAccount(senderAccountId, user1Token);

        // Создаем второй аккаунт пользователя 1
        int receiverAccountId = createUserAccount(user1Token);

        String requestBody = String.format("""
                {
                            "senderAccountId": %s,
                            "receiverAccountId": %s,
                            "amount": %s
                        }
                """, senderAccountId, receiverAccountId, 100f);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", user1Token)
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("amount", Matchers.equalTo(100f))
                .body("receiverAccountId", Matchers.equalTo(receiverAccountId))
                .body("senderAccountId", Matchers.equalTo(senderAccountId))
                .body("message", Matchers.equalTo("Transfer successful"));
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
        // Создаем имя пользователя 1
        String username1 = getUsername("u1");

        // Создаем пользователя 1
        createUser(username1);

        // Получаем токен пользователя 1
        String user1Token = getUserToken(username1);

        // Создаем первый аккаунт пользователя, с которого будем списывать деньги
        int senderAccountId = createUserAccount(user1Token);

        // Пополняем senderAccount на 5000, чтобы в кейсе с 7500 проверить недостаток средств
        depositMoneyOnAccount(senderAccountId, user1Token);

        // Создаем второй аккаунт пользователя 1
        int receiverAccountId = createUserAccount(user1Token);

        String requestBody = String.format("""
                {
                            "senderAccountId": %s,
                            "receiverAccountId": %s,
                            "amount": %s
                        }
                """, senderAccountId, receiverAccountId, amount);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", user1Token)
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(Matchers.equalTo(errorValue));
    }

    // негативный тест: трансфер на несуществующий аккаунт
    @Test
    public void userCannotTransferToNonExistingAccount(){
        // Создаем имя пользователя 1
        String username1 = getUsername("u1");

        // Создаем пользователя 1
        createUser(username1);

        // Получаем токен пользователя 1
        String user1Token = getUserToken(username1);

        // Создаем аккаунт пользователя
        int senderAccountId = createUserAccount(user1Token);

        // Делаем депозит на 5 тысяч
        depositMoneyOnAccount(senderAccountId, user1Token);

        // берем accId который скорее не будет существовать(и вряд ли у нас столько аккаунтов,
        // что мы выйдем за пределы int)
        int nonExistingAccId = senderAccountId + 1000000;

        String requestBody = String.format("""
                {
                            "senderAccountId": %s,
                            "receiverAccountId": %s,
                            "amount": %s
                        }
                """, senderAccountId, nonExistingAccId, 100f);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", user1Token)
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(Matchers.equalTo("Invalid transfer: insufficient funds or invalid accounts"));
    }

    // негативный тест: трансфер с несуществующего аккаунта
    @Test
    public void userCannotTransferFromNonExistingAccount(){
        // Создаем имя пользователя 1
        String username1 = getUsername("u1");

        // Создаем пользователя 1
        createUser(username1);

        // Получаем токен пользователя 1
        String user1Token = getUserToken(username1);

        // Создаем аккаунт пользователя
        int receiverAccountId = createUserAccount(user1Token);

        // Делаем депозит на 5 тысяч
        depositMoneyOnAccount(receiverAccountId, user1Token);

        // берем accId который скорее не будет существовать(и вряд ли у нас столько аккаунтов,
        // что мы выйдем за пределы int)
        int nonExistingAccId = receiverAccountId + 1000000;

        String requestBody = String.format("""
                {
                            "senderAccountId": %s,
                            "receiverAccountId": %s,
                            "amount": %s
                        }
                """, nonExistingAccId, receiverAccountId, 100f);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", user1Token)
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_FORBIDDEN)
                .body(Matchers.equalTo("Unauthorized access to account"));
    }

    // негативный тест: трансфер с чужого аккаунта
    @Test
    public void userCannotTransferFromAnotherUserAccount(){
        // Создаем имя пользователя 1
        String username1 = getUsername("u1");

        // Создаем пользователя 1
        createUser(username1);

        // Получаем токен пользователя 1
        String user1Token = getUserToken(username1);

        // Создаем аккаунт пользователя
        int senderAccountId = createUserAccount(user1Token);

        // Создаем имя пользователя 2
        String username2 = getUsername("u2");

        // Создаем пользователя 2
        createUser(username2);

        // Получаем токен пользователя 2
        String user2Token = getUserToken(username2);

        // Создаем аккаунт пользователя 2
        int receiverAccountId = createUserAccount(user2Token);

        String requestBody = String.format("""
                {
                            "senderAccountId": %s,
                            "receiverAccountId": %s,
                            "amount": %s
                        }
                """, senderAccountId, receiverAccountId, 100f);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", user2Token)
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_FORBIDDEN)
                .body(Matchers.equalTo("Unauthorized access to account"));
    }

    // негативный тест: без senderAccountId в body
    @Test
    public void userCannotTransferWithoutSenderAccountIdInBody(){
        // Создаем имя пользователя 1
        String username1 = getUsername("u1");

        // Создаем пользователя 1
        createUser(username1);

        // Получаем токен пользователя 1
        String user1Token = getUserToken(username1);

        // Sender account не создаем, потому что проверяем отсутствие senderAccountId в body

        // Создаем второй аккаунт пользователя 1
        int receiverAccountId = createUserAccount(user1Token);

        String requestBody = String.format("""
                {
                            "receiverAccountId": %s,
                            "amount": %s
                        }
                """, receiverAccountId, 100f);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", user1Token)
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                .body("status", Matchers.equalTo(500))
                .body("error", Matchers.equalTo("Internal Server Error"))
                .body("path", Matchers.equalTo("/api/v1/accounts/transfer"));
    }

    // негативный тест: без receiverAccountId в body
    @Test
    public void userCannotTransferWithoutReceiverAccountIdInBody(){
        // Создаем имя пользователя 1
        String username1 = getUsername("u1");

        // Создаем пользователя 1
        createUser(username1);

        // Получаем токен пользователя 1
        String user1Token = getUserToken(username1);

        // Создаем первый аккаунт пользователя, с которого будем списывать деньги
        int senderAccountId = createUserAccount(user1Token);

        // Делаем депозит на 5 тысяч
        depositMoneyOnAccount(senderAccountId, user1Token);

        // Receiver account не создаем, потому что проверяем отсутствие receiverAccountId в body

        String requestBody = String.format("""
                {
                            "senderAccountId": %s,
                            "amount": %s
                        }
                """, senderAccountId, 100f);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", user1Token)
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                .body("status", Matchers.equalTo(500))
                .body("error", Matchers.equalTo("Internal Server Error"))
                .body("path", Matchers.equalTo("/api/v1/accounts/transfer"));
    }

    // негативный тест: без amount в body
    @Test
    public void userCannotTransferWithoutAmountInBody(){
        // Создаем имя пользователя 1
        String username1 = getUsername("u1");

        // Создаем пользователя 1
        createUser(username1);

        // Получаем токен пользователя 1
        String user1Token = getUserToken(username1);

        // Создаем первый аккаунт пользователя, с которого будем списывать деньги
        int senderAccountId = createUserAccount(user1Token);

        // Делаем депозит на 5 тысяч
        depositMoneyOnAccount(senderAccountId, user1Token);

        // Создаем второй аккаунт пользователя 1
        int receiverAccountId = createUserAccount(user1Token);

        String requestBody = String.format("""
                {
                            "senderAccountId": %s,
                            "receiverAccountId": %s
                        }
                """, senderAccountId, receiverAccountId);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", user1Token)
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                .body("status", Matchers.equalTo(500))
                .body("error", Matchers.equalTo("Internal Server Error"))
                .body("path", Matchers.equalTo("/api/v1/accounts/transfer"));
    }

    // негативный тест: c string senderAccountId в body
    @Test
    public void userCannotTransferWithStringSenderAccountIdInBody(){
        // Создаем имя пользователя 1
        String username1 = getUsername("u1");

        // Создаем пользователя 1
        createUser(username1);

        // Получаем токен пользователя 1
        String user1Token = getUserToken(username1);

        // Sender account не создаем, потому что проверяем senderAccountId = string в body

        // Создаем второй аккаунт пользователя 1
        int receiverAccountId = createUserAccount(user1Token);

        String requestBody = String.format("""
                {
                            "senderAccountId": "%s",
                            "receiverAccountId": %s,
                            "amount": %s
                        }
                """, "abcd", receiverAccountId, 100f);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", user1Token)
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                .body("status", Matchers.equalTo(500))
                .body("error", Matchers.equalTo("Internal Server Error"))
                .body("path", Matchers.equalTo("/api/v1/accounts/transfer"));
    }

    // негативный тест: c string receiverAccountId в body
    @Test
    public void userCannotTransferWithStringReceiverAccountIdInBody(){
        // Создаем имя пользователя 1
        String username1 = getUsername("u1");

        // Создаем пользователя 1
        createUser(username1);

        // Получаем токен пользователя 1
        String user1Token = getUserToken(username1);

        // Создаем первый аккаунт пользователя, с которого будем списывать деньги
        int senderAccountId = createUserAccount(user1Token);

        // Делаем депозит на 5 тысяч
        depositMoneyOnAccount(senderAccountId, user1Token);

        // Receiver account не создаем, потому что проверяем receiverAccountId = string в body

        String requestBody = String.format("""
                {
                            "senderAccountId": %s,
                            "receiverAccountId": "%s",
                            "amount": %s
                        }
                """, senderAccountId, "abcd", 100f);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", user1Token)
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                .body("status", Matchers.equalTo(500))
                .body("error", Matchers.equalTo("Internal Server Error"))
                .body("path", Matchers.equalTo("/api/v1/accounts/transfer"));
    }

    // негативный тест: c string amount в body
    @Test
    public void userCannotTransferWithStringAmountInBody(){
        // Создаем имя пользователя 1
        String username1 = getUsername("u1");

        // Создаем пользователя 1
        createUser(username1);

        // Получаем токен пользователя 1
        String user1Token = getUserToken(username1);

        // Создаем первый аккаунт пользователя, с которого будем списывать деньги
        int senderAccountId = createUserAccount(user1Token);

        // Делаем депозит на 5 тысяч
        depositMoneyOnAccount(senderAccountId, user1Token);

        // Создаем второй аккаунт пользователя 1
        int receiverAccountId = createUserAccount(user1Token);

        String requestBody = String.format("""
                {
                            "senderAccountId": %s,
                            "receiverAccountId": %s,
                            "amount": "%s"
                        }
                """, senderAccountId, receiverAccountId, "abcd");
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", user1Token)
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                .body("status", Matchers.equalTo(500))
                .body("error", Matchers.equalTo("Internal Server Error"))
                .body("path", Matchers.equalTo("/api/v1/accounts/transfer"));
    }

    // негативный тест: трансфер без токена авторизации
    @Test
    public void userCannotTransferWithoutAuthorizationToken(){
        // Создаем имя пользователя 1
        String username1 = getUsername("u1");

        // Создаем пользователя 1
        createUser(username1);

        // Получаем токен пользователя 1
        String user1Token = getUserToken(username1);

        // Создаем первый аккаунт пользователя, с которого будем списывать деньги
        int senderAccountId = createUserAccount(user1Token);

        // Делаем депозит на 5 тысяч
        depositMoneyOnAccount(senderAccountId, user1Token);

        // Создаем второй аккаунт пользователя 1
        int receiverAccountId = createUserAccount(user1Token);

        String requestBody = String.format("""
                {
                            "senderAccountId": %s,
                            "receiverAccountId": %s,
                            "amount": %s
                        }
                """, senderAccountId, receiverAccountId, 100f);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    // негативный тест: трансфер с битым токеном авторизации
    @Test
    public void userCannotTransferWithWrongAuthorizationToken(){
        // Создаем имя пользователя 1
        String username1 = getUsername("u1");

        // Создаем пользователя 1
        createUser(username1);

        // Получаем токен пользователя 1
        String user1Token = getUserToken(username1);

        // Создаем первый аккаунт пользователя, с которого будем списывать деньги
        int senderAccountId = createUserAccount(user1Token);

        // Делаем депозит на 5 тысяч
        depositMoneyOnAccount(senderAccountId, user1Token);

        // Создаем второй аккаунт пользователя 1
        int receiverAccountId = createUserAccount(user1Token);

        String brokenToken = user1Token.substring(0,user1Token.length()-5);

        String requestBody = String.format("""
                {
                            "senderAccountId": %s,
                            "receiverAccountId": %s,
                            "amount": %s
                        }
                """, senderAccountId, receiverAccountId, 100f);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", brokenToken)
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }
}
