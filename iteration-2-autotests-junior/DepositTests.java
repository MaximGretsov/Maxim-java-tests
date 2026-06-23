package iteration2;

import io.restassured.RestAssured;
import io.restassured.config.JsonConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.path.json.config.JsonPathConfig;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

public class DepositTests {
    @BeforeAll
    public static void setupRestAssured(){
        RestAssured.filters(
                List.of(new RequestLoggingFilter(),
                        new ResponseLoggingFilter()));
    }

    // Генерируем уникальный username чтобы не ловить ошибки
    public static String getUsername(){
        return "Acc" + String.format("%08d", System.currentTimeMillis() %100_000_000L);
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
        // Создаем имя юзера
        String username = getUsername();

        // Создаем юзера
        createUser(username);

        // Получаем его токен
        String userToken = getUserToken(username);

        // Создаем аккаунт пользователя
        int accId = createUserAccount(userToken);

        String requestBody = String.format("""
                    {
                        "id": %s,
                        "balance": %s
                    }
                """, accId, balance);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userToken)
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", Matchers.equalTo(accId))
                .body("accountNumber", Matchers.equalTo("ACC" + accId))
                .body("balance", Matchers.comparesEqualTo(newBalance))
                .body("transactions", Matchers.notNullValue());
    }

    // негативные тесты c невалидной суммой депозита
    public static Stream<Arguments> InvalidAmount(){
        return Stream.of(
                // отправка 0
                Arguments.of(0, "Deposit amount must be at least 0.01"),
                // отправка отрицательного числа
                Arguments.of(-1f, "Deposit amount must be at least 0.01"),
                //  отправка больше максимума
                Arguments.of(5000.01f, "Deposit amount cannot exceed 5000")
        );
    }

    @MethodSource("InvalidAmount")
    @ParameterizedTest
    public void userCannotDepositWithInvalidAmount(float  balance,String errorValue){
        // Создаем имя юзера
        String username = getUsername();

        // Создаем юзера
        createUser(username);

        // Получаем его токен
        String userToken = getUserToken(username);

        // Создаем аккаунт пользователя
        int accId = createUserAccount(userToken);

        String requestBody = String.format("""
                {
                            "id": %s,
                            "balance": %s
                        }
                """, accId, balance);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userToken)
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(Matchers.equalTo(errorValue));

    }

    // негативный тест c депозитом на несуществующий аккаунт
    @Test
    public void userCannotDepositWithNonExistingAccount(){
        // Создаем имя юзера
        String username = getUsername();

        // Создаем юзера
        createUser(username);

        // Получаем его токен
        String userToken = getUserToken(username);

        // Создаем аккаунт пользователя
        int accId = createUserAccount(userToken);

        // берем accId который скорее не будет существовать(и вряд ли у нас столько аккаунтов,
        // что мы выйдем за пределы int)
        int nonExistingAccId = accId + 1000000;

        String requestBody = String.format("""
                {
                            "id": %s,
                            "balance": %s
                        }
                """, nonExistingAccId, 100);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userToken)
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_FORBIDDEN)
                .body(Matchers.equalTo("Unauthorized access to account"));
    }

    // негативный тест c депозитом на чужой аккаунт
    @Test
    public void userCannotDepositToAnotherUserAccount(){
        // Создаем имя пользователя1
        String username1 = getUsername();

        // Создаем пользователя1
        createUser(username1);

        // Получаем токен пользователя1
        String userToken1 = getUserToken(username1);

        // Создаем аккаунт пользователя1
        int accId1 = createUserAccount(userToken1);

        // Создаем имя пользователя2
        String username2 = getUsername();

        // Создаем пользователя2
        createUser(username2);

        // Получаем токен пользователя2
        String userToken2 = getUserToken(username2);

        // Создаем аккаунт пользователя2
        int accId2 = createUserAccount(userToken2);

        String requestBody = String.format("""
                {
                            "id": %s,
                            "balance": %s
                        }
                """, accId1, 100);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userToken2)
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_FORBIDDEN)
                .body(Matchers.equalTo("Unauthorized access to account"));
    }

    // негативный тест c string id в body (для этого теста создавать аккаунт внутри не надо)
    @Test
    public void userCannotDepositWithStringIdInBody(){
        // Создаем имя юзера
        String username = getUsername();

        // Создаем юзера
        createUser(username);

        // Получаем его токен
        String userToken = getUserToken(username);

        String requestBody = String.format("""
                {
                            "id": "%s",
                            "balance": %s
                        }
                """, "abcd", 100);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userToken)
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                .body("status", Matchers.equalTo(500))
                .body("error", Matchers.equalTo("Internal Server Error"))
                .body("path", Matchers.equalTo("/api/v1/accounts/deposit"));
    }

    // негативный тест c string balance в body
    @Test
    public void userCannotDepositWithStringBalanceInBody(){
        // Создаем имя юзера
        String username = getUsername();

        // Создаем юзера
        createUser(username);

        // Получаем его токен
        String userToken = getUserToken(username);

        // Создаем аккаунт пользователя
        int accId = createUserAccount(userToken);

        String requestBody = String.format("""
                {
                            "id": %s,
                            "balance": "%s"
                        }
                """, accId, "abcd");

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userToken)
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                .body("status", Matchers.equalTo(500))
                .body("error", Matchers.equalTo("Internal Server Error"))
                .body("path", Matchers.equalTo("/api/v1/accounts/deposit"));
    }

    // негативный тест без id в body (для этого теста создавать аккаунт внутри не надо)
    @Test
    public void userCannotDepositWithoutIdInBody(){
        // Создаем имя юзера
        String username = getUsername();

        // Создаем юзера
        createUser(username);

        // Получаем его токен
        String userToken = getUserToken(username);


        String requestBody = String.format("""
                {
                            "balance": %s
                        }
                """, 100);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userToken)
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                .body("status", Matchers.equalTo(500))
                .body("error", Matchers.equalTo("Internal Server Error"))
                .body("path", Matchers.equalTo("/api/v1/accounts/deposit"));
    }

    // негативный тест без balance в body
    @Test
    public void userCannotDepositWithoutBalanceInBody(){
        // Создаем имя юзера
        String username = getUsername();

        // Создаем юзера
        createUser(username);

        // Получаем его токен
        String userToken = getUserToken(username);

        // Создаем аккаунт пользователя
        int accId = createUserAccount(userToken);

        String requestBody = String.format("""
                {
                            "id": %s
                        }
                """, accId);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userToken)
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                .body("status", Matchers.equalTo(500))
                .body("error", Matchers.equalTo("Internal Server Error"))
                .body("path", Matchers.equalTo("/api/v1/accounts/deposit"));
    }

    // негативный тест с невалидным токеном авторизации
    @Test
    public void userCannotDepositWithWrongAuthorizationToken(){
        // Создаем имя юзера
        String username = getUsername();

        // Создаем юзера
        createUser(username);

        // Получаем его токен
        String userToken = getUserToken(username);

        // Создаем аккаунт пользователя
        int accId = createUserAccount(userToken);

        String brokenToken = userToken.substring(0,userToken.length()-5);

        String requestBody = String.format("""
                {
                            "id": %s,
                            "balance": %s
                        }
                """, accId, 100);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", brokenToken)
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    // негативный тест без токена авторизации(нет хедера с авторизацией)
    @Test
    public void userCannotDepositWithoutAuthorization(){
        // Создаем имя юзера
        String username = getUsername();

        // Создаем юзера
        createUser(username);

        // Получаем его токен
        String userToken = getUserToken(username);

        // Создаем аккаунт пользователя
        int accId = createUserAccount(userToken);

        String requestBody = String.format("""
                {
                            "id": %s,
                            "balance": %s
                        }
                """, accId, 100);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }
}

