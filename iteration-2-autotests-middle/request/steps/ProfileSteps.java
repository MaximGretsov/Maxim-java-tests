package request.steps;

import io.restassured.specification.RequestSpecification;
import models.CustomerProfileResponse;
import request.CustomerProfileRequester;
import specs.ResponseSpecs;

public final class ProfileSteps {

    private ProfileSteps() {
    }

    public static CustomerProfileResponse getProfile(
            RequestSpecification userSpec
    ) {
        return new CustomerProfileRequester(
                userSpec,
                ResponseSpecs.requestReturnsOk()
        ).getProfile();
    }
}