package iteration2.api;

import io.restassured.specification.RequestSpecification;
import models.InvalidProfileUpdateRequest;
import models.ProfileUpdateRequest;
import models.ProfileUpdateResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.skeleton.Endpoint;
import requests.skeleton.requesters.CrudRequester;
import requests.skeleton.requesters.ValidatedCrudRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.stream.Stream;

import static assertions.ProfileAssertions.assertProfileName;
import static assertions.ProfileAssertions.assertSuccessfulProfileUpdate;
import static factories.ProfileRequestFactory.profileUpdateRequest;
import static factories.ProfileRequestFactory.validProfileUpdateRequest;
import static testdata.ProfileTestData.DEFAULT_PROFILE_NAME;

public class ChangeNameInProfileTests extends BaseTest {

    @Test
    public void userCanUpdateNameWithCorrectData() {
        RequestSpecification userSpec = createUserSpecForTest();

        ProfileUpdateRequest profileUpdateRequest =
                validProfileUpdateRequest();

        ProfileUpdateResponse profileUpdateResponse =
                new ValidatedCrudRequester<ProfileUpdateResponse>(
                        userSpec,
                        Endpoint.CUSTOMER_PROFILE_UPDATE,
                        ResponseSpecs.requestReturnsOk()
                ).put(profileUpdateRequest);

        assertSuccessfulProfileUpdate(
                softy,
                profileUpdateRequest,
                profileUpdateResponse
        );

        assertProfileName(
                softy,
                userSpec,
                profileUpdateRequest.getName()
        );
    }

    public static Stream<Arguments> incorrectNameData() {
        return Stream.of(
                Arguments.of("Name"),
                Arguments.of("Three word name"),
                Arguments.of(""),
                Arguments.of(" New Name"),
                Arguments.of("New Name "),
                Arguments.of("   "),
                Arguments.of("New Nam%e"),
                Arguments.of("New Na1me"),
                Arguments.of("New John-Doe"),
                Arguments.of("New  Name")
        );
    }

    @MethodSource("incorrectNameData")
    @ParameterizedTest
    public void userCannotChangeNameWithWrongData(String newName) {
        RequestSpecification userSpec = createUserSpecForTest();

        ProfileUpdateRequest profileUpdateRequest = profileUpdateRequest(newName);

        new CrudRequester(
                userSpec,
                Endpoint.CUSTOMER_PROFILE_UPDATE,
                ResponseSpecs.profileNameValidationError()
        ).put(profileUpdateRequest);

        assertProfileName(softy, userSpec, DEFAULT_PROFILE_NAME);
    }

    @Test
    public void userCannotChangeNameToNull() {
        RequestSpecification userSpec = createUserSpecForTest();

        ProfileUpdateRequest profileUpdateRequest = profileUpdateRequest(null);

        new CrudRequester(
                userSpec,
                Endpoint.CUSTOMER_PROFILE_UPDATE,
                ResponseSpecs.internalServerErrorForEndpoint(Endpoint.CUSTOMER_PROFILE_UPDATE)
        ).put(profileUpdateRequest);

        assertProfileName(softy, userSpec, DEFAULT_PROFILE_NAME);
    }

    @Test
    public void userCannotChangeNameWithoutNameInBody() {
        RequestSpecification userSpec = createUserSpecForTest();

        InvalidProfileUpdateRequest profileUpdateRequest =
                InvalidProfileUpdateRequest.builder()
                        .build();

        new CrudRequester(
                userSpec,
                Endpoint.CUSTOMER_PROFILE_UPDATE,
                ResponseSpecs.internalServerErrorForEndpoint(Endpoint.CUSTOMER_PROFILE_UPDATE)
        ).put(profileUpdateRequest);

        assertProfileName(softy, userSpec, DEFAULT_PROFILE_NAME);
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

        assertProfileName(softy, userSpec, DEFAULT_PROFILE_NAME);
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

        assertProfileName(softy, userSpec, DEFAULT_PROFILE_NAME);
    }
}