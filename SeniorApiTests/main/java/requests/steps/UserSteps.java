package requests.steps;

import io.qameta.allure.Step;
import io.restassured.specification.RequestSpecification;
import models.CreateUserRequest;
import specs.RequestSpecs;

public class UserSteps {
    @Step("Create user and get authorization specification")
    public static RequestSpecification createUserAndGetAuthSpec() {
        CreateUserRequest userRequest = AdminSteps.createUser();

        return RequestSpecs.authAsUserSpec(
                userRequest.getUsername(),
                userRequest.getPassword()
        );
    }
}
