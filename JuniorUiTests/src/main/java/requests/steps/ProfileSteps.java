package requests.steps;

import io.qameta.allure.Step;
import io.restassured.specification.RequestSpecification;
import models.CustomerProfileResponse;
import requests.skeleton.Endpoint;
import requests.skeleton.requesters.ValidatedCrudRequester;
import specs.ResponseSpecs;

public class ProfileSteps {
    @Step("Get customer profile")
    public static CustomerProfileResponse getProfile(RequestSpecification userSpec) {
        return new ValidatedCrudRequester<CustomerProfileResponse>(
                userSpec,
                Endpoint.CUSTOMER_PROFILE,
                ResponseSpecs.requestReturnsOk()
        ).get();
    }
}
