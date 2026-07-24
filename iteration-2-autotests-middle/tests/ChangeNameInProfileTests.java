package iteration2;


import io.restassured.specification.RequestSpecification;
import iteration1.BaseTest;
import models.ProfileUpdateRequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import request.ProfileRequester;
import specs.RequestsSpecs;
import specs.ResponseSpecs;

import java.util.stream.Stream;

import static TestData.ProfileTestData.DEFAULT_PROFILE_NAME;
import static generators.RandomData.*;
import static iteration2.assertions.ProfileAssertions.assertProfileName;
import static request.steps.UserSteps.createUserAndGetAuthSpec;

public class ChangeNameInProfileTests extends BaseTest {
    // позитивный тест: изменение имени из 2х слов используя только буквы и пробел
    @Test
    public void userCanUpdateNameWithTwoWordsAndWithLettersAndSpacesOnly(){
        RequestSpecification userSpec = createUserAndGetAuthSpec();

        String newName = generateValidProfileName();

        ProfileUpdateRequest profileUpdateRequest = ProfileUpdateRequest.builder()
                .name(newName)
                .build();

        new ProfileRequester(
                userSpec,
                ResponseSpecs.successfulProfileUpdateResponse(newName)
        ).put(profileUpdateRequest);

        // проверка, что имя изменилось
        assertProfileName(softy, userSpec, newName);
    }

    public static Stream<Arguments> incorrectNameData() {
        return Stream.of(
                Arguments.of(
                        "Name contains one word",
                        generateSingleWordProfileName()
                ),
                Arguments.of(
                        "Name contains three words",
                        generateThreeWordProfileName()
                ),
                Arguments.of(
                        "Name is empty",
                        ""
                ),
                Arguments.of(
                        "Name contains leading space",
                        generateProfileNameWithLeadingSpace()
                ),
                Arguments.of(
                        "Name contains trailing space",
                        generateProfileNameWithTrailingSpace()
                ),
                Arguments.of(
                        "Name contains only spaces",
                        generateOnlySpacesProfileName()
                ),
                Arguments.of(
                        "Name contains special character",
                        generateProfileNameWithSpecialCharacter()
                ),
                Arguments.of(
                        "Name contains digit",
                        generateProfileNameWithDigit()
                ),
                Arguments.of(
                        "Name contains hyphen",
                        generateProfileNameWithHyphen()
                ),
                Arguments.of(
                        "Name contains double space",
                        generateProfileNameWithDoubleSpace()
                )
        );
    }

    @MethodSource("incorrectNameData")
    @ParameterizedTest(name = "{0}")
    public void userCannotChangeNameWithWrongData( String caseName, String newName){
        RequestSpecification userSpec = createUserAndGetAuthSpec();

        ProfileUpdateRequest profileUpdateRequest = ProfileUpdateRequest.builder()
                .name(newName)
                .build();

        new ProfileRequester(userSpec,ResponseSpecs.profileNameValidationError())
                .put(profileUpdateRequest);

        // проверка, что имя не изменилось
        assertProfileName(softy, userSpec, DEFAULT_PROFILE_NAME);
    }

    // негативный тест с невалидным токеном авторизации
    @Test
    public void userCannotChangeNameWithWrongAuthorizationToken(){
        RequestSpecification userSpec = createUserAndGetAuthSpec();

        String newName = generateValidProfileName();

        ProfileUpdateRequest profileUpdateRequest = ProfileUpdateRequest.builder()
                .name(newName)
                .build();

        new ProfileRequester(RequestsSpecs.brokenAuthSpec(), ResponseSpecs.unauthorized())
                .put(profileUpdateRequest);

        // проверка, что имя не изменилось
        assertProfileName(softy, userSpec, DEFAULT_PROFILE_NAME);
    }

    // негативный тест без токена авторизации(нет хедера с авторизацией)
    @Test
    public void userCannotChangeNameWithoutAuthorization(){
        RequestSpecification userSpec = createUserAndGetAuthSpec();

        String newName = generateValidProfileName();

        ProfileUpdateRequest profileUpdateRequest = ProfileUpdateRequest.builder()
                .name(newName)
                .build();

        new ProfileRequester(RequestsSpecs.unauthSpec(), ResponseSpecs.unauthorized())
                .put(profileUpdateRequest);

        // проверка, что имя не изменилось
        assertProfileName(softy, userSpec, DEFAULT_PROFILE_NAME);
    }
}
