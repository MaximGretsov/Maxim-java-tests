package request;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.CustomerProfileResponse;

import static io.restassured.RestAssured.given;

public class CustomerProfileRequester extends GetRequester{
    public CustomerProfileRequester(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
        super(requestSpecification, responseSpecification);
    }

    @Override
    public ValidatableResponse get() {
        return given()
                .spec(requestSpecification)
                .get("/api/v1/customer/profile")
                .then()
                .assertThat()
                .spec(responseSpecification);
    }

    public CustomerProfileResponse getProfile() {
        return get()
                .extract()
                .as(CustomerProfileResponse.class);
    }
}
