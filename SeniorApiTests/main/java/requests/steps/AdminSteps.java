package requests.steps;

import generators.RandomModelGenerator;
import io.qameta.allure.Step;
import models.CreateUserRequest;
import models.CreateUserResponse;
import requests.skeleton.Endpoint;
import requests.skeleton.requesters.CrudRequester;
import requests.skeleton.requesters.ValidatedCrudRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

public class AdminSteps {
    @Step("Create user by admin")
    public static CreateUserRequest createUser() {
        CreateUserRequest userRequest = RandomModelGenerator.generate(CreateUserRequest.class);

        createUserAndGetResponse(userRequest);

        return userRequest;
    }

    @Step("Create user by admin and get response")
    public static CreateUserResponse createUserAndGetResponse(CreateUserRequest userRequest) {
        return new ValidatedCrudRequester<CreateUserResponse>(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USER,
                ResponseSpecs.entityWasCreated()
        ).post(userRequest);
    }

    @Step("Delete user by admin")
    public static void deleteUser(int userId) {
        new CrudRequester(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USER,
                ResponseSpecs.requestReturnsOk()
        ).delete(userId);
    }
}
