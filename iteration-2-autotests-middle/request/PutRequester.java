package request;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.BaseModel;

public abstract class PutRequester <T extends BaseModel> extends BaseRequester{
    public PutRequester(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
        super(requestSpecification, responseSpecification);
    }

    public abstract ValidatableResponse put(T model);
}
