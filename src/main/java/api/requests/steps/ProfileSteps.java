package api.requests.steps;

import common.helpers.StepLogger;
import io.qameta.allure.Step;
import io.restassured.specification.RequestSpecification;
import api.models.CustomerProfileResponse;
import api.requests.skeleton.Endpoint;
import api.requests.skeleton.requesters.ValidatedCrudRequester;
import api.specs.ResponseSpecs;

public class ProfileSteps {
    @Step("Get customer profile")
    public static CustomerProfileResponse getProfile(RequestSpecification userSpec) {
        return StepLogger.log("User gets customer profile", () ->
                new ValidatedCrudRequester<CustomerProfileResponse>(
                        userSpec,
                        Endpoint.CUSTOMER_PROFILE,
                        ResponseSpecs.requestReturnsOk()
                ).get()
        );
    }
}
