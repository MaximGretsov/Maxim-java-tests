package api.requests.skeleton;

import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

public abstract class HttpRequest {
    protected RequestSpecification requestSpecification;
    protected Endpoint endpoint;
    protected ResponseSpecification responseSpecification;

    public HttpRequest(RequestSpecification requestsSpecification, Endpoint endpoint, ResponseSpecification responseSpecification) {
        this.requestSpecification = requestsSpecification;
        this.endpoint = endpoint;
        this.responseSpecification = responseSpecification;
    }
}
