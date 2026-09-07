package api.assertions;

import api.requests.steps.UserSteps;
import io.restassured.specification.RequestSpecification;
import api.models.CustomerProfileResponse;
import api.models.ProfileUpdateRequest;
import api.models.ProfileUpdateResponse;
import org.assertj.core.api.SoftAssertions;
import api.requests.steps.ProfileSteps;

import static api.specs.ResponseSpecs.PROFILE_UPDATE_SUCCESS_MESSAGE;

public class ProfileAssertions {

    private ProfileAssertions() {
    }

    public static void assertProfileName(
            SoftAssertions softy,
            RequestSpecification userSpec,
            String expectedName
    ) {
        assertProfileName(
                softy,
                ProfileSteps.getProfile(userSpec),
                expectedName
        );
    }

    public static void assertProfileName(
            SoftAssertions softy,
            UserSteps userSteps,
            String expectedName
    ) {
        assertProfileName(
                softy,
                userSteps.getProfile(),
                expectedName
        );
    }

    public static void assertProfileName(
            SoftAssertions softy,
            CustomerProfileResponse profile,
            String expectedName
    ) {
        softy.assertThat(profile.getName())
                .as("Имя пользователя в профиле")
                .isEqualTo(expectedName);
    }

    public static void assertSuccessfulProfileUpdate(
            SoftAssertions softy,
            ProfileUpdateRequest profileUpdateRequest,
            ProfileUpdateResponse profileUpdateResponse
    ) {
        softy.assertThat(profileUpdateResponse.getCustomer())
                .as("Объект пользователя в ответе")
                .isNotNull();

        softy.assertThat(
                        profileUpdateResponse
                                .getCustomer()
                                .getName()
                )
                .as("Новое имя пользователя в ответе")
                .isEqualTo(profileUpdateRequest.getName());

        softy.assertThat(profileUpdateResponse.getMessage())
                .as("Сообщение об успешном изменении профиля")
                .isEqualTo(PROFILE_UPDATE_SUCCESS_MESSAGE);
    }
}