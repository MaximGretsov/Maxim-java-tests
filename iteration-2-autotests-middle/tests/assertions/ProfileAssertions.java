package iteration2.assertions;

import io.restassured.specification.RequestSpecification;
import models.CustomerProfileResponse;
import org.assertj.core.api.SoftAssertions;
import request.steps.ProfileSteps;

public final class ProfileAssertions {

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
                .as("Profile name")
                .isEqualTo(expectedName);
    }
}
