package assertions;

import io.restassured.specification.RequestSpecification;
import models.CustomerProfileResponse;
import models.ProfileUpdateRequest;
import models.ProfileUpdateResponse;
import org.assertj.core.api.SoftAssertions;
import requests.steps.ProfileSteps;

import static specs.ResponseSpecs.PROFILE_UPDATE_SUCCESS_MESSAGE;

public class ProfileAssertions {

    private ProfileAssertions() {
    }

    public static void assertProfileName(
            SoftAssertions softy,
            RequestSpecification userSpec,
            String expectedName
    ) {
        CustomerProfileResponse profile =
                ProfileSteps.getProfile(userSpec);

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
