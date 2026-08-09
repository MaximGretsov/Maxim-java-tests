package iteration2.api;

import api.dao.UserDao;
import api.dao.comparison.DaoAndModelAssertions;
import api.models.CustomerProfileResponse;
import api.requests.steps.DataBaseSteps;
import api.requests.steps.ProfileSteps;
import io.restassured.specification.RequestSpecification;
import api.models.ProfileUpdateRequest;
import api.models.ProfileUpdateResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import api.requests.skeleton.Endpoint;
import api.requests.skeleton.requesters.CrudRequester;
import api.requests.skeleton.requesters.ValidatedCrudRequester;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;

import java.util.stream.Stream;

import static api.assertions.ProfileAssertions.assertProfileName;
import static api.assertions.ProfileAssertions.assertSuccessfulProfileUpdate;
import static api.factories.ProfileRequestFactory.profileUpdateRequest;
import static api.factories.ProfileRequestFactory.validProfileUpdateRequest;
import static api.generators.RandomModelGenerator.*;
import static api.testdata.ProfileTestData.DEFAULT_PROFILE_API_NAME;

public class ChangeNameInProfileTests extends BaseTest {

    @Test
    public void userCanUpdateNameWithCorrectData() {
        RequestSpecification userSpec = createUserSpecForTest();

        ProfileUpdateRequest profileUpdateRequest = validProfileUpdateRequest();

        ProfileUpdateResponse profileUpdateResponse =
                new ValidatedCrudRequester<ProfileUpdateResponse>(
                        userSpec,
                        Endpoint.CUSTOMER_PROFILE_UPDATE,
                        ResponseSpecs.requestReturnsOk()
                ).put(profileUpdateRequest);

        assertSuccessfulProfileUpdate(softy, profileUpdateRequest, profileUpdateResponse);

        CustomerProfileResponse profileResponse = ProfileSteps.getProfile(userSpec);

        assertProfileName(softy, profileResponse, profileUpdateRequest.getName());

        UserDao userDao = DataBaseSteps.getUserByUsername(profileResponse.getUsername());

        DaoAndModelAssertions
                .assertThat(profileResponse, userDao)
                .match();
    }

    public static Stream<Arguments> incorrectNameData() {
        return Stream.of(
                Arguments.of(
                        "Name contains one word",generateSingleWordProfileName()
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
    public void userCannotChangeNameWithWrongData(String caseName, String newName) {
        RequestSpecification userSpec = createUserSpecForTest();

        ProfileUpdateRequest profileUpdateRequest = profileUpdateRequest(newName);

        new CrudRequester(
                userSpec,
                Endpoint.CUSTOMER_PROFILE_UPDATE,
                ResponseSpecs.profileNameValidationError()
        ).put(profileUpdateRequest);

        CustomerProfileResponse profileResponse = ProfileSteps.getProfile(userSpec);

        assertProfileName(softy, profileResponse, DEFAULT_PROFILE_API_NAME);

        UserDao userDao = DataBaseSteps.getUserByUsername(profileResponse.getUsername());

        DaoAndModelAssertions
                .assertThat(profileResponse, userDao)
                .match();
    }

    @Test
    public void userCannotChangeNameToNull() {
        RequestSpecification userSpec = createUserSpecForTest();

        ProfileUpdateRequest profileUpdateRequest = profileUpdateRequest(null);

        new CrudRequester(
                userSpec,
                Endpoint.CUSTOMER_PROFILE_UPDATE,
                ResponseSpecs.internalServerErrorForEndpoint(
                        Endpoint.CUSTOMER_PROFILE_UPDATE
                )
        ).put(profileUpdateRequest);

        CustomerProfileResponse profileResponse = ProfileSteps.getProfile(userSpec);

        assertProfileName(softy, profileResponse, DEFAULT_PROFILE_API_NAME);

        UserDao userDao = DataBaseSteps.getUserByUsername(profileResponse.getUsername());

        DaoAndModelAssertions
                .assertThat(profileResponse, userDao)
                .match();
    }

    @Test
    public void userCannotChangeNameWithWrongAuthorizationToken() {
        RequestSpecification userSpec = createUserSpecForTest();

        ProfileUpdateRequest profileUpdateRequest = validProfileUpdateRequest();

        new CrudRequester(
                RequestSpecs.brokenAuthSpec(),
                Endpoint.CUSTOMER_PROFILE_UPDATE,
                ResponseSpecs.unauthorized()
        ).put(profileUpdateRequest);

        CustomerProfileResponse profileResponse = ProfileSteps.getProfile(userSpec);

        assertProfileName(softy, profileResponse, DEFAULT_PROFILE_API_NAME);

        UserDao userDao = DataBaseSteps.getUserByUsername(profileResponse.getUsername());

        DaoAndModelAssertions
                .assertThat(profileResponse, userDao)
                .match();
    }

    @Test
    public void userCannotChangeNameWithoutAuthorization() {
        RequestSpecification userSpec = createUserSpecForTest();

        ProfileUpdateRequest profileUpdateRequest = validProfileUpdateRequest();

        new CrudRequester(
                RequestSpecs.unauthSpec(),
                Endpoint.CUSTOMER_PROFILE_UPDATE,
                ResponseSpecs.unauthorized()
        ).put(profileUpdateRequest);

        CustomerProfileResponse profileResponse = ProfileSteps.getProfile(userSpec);

        assertProfileName(softy, profileResponse, DEFAULT_PROFILE_API_NAME);

        UserDao userDao = DataBaseSteps.getUserByUsername(profileResponse.getUsername());

        DaoAndModelAssertions
                .assertThat(profileResponse, userDao)
                .match();
    }
}