package request;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.ProfileUpdateRequest;

import static io.restassured.RestAssured.given;

public class ProfileRequester extends PutRequester<ProfileUpdateRequest>{
    public ProfileRequester(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
        super(requestSpecification, responseSpecification);
    }

    @Override
    public ValidatableResponse put(ProfileUpdateRequest model) {
        return given()
                .spec(requestSpecification)
                .body(model)
                .put("/api/v1/customer/profile")
                .then()
                .assertThat()
                .spec(responseSpecification);
    }

    // Для невалидного body: {}, name: null и т.д.
    public ValidatableResponse putRawBody(String rawBody) {
        return given()
                .spec(requestSpecification)
                .body(rawBody)
                .put("/api/v1/customer/profile")
                .then()
                .assertThat()
                .spec(responseSpecification);
    }
}
