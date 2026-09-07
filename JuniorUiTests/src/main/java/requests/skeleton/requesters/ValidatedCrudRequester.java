package requests.skeleton.requesters;

import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.BaseModel;
import requests.skeleton.Endpoint;
import requests.skeleton.HttpRequest;
import requests.skeleton.interfaces.CrudEndpointInterface;

public class ValidatedCrudRequester<T extends BaseModel> extends HttpRequest implements CrudEndpointInterface {
    private CrudRequester crudRequester;

    public ValidatedCrudRequester(RequestSpecification requestsSpecification, Endpoint endpoint, ResponseSpecification responseSpecification) {
        super(requestsSpecification, endpoint, responseSpecification);
        this.crudRequester = new CrudRequester(requestSpecification,endpoint,responseSpecification);
    }

    @Override
    public T post(BaseModel model) {
        return (T) crudRequester.post(model)
                .extract()
                .as(endpoint.getResponseModel());
    }

    public T post() {
        return (T) crudRequester.post()
                .extract()
                .as(endpoint.getResponseModel());
    }

    public T get() {
        return (T) crudRequester.get()
                .extract()
                .as(endpoint.getResponseModel());
    }

    @Override
    public T get(long id) {
        return (T) crudRequester.get(id)
                .extract()
                .as(endpoint.getResponseModel());
    }

    public T put(BaseModel model) {
        return (T) crudRequester.put(model)
                .extract()
                .as(endpoint.getResponseModel());
    }

    public T put() {
        return (T) crudRequester.put()
                .extract()
                .as(endpoint.getResponseModel());
    }

    @Override
    public T update(long id, BaseModel model) {
        return (T) crudRequester.update(id, model)
                .extract()
                .as(endpoint.getResponseModel());
    }

    @Override
    public T delete(long id) {
        return (T) crudRequester.delete(id)
                .extract()
                .as(endpoint.getResponseModel());
    }
}
