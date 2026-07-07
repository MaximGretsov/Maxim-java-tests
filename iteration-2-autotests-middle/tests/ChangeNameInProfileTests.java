package tests;

import generators.RandomData;
import io.restassured.specification.RequestSpecification;
import iteration1.BaseTest;
import models.CreateUserRequest;
import models.CustomerProfileResponse;
import models.ProfileUpdateRequest;
import models.UserRole;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import request.AdminCreateUserRequester;
import request.CustomerProfileRequester;
import request.ProfileRequester;
import specs.RequestsSpecs;
import specs.ResponseSpecs;

import java.util.stream.Stream;

public class ChangeNameInProfileTests extends BaseTest {
    private static final String PROFILE_PATH = "/api/v1/customer/profile";

    // Создаем пользователя и возвращаем request spec уже с токеном этого пользователя
    private RequestSpecification createUserAndGetAuthSpec() {
        CreateUserRequest userRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestsSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated()
        ).post(userRequest);

        return RequestsSpecs.authAsUserSpec(
                userRequest.getUsername(),
                userRequest.getPassword()
        );
    }

    // Получаем профиль пользователя
    private CustomerProfileResponse getProfile(RequestSpecification userSpec) {
        return new CustomerProfileRequester(
                userSpec,
                ResponseSpecs.requestReturnsOk()
        ).getProfile();
    }

    // Проверяем имя пользователя через GET /api/v1/customer/profile
    private void assertProfileName(RequestSpecification userSpec, String expectedName) {
        CustomerProfileResponse profile = getProfile(userSpec);

        softy.assertThat(profile.getName())
                .isEqualTo(expectedName);
    }

    // позитивный тест: изменение имени из 2х слов используя только буквы и пробел
    @Test
    public void userCanUpdateNameWithTwoWordsAndWithLettersAndSpacesOnly(){
        RequestSpecification userSpec = createUserAndGetAuthSpec();

        String newName = "New Name";

        ProfileUpdateRequest profileUpdateRequest = ProfileUpdateRequest.builder()
                .name(newName)
                .build();

        new ProfileRequester(
                userSpec,
                ResponseSpecs.successfulProfileUpdateResponse(newName)
        ).put(profileUpdateRequest);

        // проверка, что имя изменилось
        assertProfileName(userSpec, newName);
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
        RequestSpecification userSpec = createUserAndGetAuthSpec();

        String expectedName = null;

        ProfileUpdateRequest profileUpdateRequest = ProfileUpdateRequest.builder()
                .name(newName)
                .build();

        new ProfileRequester(userSpec,ResponseSpecs.requestReturnsBadRequestWithText
                ("Name must contain two words with letters only"))
                .put(profileUpdateRequest);

        // проверка, что имя не изменилось
        assertProfileName(userSpec, expectedName);
    }

    // негативный тест: изменение имени на null
    @Test
    public void userCannotChangeNameToNull(){
        RequestSpecification userSpec = createUserAndGetAuthSpec();

        String expectedName = null;

        String requestBody = """
                {
                    "name": null
                }
                """;

        new ProfileRequester(userSpec, ResponseSpecs.internalServerErrorForPath(PROFILE_PATH))
                .putRawBody(requestBody);

        // проверка, что имя не изменилось
        assertProfileName(userSpec, expectedName);
    }

    // негативный тест: отправка запроса без поля name в body
    @Test
    public void userCannotChangeNameWithoutNameInBody(){
        RequestSpecification userSpec = createUserAndGetAuthSpec();

        String expectedName = null;

        String requestBody = """
                    {}
                """;

        new ProfileRequester(userSpec, ResponseSpecs.internalServerErrorForPath(PROFILE_PATH))
                .putRawBody(requestBody);

        // проверка, что имя не изменилось
        assertProfileName(userSpec, expectedName );
    }

    // негативный тест с невалидным токеном авторизации
    @Test
    public void userCannotChangeNameWithWrongAuthorizationToken(){
        RequestSpecification userSpec = createUserAndGetAuthSpec();

        String newName = "New Name";
        String expectedName = null;

        ProfileUpdateRequest profileUpdateRequest = ProfileUpdateRequest.builder()
                .name(newName)
                .build();

        new ProfileRequester(RequestsSpecs.brokenAuthSpec(), ResponseSpecs.unauthorized())
                .put(profileUpdateRequest);

        // проверка, что имя не изменилось
        assertProfileName(userSpec, expectedName);
    }

    // негативный тест без токена авторизации(нет хедера с авторизацией)
    @Test
    public void userCannotChangeNameWithoutAuthorization(){
        RequestSpecification userSpec = createUserAndGetAuthSpec();

        String newName = "New Name";
        String expectedName = null;

        ProfileUpdateRequest profileUpdateRequest = ProfileUpdateRequest.builder()
                .name(newName)
                .build();

        new ProfileRequester(RequestsSpecs.unauthSpec(), ResponseSpecs.unauthorized())
                .put(profileUpdateRequest);

        // проверка, что имя не изменилось
        assertProfileName(userSpec, expectedName);
    }
}
