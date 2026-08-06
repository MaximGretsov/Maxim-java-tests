package api.requests.skeleton.requesters;

import api.configs.Config;
import api.requests.skeleton.interfaces.GetAllEndpointInterface;
import common.helpers.StepLogger;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import api.models.BaseModel;
import api.requests.skeleton.Endpoint;
import api.requests.skeleton.HttpRequest;
import api.requests.skeleton.interfaces.CrudEndpointInterface;

import static io.restassured.RestAssured.given;

public class CrudRequester extends HttpRequest implements CrudEndpointInterface, GetAllEndpointInterface {
    private final static String API_VERSION = Config.getProperty("apiVersion");

    public CrudRequester(RequestSpecification requestSpecification, Endpoint endpoint, ResponseSpecification responseSpecification) {
        super(requestSpecification, endpoint, responseSpecification);
    }

    @Override
    public ValidatableResponse post(BaseModel model) {
        return StepLogger.log("POST request to " + endpoint.getUrl(), () -> {
            var body = model == null ? "" : model;
            return given()
                    .spec(requestSpecification)
                    .body(body)
                    .post(API_VERSION + endpoint.getUrl())
                    .then()
                    .assertThat()
                    .spec(responseSpecification);
        });
    }

    public ValidatableResponse post() {
        return given()
                .spec(requestSpecification)
                .post(API_VERSION + endpoint.getUrl())
                .then()
                .spec(responseSpecification);
    }

    public ValidatableResponse get() {
        return given()
                .spec(requestSpecification)
                .get(API_VERSION + endpoint.getUrl())
                .then()
                .spec(responseSpecification);
    }

    @Override
    public ValidatableResponse get(long id) {
        return given()
                .spec(requestSpecification)
                .get(API_VERSION + endpoint.getUrl() + "/" + id)
                .then()
                .spec(responseSpecification);
    }

    public ValidatableResponse put(BaseModel model) {
        return given()
                .spec(requestSpecification)
                .body(model)
                .put(API_VERSION + endpoint.getUrl())
                .then()
                .spec(responseSpecification);
    }

    public ValidatableResponse put() {
        return given()
                .spec(requestSpecification)
                .put(API_VERSION + endpoint.getUrl())
                .then()
                .spec(responseSpecification);
    }

    public ValidatableResponse putRawBody(String rawBody) {
        return given()
                .spec(requestSpecification)
                .body(rawBody)
                .put(API_VERSION + endpoint.getUrl())
                .then()
                .spec(responseSpecification);
    }

    @Override
    public ValidatableResponse update(long id, BaseModel model) {
        Object body = model == null ? "" : model;

        return given()
                .spec(requestSpecification)
                .body(body)
                .put(API_VERSION + endpoint.getUrl() + "/" + id)
                .then()
                .spec(responseSpecification);
    }

    @Override
    public ValidatableResponse delete(long id) {
        return given()
                .spec(requestSpecification)
                .delete(API_VERSION + endpoint.getUrl() + "/" + id)
                .then()
                .spec(responseSpecification);
    }

    @Override
    public ValidatableResponse getAll(Class<?> clazz) {
        return  given()
                .spec(requestSpecification)
                .get(API_VERSION + endpoint.getUrl())
                .then().assertThat()
                .spec(responseSpecification);
    }
}
