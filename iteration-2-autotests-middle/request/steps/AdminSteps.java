package request.steps;

import generators.RandomData;
import models.CreateUserRequest;
import models.UserRole;
import request.AdminCreateUserRequester;
import specs.RequestsSpecs;
import specs.ResponseSpecs;

public final class AdminSteps {

    private AdminSteps() {
    }

    public static CreateUserRequest createUser() {
        CreateUserRequest userRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestsSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated()
        ).post(userRequest);

        return userRequest;
    }
}