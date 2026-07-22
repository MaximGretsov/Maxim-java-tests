package iteration2.api;

import generators.RandomModelGenerator;
import io.restassured.specification.RequestSpecification;
import models.CreateUserRequest;
import models.CreateUserResponse;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import requests.steps.AdminSteps;
import specs.RequestSpecs;

import java.util.ArrayList;
import java.util.List;

public class BaseTest {
    protected SoftAssertions softy;
    private final List<Integer> usersForDeletion = new ArrayList<>();

    @BeforeEach
    public void setUpTest(){
        this.softy = new SoftAssertions();
    }

    protected RequestSpecification createUserSpecForTest() {
        CreateUserRequest userRequest = RandomModelGenerator.generate(CreateUserRequest.class);
        CreateUserResponse userResponse = AdminSteps.createUserAndGetResponse(userRequest);

        usersForDeletion.add((int) userResponse.getId());

        return RequestSpecs.authAsUserSpec(
                userRequest.getUsername(),
                userRequest.getPassword()
        );
    }


    @AfterEach
    public void afterTest() {
        try {
            softy.assertAll();
        } finally {
            deleteCreatedUsers();
        }
    }

    private void deleteCreatedUsers() {
        usersForDeletion.forEach(AdminSteps::deleteUser);
        usersForDeletion.clear();
    }
}
