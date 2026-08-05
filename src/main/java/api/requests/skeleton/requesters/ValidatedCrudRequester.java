package api.requests.skeleton.requesters;

import api.requests.skeleton.interfaces.GetAllEndpointInterface;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import api.models.BaseModel;
import api.requests.skeleton.Endpoint;
import api.requests.skeleton.HttpRequest;
import api.requests.skeleton.interfaces.CrudEndpointInterface;

import java.util.Arrays;
import java.util.List;

import static io.restassured.RestAssured.given;

public class ValidatedCrudRequester<T extends BaseModel> extends HttpRequest implements CrudEndpointInterface, GetAllEndpointInterface {
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

    @Override
    public List<T> getAll(Class<?> clazz) {
        T[] array = (T[]) crudRequester.getAll(clazz).extract().as(clazz);
        return Arrays.asList(array);
    }
}
