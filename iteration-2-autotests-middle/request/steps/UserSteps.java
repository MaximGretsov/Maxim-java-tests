package request.steps;

import io.restassured.specification.RequestSpecification;
import models.CreateUserRequest;
import specs.RequestsSpecs;

public final class UserSteps {

    private UserSteps() {
    }

    public static RequestSpecification createUserAndGetAuthSpec() {
        CreateUserRequest userRequest = AdminSteps.createUser();

        return RequestsSpecs.authAsUserSpec(
                userRequest.getUsername(),
                userRequest.getPassword()
        );
    }
}