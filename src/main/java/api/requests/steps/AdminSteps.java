package api.requests.steps;

import api.generators.RandomModelGenerator;
import common.helpers.StepLogger;
import io.qameta.allure.Step;
import api.models.CreateUserRequest;
import api.models.CreateUserResponse;
import api.requests.skeleton.Endpoint;
import api.requests.skeleton.requesters.CrudRequester;
import api.requests.skeleton.requesters.ValidatedCrudRequester;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;

import java.util.List;

public class AdminSteps {
    @Step("Create user by admin")
    public static CreateUserRequest createUser() {
        CreateUserRequest userRequest =
                RandomModelGenerator.generate(CreateUserRequest.class);

        return StepLogger.log("Admin creates user " + userRequest.getUsername(), () -> {
                    new ValidatedCrudRequester<CreateUserResponse>(
                            RequestSpecs.adminSpec(),
                            Endpoint.ADMIN_USER,
                            ResponseSpecs.entityWasCreated())
                            .post(userRequest);

                    return userRequest;
                }
        );
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

    @Step("Get All Users")
    public static List<CreateUserResponse> getAllUsers(){
        return StepLogger.log("Admin gets all users", () -> {
            return new ValidatedCrudRequester<CreateUserResponse>(
                    RequestSpecs.adminSpec(),
                    Endpoint.ADMIN_USER,
                    ResponseSpecs.requestReturnsOk()).getAll(CreateUserResponse[].class);
        });
    }
}
