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

public class ChangeNameInProfileTests {
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

    // создаем пользователя - у него автоматически будет имя null
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

    // позитивный тест: изменение имени из 2х слов используя только буквы и пробел
    @Test
    public void userCanUpdateNameWithTwoWordsAndWithLettersAndSpacesOnly(){
        // Создаем username юзера
        String username = getUsername();

        // Создаем юзера
        createUser(username);

        // Получаем его токен
        String userToken = getUserToken(username);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userToken)
                .body("""
                    {
                        "name": "New Name"
                    }
                """)
                .put("http://localhost:4111/api/v1/customer/profile")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("customer.name", Matchers.equalTo("New Name"))
                .body("message", Matchers.equalTo("Profile updated successfully"));
    }

    // негативные тесты
    public static Stream<Arguments> incorrectNameData(){
        return Stream.of(
                // одно слово в поле name
                Arguments.of("Name"),
                // три слова в поле name
                Arguments.of("Three word name"),
                // пустое поле
                Arguments.of(""),
                // пробел перед двумя словами в имени
                Arguments.of(" New Name"),
                // пробел после двух слов в имени
                Arguments.of("New Name "),
                // имя из пробелов
                Arguments.of("   "),
                // имя из двух слов со специальными знаками
                Arguments.of("New Nam%e"),
                // имя из двух слов с цифрами
                Arguments.of("New Na1me"),
                // имя из двух слов с дефисом
                Arguments.of("New John-Doe"),
                // имя из двух слов с двумя пробелами между словами
                Arguments.of("New  Name")

        );
    }

    @MethodSource("incorrectNameData")
    @ParameterizedTest
    public void userCannotChangeNameWithWrongData(String newName){
        // Создаем username юзера
        String username = getUsername();

        // Создаем юзера
        createUser(username);

        // Получаем его токен
        String userToken = getUserToken(username);

        String requestBody = String.format("""
                    {
                        "name": "%s"
                    }
                """,newName);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userToken)
                .body(requestBody)
                .put("http://localhost:4111/api/v1/customer/profile")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(Matchers.equalTo("Name must contain two words with letters only"));
    }


    // негативный тест: изменение имени на null
    @Test
    public void userCannotChangeNameToNull(){
        // Создаем username юзера
        String username = getUsername();

        // Создаем юзера
        createUser(username);

        // Получаем его токен
        String userToken = getUserToken(username);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userToken)
                .body("""
                    {
                        "name": null
                    }
                """)
                .put("http://localhost:4111/api/v1/customer/profile")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                .body("status", Matchers.equalTo(500))
                .body("error", Matchers.equalTo("Internal Server Error"))
                .body("path", Matchers.equalTo("/api/v1/customer/profile"));
    }

    // негативный тест: отправка запроса без поля name в body
    @Test
    public void userCannotChangeNameWithoutNameInBody(){
        // Создаем username юзера
        String username = getUsername();

        // Создаем юзера
        createUser(username);

        // Получаем его токен
        String userToken = getUserToken(username);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userToken)
                .body("""
                    {}
                """)
                .put("http://localhost:4111/api/v1/customer/profile")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                .body("status", Matchers.equalTo(500))
                .body("error", Matchers.equalTo("Internal Server Error"))
                .body("path", Matchers.equalTo("/api/v1/customer/profile"));
    }

    // негативный тест с невалидным токеном авторизации
    @Test
    public void userCannotChangeNameWithWrongAuthorizationToken(){
        // Создаем имя юзера
        String username = getUsername();

        // Создаем юзера
        createUser(username);

        // Получаем его токен
        String userToken = getUserToken(username);

        String brokenToken = userToken.substring(0,userToken.length()-5);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", brokenToken)
                .body("""
                    {
                        "name": "New Name"
                    }
                """)
                .put("http://localhost:4111/api/v1/customer/profile")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    // негативный тест без токена авторизации(нет хедера с авторизацией)
    @Test
    public void userCannotChangeNameWithoutAuthorization(){
        // Создаем имя юзера
        String username = getUsername();

        // Создаем юзера
        createUser(username);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                    {
                        "name": "New Name"
                    }
                """)
                .put("http://localhost:4111/api/v1/customer/profile")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }
}
